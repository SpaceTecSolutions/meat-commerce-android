import {FieldValue} from "firebase-admin/firestore";
import {getFirestore} from "./database.js";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {
  AdminContext, beginCatalogUpload, catalogOptions, deleteCatalogImageUrls, finalizeUpload, integer, normalize, number,
  productResponse, requireAdminOrStaff, requireAdminOrStaffAny, requireCustomer, publicCustomerShop, shop, text,
} from "./catalog-common.js";

export const adminBeginProductImageUpload = beginCatalogUpload("products", "MANAGE_PRODUCTS");

export const adminGetProducts = onCall(catalogOptions, async (request) => {
  const context = await requireAdminOrStaffAny(request, ["MANAGE_PRODUCTS", "MANAGE_STOCK"]);
  const snapshot = await shop(context).collection("products").limit(500).get();
  const search = normalize(text(request.data?.search));
  const filter = text(request.data?.filter, "ALL");
  const categoryId = text(request.data?.categoryId);
  const subcategoryId = text(request.data?.subcategoryId);
  const pageSize = integer(request.data?.pageSize, 30);
  const cursor = text(request.data?.cursor);
  const all = snapshot.docs.filter((doc) => doc.get("archived") !== true);
  const filtered = all.filter((doc) => {
    const value = doc.data();
    return (!search || normalize(text(value.name)).includes(search)) &&
      (!categoryId || value.categoryId === categoryId) && (!subcategoryId || value.subcategoryId === subcategoryId) &&
      productFilter(value, filter);
  }).sort((a, b) => text(a.get("name")).localeCompare(text(b.get("name"))));
  const start = cursor ? Math.max(0, filtered.findIndex((doc) => doc.id === cursor) + 1) : 0;
  const page = filtered.slice(start, start + pageSize);
  return {products: page.map((doc) => productResponse(doc.id, doc.data())),
    nextCursor: start + pageSize < filtered.length ? page.at(-1)?.id ?? null : null,
    countedProducts: all.length, productLimit: await maximumProducts()};
});

export const customerGetProducts = onCall(catalogOptions, async (request) => {
  const context = request.auth ? await requireCustomer(request) : null;
  const base = getFirestore().collection("shops").doc(await publicCustomerShop(request));
  const categoryId = text(request.data?.categoryId);
  const subcategoryId = text(request.data?.subcategoryId);
  let productQuery: FirebaseFirestore.Query = categoryId ? base.collection("products").where("categoryId", "==", categoryId) :
    base.collection("products");
  if (subcategoryId) productQuery = productQuery.where("subcategoryId", "==", subcategoryId);
  const [products, activeCategories, cart] = await Promise.all([
    productQuery.limit(500).get(), activeCategoryIds(base, categoryId),
    context ? customerCart(context.uid).limit(100).get() : null,
  ]);
  const search = normalize(text(request.data?.search));
  const filter = text(request.data?.filter, "ALL");
  const pageSize = Math.min(40, Math.max(1, integer(request.data?.pageSize, 24)));
  const cursor = text(request.data?.cursor);
  const visible = products.docs.filter((doc) => {
    const value = doc.data();
    return value.active === true && value.archived !== true && activeCategories.has(text(value.categoryId)) &&
      (!categoryId || value.categoryId === categoryId) && (!subcategoryId || value.subcategoryId === subcategoryId) &&
      (!search || normalize(text(value.name)).includes(search)) &&
      (filter !== "IN_STOCK" || number(value.stockQuantity) > 0) &&
      (filter !== "OFFERS" || value.offerPriceMinor != null);
  }).sort((a, b) => text(a.get("name")).localeCompare(text(b.get("name"))));
  const start = cursor ? Math.max(0, visible.findIndex((doc) => doc.id === cursor) + 1) : 0;
  const page = visible.slice(start, start + pageSize);
  return {products: page.map((doc) => productResponse(doc.id, doc.data())),
    nextCursor: start + pageSize < visible.length ? page.at(-1)?.id ?? null : null,
    cartQuantity: cart?.size ?? 0,
    cartQuantities: Object.fromEntries(cart?.docs.map((doc) => [doc.id, integer(doc.get("quantity"))]) ?? []),
    offersEnabled: true};
});

export const customerGetProduct = onCall(catalogOptions, async (request) => {
  const context = request.auth ? await requireCustomer(request) : null; const productId = text(request.data?.productId);
  const shopId = await publicCustomerShop(request);
  const reference = getFirestore().collection("shops").doc(shopId).collection("products").doc(productId);
  const product = await reference.get();
  if (!product.exists || product.get("active") !== true || product.get("archived") === true) unavailable();
  const category = await getFirestore().collection("shops").doc(shopId).collection("categories")
    .doc(text(product.get("categoryId"))).get();
  if (!category.exists || category.get("active") !== true) unavailable();
  return {product: productResponse(product.id, product.data()!)};
});

export const customerAddToCart = onCall(catalogOptions, async (request) => {
  const context = await requireCustomer(request);
  const productId = text(request.data?.productId); const requested = integer(request.data?.quantity);
  if (!productId || requested < 1 || requested > 99) {
    throw new HttpsError("invalid-argument", "Valid product and quantity required");
  }
  const productReference = getFirestore().collection("shops").doc(context.shopId)
    .collection("products").doc(productId);
  const lineReference = customerCart(context.uid).doc(productId);
  return getFirestore().runTransaction(async (transaction) => {
    const product = await transaction.get(productReference);
    if (!product.exists || product.get("active") !== true || product.get("archived") === true) unavailable();
    const categoryReference = getFirestore().collection("shops").doc(context.shopId)
      .collection("categories").doc(text(product.get("categoryId")));
    const [category, currentLine, cart] = await Promise.all([
      transaction.get(categoryReference),
      transaction.get(lineReference),
      transaction.get(customerCart(context.uid).limit(100)),
    ]);
    if (!category.exists || category.get("active") !== true) unavailable();
    const stock = number(product.get("stockQuantity"));
    const previous = currentLine.exists ? integer(currentLine.get("quantity")) : 0;
    const nextQuantity = validCartQuantity(previous, requested, stock);
    const regularPrice = integer(product.get("priceMinor"));
    if (regularPrice <= 0) unavailable();
    const offered = integer(product.get("offerPriceMinor"));
    const effectivePrice = offered > 0 && offered < regularPrice ? offered : regularPrice;
    transaction.set(lineReference, {
      productId, shopId: context.shopId, name: text(product.get("name")),
      categoryId: text(product.get("categoryId")), unit: text(product.get("unit"), "PIECE"),
      imageUrl: Array.isArray(product.get("imageUrls")) ? product.get("imageUrls")[0] ?? null : null,
      regularPriceMinor: regularPrice, unitPriceMinor: effectivePrice, quantity: nextQuantity,
      updatedAt: FieldValue.serverTimestamp(),
    }, {merge: true});
    return {cartQuantity: cartProductCount(cart.size, currentLine.exists)};
  });
});

export function validCartQuantity(current: number, requested: number, stock: number): number {
  const maximum = Math.min(99, Math.floor(stock));
  if (maximum < 1 || current + requested > maximum) {
    throw new HttpsError("failed-precondition", "Product is out of stock", {reason: "OUT_OF_STOCK"});
  }
  return current + requested;
}

export function cartProductCount(existingLines: number, lineAlreadyExists: boolean): number {
  return existingLines + (lineAlreadyExists ? 0 : 1);
}

export const adminCreateProduct = onCall(catalogOptions, async (request) => {
  const context = await requireAdminOrStaff(request, "MANAGE_PRODUCTS"); const input = productInput(request.data);
  const reference = shop(context).collection("products").doc();
  const uploads = await finalizeImages(context, imageTokens(request.data?.imageUploadTokens), reference.id);
  const initialCount = await nonArchivedCount(context);
  const result = await getFirestore().runTransaction(async (transaction) => {
    const categoryReference = shop(context).collection("categories").doc(input.categoryId);
    const counterReference = shop(context).collection("counters").doc("catalog");
    const duplicateQuery = shop(context).collection("products").where("categoryId", "==", input.categoryId).limit(500);
    const [category, counter, config, duplicate] = await Promise.all([
      transaction.get(categoryReference), transaction.get(counterReference),
      transaction.get(getFirestore().collection("appConfig").doc("features")), transaction.get(duplicateQuery),
    ]);
    if (!category.exists || category.get("active") !== true) inactiveCategory();
    if (duplicate.docs.some((doc) => doc.get("archived") !== true && doc.get("normalizedName") === normalize(input.name))) duplicateProduct();
    const count = counter.exists ? integer(counter.get("productCount")) : initialCount;
    const maximum = integer(config.get("maxProducts"), 100);
    if (count >= maximum) limitReached();
    const value = {...input, categoryName: text(category.get("name")), normalizedName: normalize(input.name),
      imageUrls: uploads.map((upload) => upload.url), archived: false, revision: 0,
      createdAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp(), updatedBy: context.uid};
    transaction.create(reference, value);
    transaction.set(counterReference, {productCount: count + 1, updatedAt: FieldValue.serverTimestamp()}, {merge: true});
    transaction.update(categoryReference, {productCount: FieldValue.increment(1),
      activeProductCount: FieldValue.increment(input.active ? 1 : 0), revision: FieldValue.increment(1)});
    return productResponse(reference.id, value);
  });
  await cleanup(uploads); return {product: result};
});

export const adminUpdateProduct = onCall(catalogOptions, async (request) => {
  const context = await requireAdminOrStaff(request, "MANAGE_PRODUCTS"); const input = productInput(request.data);
  const productId = text(request.data?.productId); const expected = integer(request.data?.expectedRevision, -1);
  if (!productId || expected < 0) throw new HttpsError("invalid-argument", "Product revision required");
  const tokens = imageTokens(request.data?.imageUploadTokens);
  const retainedRequest = stringList(request.data?.retainedImageUrls);
  if (tokens.length + retainedRequest.length > 5) tooManyImages();
  const uploads = await finalizeImages(context, tokens, productId);
  const reference = shop(context).collection("products").doc(productId);
  let removedImageUrls: string[] = [];
  const result = await getFirestore().runTransaction(async (transaction) => {
    const current = await transaction.get(reference);
    if (!current.exists || current.get("archived") === true) unavailable();
    if (integer(current.get("revision")) !== expected) changed();
    const oldCategoryId = text(current.get("categoryId"));
    const categoryReferences = [...new Set([oldCategoryId, input.categoryId])]
      .map((id) => shop(context).collection("categories").doc(id));
    const categories = await Promise.all(categoryReferences.map((category) => transaction.get(category)));
    const targetCategory = categories.find((category) => category.id === input.categoryId);
    if (!targetCategory?.exists || targetCategory.get("active") !== true) inactiveCategory();
    const duplicate = await transaction.get(shop(context).collection("products").where("categoryId", "==", input.categoryId).limit(500));
    if (duplicate.docs.some((doc) => doc.id !== productId && doc.get("archived") !== true &&
        doc.get("normalizedName") === normalize(input.name))) duplicateProduct();
    const existingImages = stringList(current.get("imageUrls"));
    const replaceImages = request.data?.replaceImages === true;
    const retainedImages = replaceImages ? validateRetainedImages(existingImages, retainedRequest) : existingImages;
    if (replaceImages) removedImageUrls = existingImages.filter((url) => !retainedImages.includes(url));
    const update = {...input, categoryName: text(targetCategory.get("name")), normalizedName: normalize(input.name),
      imageUrls: replaceImages ? [...retainedImages, ...uploads.map((upload) => upload.url)] : existingImages,
      revision: expected + 1, updatedAt: FieldValue.serverTimestamp(), updatedBy: context.uid};
    transaction.update(reference, update);
    adjustCategoryCounts(transaction, context, current.data()!, input);
    return productResponse(productId, {...current.data(), ...update});
  });
  await cleanup(uploads);
  await deleteCatalogImageUrls(context, removedImageUrls);
  return {product: result};
});

export const adminSetProductActive = onCall(catalogOptions, async (request) => {
  const context = await requireAdminOrStaffAny(request, ["MANAGE_PRODUCTS", "MANAGE_STOCK"]);
  const productId = text(request.data?.productId);
  const expected = integer(request.data?.expectedRevision, -1); const active = request.data?.active === true;
  const reference = shop(context).collection("products").doc(productId);
  const result = await getFirestore().runTransaction(async (transaction) => {
    const current = await transaction.get(reference);
    if (!current.exists || current.get("archived") === true) unavailable();
    if (integer(current.get("revision")) !== expected) changed();
    if (active) {
      const category = await transaction.get(shop(context).collection("categories").doc(text(current.get("categoryId"))));
      if (!category.exists || category.get("active") !== true) inactiveCategory();
    }
    const delta = active === (current.get("active") === true) ? 0 : active ? 1 : -1;
    transaction.update(reference, {active, revision: expected + 1, updatedAt: FieldValue.serverTimestamp(), updatedBy: context.uid});
    if (delta) transaction.update(shop(context).collection("categories").doc(text(current.get("categoryId"))),
      {activeProductCount: FieldValue.increment(delta), revision: FieldValue.increment(1)});
    return productResponse(productId, {...current.data(), active, revision: expected + 1});
  });
  return {product: result};
});

export const adminArchiveProduct = onCall(catalogOptions, async (request) => {
  const context = await requireAdminOrStaff(request, "MANAGE_PRODUCTS"); const productId = text(request.data?.productId);
  const expected = integer(request.data?.expectedRevision, -1);
  if (!productId || expected < 0) throw new HttpsError("invalid-argument", "Product revision required");
  const reference = shop(context).collection("products").doc(productId);
  let imageUrls: string[] = [];
  await getFirestore().runTransaction(async (transaction) => {
    const counterReference = shop(context).collection("counters").doc("catalog");
    const [current, counter] = await Promise.all([
      transaction.get(reference), transaction.get(counterReference),
    ]);
    if (!current.exists || current.get("archived") === true) unavailable();
    if (integer(current.get("revision")) !== expected) changed();
    imageUrls = stringList(current.get("imageUrls"));
    transaction.update(reference, {archived: true, active: false, revision: expected + 1,
      imageUrls: [],
      archivedAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp(), updatedBy: context.uid});
    transaction.update(shop(context).collection("categories").doc(text(current.get("categoryId"))), {
      productCount: FieldValue.increment(-1),
      activeProductCount: FieldValue.increment(current.get("active") === true ? -1 : 0),
      revision: FieldValue.increment(1),
    });
    transaction.set(counterReference, {
      productCount: Math.max(0, integer(counter.get("productCount"), 1) - 1),
      updatedAt: FieldValue.serverTimestamp(),
    }, {merge: true});
  });
  await deleteCatalogImageUrls(context, imageUrls);
  return {success: true};
});

function productInput(data: any) {
  const name = text(data?.name).trim(); const description = text(data?.description).trim();
  const categoryId = text(data?.categoryId); const subcategoryId = data?.removeSubcategory === true ? null :
    text(data?.subcategoryId) || null; const unit = text(data?.unit); const priceMinor = integer(data?.priceMinor, -1);
  const offer = data?.removeOfferPrice === true ? null : data?.offerPriceMinor == null ? null : integer(data.offerPriceMinor, -1);
  const stockQuantity = number(data?.stockQuantity, -1); const lowStock = data?.removeLowStockThreshold === true ? null :
    data?.lowStockThreshold == null ? null : number(data.lowStockThreshold, -1);
  const active = data?.active !== false; const attributes = data?.attributes && typeof data.attributes === "object" ? data.attributes : {};
  if (name.length < 2 || name.length > 100 || description.length > 1000 || !categoryId ||
      !["KILOGRAM", "GRAM", "PIECE", "PACK"].includes(unit) || priceMinor <= 0 || stockQuantity < 0 ||
      (offer != null && (offer < 0 || offer >= priceMinor)) || (lowStock != null && lowStock < 0)) {
    throw new HttpsError("invalid-argument", "Invalid product details");
  }
  return {categoryId, subcategoryId, name, description, unit, priceMinor, offerPriceMinor: offer,
    stockQuantity, lowStockThreshold: lowStock, attributes, active};
}

function adjustCategoryCounts(transaction: FirebaseFirestore.Transaction, context: AdminContext,
  previous: FirebaseFirestore.DocumentData, next: ReturnType<typeof productInput>) {
  const oldId = text(previous.categoryId); const oldActive = previous.active === true;
  if (oldId === next.categoryId) {
    const delta = next.active === oldActive ? 0 : next.active ? 1 : -1;
    if (delta) transaction.update(shop(context).collection("categories").doc(oldId),
      {activeProductCount: FieldValue.increment(delta), revision: FieldValue.increment(1)});
  } else {
    transaction.update(shop(context).collection("categories").doc(oldId), {productCount: FieldValue.increment(-1),
      activeProductCount: FieldValue.increment(oldActive ? -1 : 0), revision: FieldValue.increment(1)});
    transaction.update(shop(context).collection("categories").doc(next.categoryId), {productCount: FieldValue.increment(1),
      activeProductCount: FieldValue.increment(next.active ? 1 : 0), revision: FieldValue.increment(1)});
  }
}

async function finalizeImages(context: AdminContext, tokens: string[], id: string) {
  return (await Promise.all(tokens.map((token) => finalizeUpload(context, token, "products", id)))).filter(Boolean) as
    Array<{url: string; cleanup: () => Promise<void>}>;
}
function imageTokens(value: unknown): string[] {
  const values = stringList(value);
  if (values.length > 5) tooManyImages();
  return values;
}
function stringList(value: unknown): string[] {
  return Array.isArray(value) ? [...new Set(value.filter((item): item is string => typeof item === "string" && !!item))] : [];
}
export function validateRetainedImages(existing: string[], requested: string[]): string[] {
  if (requested.some((url) => !existing.includes(url))) {
    throw new HttpsError("invalid-argument", "Invalid retained product image");
  }
  return [...new Set(requested)];
}
async function cleanup(uploads: Array<{cleanup: () => Promise<void>}>) { await Promise.all(uploads.map((upload) => upload.cleanup())); }
async function maximumProducts() { const value = await getFirestore().collection("appConfig").doc("features").get(); return integer(value.get("maxProducts"), 100); }
async function nonArchivedCount(context: AdminContext) { const snapshot = await shop(context).collection("products").get(); return snapshot.docs.filter((doc) => doc.get("archived") !== true).length; }
function customerCart(uid: string) { return getFirestore().collection("users").doc(uid).collection("cartItems"); }
async function activeCategoryIds(base: FirebaseFirestore.DocumentReference, categoryId: string): Promise<Set<string>> {
  if (categoryId) {
    const category = await base.collection("categories").doc(categoryId).get();
    return new Set(category.exists && category.get("active") === true ? [category.id] : []);
  }
  const categories = await base.collection("categories").limit(200).get();
  return new Set(categories.docs.filter((doc) => doc.get("active") === true).map((doc) => doc.id));
}
function productFilter(value: FirebaseFirestore.DocumentData, filter: string) { return filter === "ACTIVE" ? value.active === true : filter === "INACTIVE" ? value.active !== true : filter === "LOW_STOCK" ? number(value.stockQuantity) <= number(value.lowStockThreshold, -1) : true; }
function limitReached(): never { throw new HttpsError("resource-exhausted", "Product limit reached", {reason: "PRODUCT_LIMIT_REACHED"}); }
function inactiveCategory(): never { throw new HttpsError("failed-precondition", "Category inactive", {reason: "CATEGORY_INACTIVE"}); }
function duplicateProduct(): never { throw new HttpsError("already-exists", "Product exists", {reason: "DUPLICATE_PRODUCT"}); }
function unavailable(): never { throw new HttpsError("not-found", "Product unavailable", {reason: "PRODUCT_UNAVAILABLE"}); }
function changed(): never { throw new HttpsError("aborted", "Product changed"); }
function tooManyImages(): never { throw new HttpsError("invalid-argument", "A product can have up to 5 photos"); }
