import {FieldValue} from "firebase-admin/firestore";
import {getFirestore} from "./database.js";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {
  beginCatalogUpload, catalogOptions, categoryResponse, deleteCatalogImageUrls, finalizeUpload, integer, normalize,
  requireAdmin, requireAdminOrStaffAny, publicCustomerShop, shop, text,
} from "./catalog-common.js";

export const adminBeginCategoryImageUpload = beginCatalogUpload("categories");

export const adminGetCategories = onCall(catalogOptions, async (request) => {
  const context = await requireAdminOrStaffAny(request, ["MANAGE_PRODUCTS", "MANAGE_STOCK"]);
  const snapshot = await shop(context).collection("categories").limit(200).get();
  return {categories: snapshot.docs.map((doc) => categoryResponse(doc.id, doc.data()))
    .sort((a, b) => a.sortOrder - b.sortOrder || a.name.localeCompare(b.name))};
});

export const customerGetActiveCategories = onCall(catalogOptions, async (request) => {
  const shopId = await publicCustomerShop(request);
  const snapshot = await getFirestore().collection("shops").doc(shopId).collection("categories").limit(200).get();
  return {categories: snapshot.docs.filter((doc) => doc.get("active") === true)
    .map((doc) => categoryResponse(doc.id, doc.data())).sort((a, b) => a.sortOrder - b.sortOrder)};
});

export const adminCreateCategory = onCall(catalogOptions, async (request) => {
  const context = await requireAdmin(request);
  const input = categoryInput(request.data);
  const reference = shop(context).collection("categories").doc();
  const image = await finalizeUpload(context, input.imageUploadToken, "categories", reference.id);
  const result = await getFirestore().runTransaction(async (transaction) => {
    const duplicate = await transaction.get(shop(context).collection("categories")
      .where("normalizedName", "==", normalize(input.name)).limit(1));
    if (!duplicate.empty) duplicateCategory();
    const value = {...input, imageUrl: image?.url ?? null, normalizedName: normalize(input.name),
      productCount: 0, activeProductCount: 0, revision: 0, createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(), updatedBy: context.uid};
    delete value.imageUploadToken;
    transaction.create(reference, value);
    return categoryResponse(reference.id, value);
  });
  await image?.cleanup();
  return {category: result};
});

export const adminUpdateCategory = onCall(catalogOptions, async (request) => {
  const context = await requireAdmin(request);
  const categoryId = text(request.data?.categoryId);
  const expectedRevision = integer(request.data?.expectedRevision, -1);
  if (!categoryId || expectedRevision < 0) throw new HttpsError("invalid-argument", "Category revision required");
  const input = categoryInput(request.data);
  const reference = shop(context).collection("categories").doc(categoryId);
  const image = await finalizeUpload(context, input.imageUploadToken, "categories", categoryId);
  let replacedImageUrl: unknown = null;
  const result = await getFirestore().runTransaction(async (transaction) => {
    const [current, duplicate] = await Promise.all([
      transaction.get(reference),
      transaction.get(shop(context).collection("categories").where("normalizedName", "==", normalize(input.name)).limit(2)),
    ]);
    if (!current.exists) throw new HttpsError("not-found", "Category not found");
    if (integer(current.get("revision")) !== expectedRevision) changed();
    if (duplicate.docs.some((doc) => doc.id !== categoryId)) duplicateCategory();
    const update: Record<string, unknown> = {...input, normalizedName: normalize(input.name),
      revision: expectedRevision + 1, updatedAt: FieldValue.serverTimestamp(), updatedBy: context.uid};
    delete update.imageUploadToken;
    if (image) { replacedImageUrl = current.get("imageUrl"); update.imageUrl = image.url; }
    transaction.update(reference, update);
    return categoryResponse(categoryId, {...current.data(), ...update});
  });
  await image?.cleanup();
  if (replacedImageUrl) await deleteCatalogImageUrls(context, [replacedImageUrl]);
  return {category: result};
});

export const adminSetCategoryActive = onCall(catalogOptions, async (request) => {
  const context = await requireAdmin(request);
  return mutateCategory(context, request.data, (current) => ({active: request.data?.active === true,
    revision: integer(current.revision) + 1}), "CATEGORY_STATUS_CHANGED");
});

export const adminDeleteCategory = onCall(catalogOptions, async (request) => {
  const context = await requireAdmin(request);
  const categoryId = text(request.data?.categoryId);
  const expectedRevision = integer(request.data?.expectedRevision, -1);
  const reference = shop(context).collection("categories").doc(categoryId);
  let imageUrl: unknown = null;
  await getFirestore().runTransaction(async (transaction) => {
    const current = await transaction.get(reference);
    if (!current.exists) throw new HttpsError("not-found", "Category not found");
    if (integer(current.get("revision")) !== expectedRevision) changed();
    if (integer(current.get("productCount")) > 0) throw new HttpsError("failed-precondition", "Category contains products", {reason: "CATEGORY_HAS_PRODUCTS"});
    imageUrl = current.get("imageUrl"); transaction.delete(reference);
  });
  await deleteCatalogImageUrls(context, [imageUrl]);
  return {deleted: true};
});

export const adminUpdateCategoryOrder = onCall(catalogOptions, async (request) => {
  const context = await requireAdmin(request);
  const ids = Array.isArray(request.data?.categoryIds) ? request.data.categoryIds.filter((id: unknown) => typeof id === "string") : [];
  const snapshot = await shop(context).collection("categories").get();
  if (ids.length !== snapshot.size || new Set(ids).size !== ids.length || ids.some((id: string) => !snapshot.docs.some((doc) => doc.id === id))) {
    throw new HttpsError("invalid-argument", "Every category must be ordered exactly once");
  }
  const batch = getFirestore().batch();
  ids.forEach((id: string, index: number) => batch.update(shop(context).collection("categories").doc(id),
    {sortOrder: index, revision: FieldValue.increment(1), updatedAt: FieldValue.serverTimestamp(), updatedBy: context.uid}));
  await batch.commit();
  const refreshed = await shop(context).collection("categories").get();
  return {categories: refreshed.docs.map((doc) => categoryResponse(doc.id, doc.data())).sort((a, b) => a.sortOrder - b.sortOrder)};
});

async function mutateCategory(context: Awaited<ReturnType<typeof requireAdmin>>, data: any,
  build: (current: FirebaseFirestore.DocumentData) => Record<string, unknown>, action: string) {
  const categoryId = text(data?.categoryId); const expected = integer(data?.expectedRevision, -1);
  const reference = shop(context).collection("categories").doc(categoryId);
  const category = await getFirestore().runTransaction(async (transaction) => {
    const snapshot = await transaction.get(reference);
    if (!snapshot.exists) throw new HttpsError("not-found", "Category not found");
    if (integer(snapshot.get("revision")) !== expected) changed();
    const update = {...build(snapshot.data()!), updatedAt: FieldValue.serverTimestamp(), updatedBy: context.uid};
    transaction.update(reference, update);
    transaction.create(shop(context).collection("auditLogs").doc(), {action, actorUid: context.uid,
      targetCategoryId: categoryId, createdAt: FieldValue.serverTimestamp()});
    return categoryResponse(categoryId, {...snapshot.data(), ...update});
  });
  return {category};
}

function categoryInput(data: any) {
  const name = text(data?.name).trim(); const description = text(data?.description).trim();
  const sortOrder = integer(data?.sortOrder); const active = data?.active !== false;
  if (name.length < 2 || name.length > 60 || description.length > 500 || sortOrder < 0) {
    throw new HttpsError("invalid-argument", "Invalid category details");
  }
  return {name, description, sortOrder, active, imageUploadToken: data?.imageUploadToken as string | undefined};
}
function duplicateCategory(): never { throw new HttpsError("already-exists", "Category already exists", {reason: "DUPLICATE_CATEGORY"}); }
function changed(): never { throw new HttpsError("aborted", "Category changed"); }
async function customerShopId(uid?: string): Promise<string> {
  if (!uid) return "default";
  const profile = await getFirestore().collection("users").doc(uid).get();
  return text(profile.get("shopId"), "default");
}
