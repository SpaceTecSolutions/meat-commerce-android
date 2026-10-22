import {FieldValue} from "firebase-admin/firestore";
import {getFirestore} from "./database.js";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {
  beginCatalogUpload, catalogOptions, deleteCatalogImageUrls, finalizeUpload, integer, normalize,
  publicCustomerShop, requireAdmin, requireAdminOrStaff, shop, text,
} from "./catalog-common.js";

export const adminBeginSubcategoryImageUpload = beginCatalogUpload("subcategories", "MANAGE_PRODUCTS");

async function enabled() {
  return (await getFirestore().collection("appConfig").doc("features").get())
    .get("subcategoriesAllowed") === true;
}
function response(id: string, value: FirebaseFirestore.DocumentData) {
  return {id, categoryId: text(value.categoryId), name: text(value.name), description: text(value.description),
    imageUrl: value.imageUrl ?? null, active: value.active === true,
    sortOrder: integer(value.sortOrder), revision: integer(value.revision),
    productCount: integer(value.productCount)};
}
function input(data: any) {
  const categoryId = text(data?.categoryId); const name = text(data?.name).trim();
  const description = text(data?.description).trim(); const sortOrder = integer(data?.sortOrder);
  if (!categoryId || name.length < 2 || name.length > 60 || description.length > 300 || sortOrder < 0)
    throw new HttpsError("invalid-argument", "Invalid subcategory details");
  return {categoryId, name, description, sortOrder, active: data?.active !== false,
    imageUploadToken: data?.imageUploadToken as string | undefined};
}
export const customerGetActiveSubcategories = onCall(catalogOptions, async (request) => {
  if (!await enabled()) return {subcategories: []};
  const categoryId = text(request.data?.categoryId); if (!categoryId) throw new HttpsError("invalid-argument", "Category required");
  const shopId = await publicCustomerShop(request);
  const snap = await getFirestore().collection("shops").doc(shopId).collection("subcategories")
    .where("categoryId", "==", categoryId).limit(100).get();
  return {subcategories: snap.docs.filter((doc) => doc.get("active") === true)
    .map((doc) => response(doc.id, doc.data())).sort((a, b) => a.sortOrder - b.sortOrder)};
});
export const adminGetSubcategories = onCall(catalogOptions, async (request) => {
  const context = await requireAdminOrStaff(request, "MANAGE_PRODUCTS"); if (!await enabled()) throw new HttpsError("failed-precondition", "Subcategories disabled");
  const categoryId = text(request.data?.categoryId); if (!categoryId) throw new HttpsError("invalid-argument", "Category required");
  const [snap, products] = await Promise.all([
    shop(context).collection("subcategories").where("categoryId", "==", categoryId).limit(100).get(),
    shop(context).collection("products").where("categoryId", "==", categoryId).get(),
  ]);
  const counts = new Map<string, number>();
  products.docs.forEach((doc) => {
    if (doc.get("archived") === true) return;
    const subcategoryId = text(doc.get("subcategoryId"));
    if (subcategoryId) counts.set(subcategoryId, (counts.get(subcategoryId) ?? 0) + 1);
  });
  return {subcategories: snap.docs.map((doc) => ({...response(doc.id, doc.data()),
    productCount: counts.get(doc.id) ?? 0}))};
});
export const adminSaveSubcategory = onCall(catalogOptions, async (request) => {
  const context = await requireAdminOrStaff(request, "MANAGE_PRODUCTS"); if (!await enabled()) throw new HttpsError("failed-precondition", "Subcategories disabled");
  const value = input(request.data); const id = text(request.data?.subcategoryId);
  const ref = id ? shop(context).collection("subcategories").doc(id) : shop(context).collection("subcategories").doc();
  const image = await finalizeUpload(context, value.imageUploadToken, "subcategories", ref.id);
  const category = await shop(context).collection("categories").doc(value.categoryId).get();
  if (!category.exists || category.get("active") !== true) throw new HttpsError("failed-precondition", "Category is unavailable");
  const duplicate = await shop(context).collection("subcategories").where("categoryId", "==", value.categoryId).get();
  if (duplicate.docs.some((doc) => doc.id !== ref.id && normalize(text(doc.get("name"))) === normalize(value.name)))
    throw new HttpsError("already-exists", "Subcategory already exists");
  const previous = await ref.get(); const expected = integer(request.data?.expectedRevision, -1);
  if (previous.exists && integer(previous.get("revision")) !== expected) throw new HttpsError("aborted", "Subcategory changed");
  if (!previous.exists) value.sortOrder = Math.max(-1, ...duplicate.docs.map((doc) => integer(doc.get("sortOrder")))) + 1;
  const replacedImageUrl = image && previous.exists ? previous.get("imageUrl") : null;
  const next: Record<string, unknown> = {...value, normalizedName: normalize(value.name),
    revision: previous.exists ? expected + 1 : 0,
    updatedAt: FieldValue.serverTimestamp(), updatedBy: context.uid};
  delete next.imageUploadToken;
  if (image) next.imageUrl = image.url;
  else if (!previous.exists) next.imageUrl = null;
  await ref.set(next, {merge: previous.exists});
  await image?.cleanup();
  if (replacedImageUrl) await deleteCatalogImageUrls(context, [replacedImageUrl]);
  return {subcategory: response(ref.id, {...(previous.data() ?? {}), ...next})};
});
export const adminDeleteSubcategory = onCall(catalogOptions, async (request) => {
  const context = await requireAdminOrStaff(request, "MANAGE_PRODUCTS"); const id = text(request.data?.subcategoryId);
  if (!id) throw new HttpsError("invalid-argument", "Subcategory required");
  const ref = shop(context).collection("subcategories").doc(id);
  const current = await ref.get();
  if (!current.exists) throw new HttpsError("not-found", "Subcategory not found");
  const products = await shop(context).collection("products").where("subcategoryId", "==", id).get();
  for (let offset = 0; offset < products.docs.length; offset += 400) {
    const batch = getFirestore().batch();
    products.docs.slice(offset, offset + 400).forEach((product) => batch.update(product.ref, {
      subcategoryId: FieldValue.delete(), subcategoryName: FieldValue.delete(),
      updatedAt: FieldValue.serverTimestamp(), updatedBy: context.uid,
    }));
    await batch.commit();
  }
  await ref.delete();
  await deleteCatalogImageUrls(context, [current.get("imageUrl")]);
  return {deleted: true};
});
