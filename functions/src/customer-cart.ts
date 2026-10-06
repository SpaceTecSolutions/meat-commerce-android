import {FieldValue} from "firebase-admin/firestore";
import {getFirestore} from "./database.js";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {catalogOptions, integer, number, requireCustomer, text} from "./catalog-common.js";

type Context = {uid: string; shopId: string};
type Adjustment = {productId: string; type: string; message: string};

export const customerGetCart = onCall(catalogOptions, async (request) => {
  const context = await requireCustomer(request);
  return {cart: await cartResponse(context)};
});

export const customerSetCartQuantity = onCall(catalogOptions, async (request) => {
  const context = await requireCustomer(request);
  const productId = text(request.data?.productId);
  const quantity = integer(request.data?.quantity, -1);
  if (!productId || quantity < 1 || quantity > 99) invalid("Valid product and quantity required");
  const firestore = getFirestore();
  await firestore.runTransaction(async (transaction) => {
    const productRef = shop(context).collection("products").doc(productId);
    const lineRef = cart(context.uid).doc(productId);
    const [product, line] = await Promise.all([
      transaction.get(productRef), transaction.get(lineRef),
    ]);
    if (!line.exists) throw new HttpsError("not-found", "Cart item not found");
    if (!isAvailable(product.data())) unavailable();
    const maximum = maximumQuantity(product.get("stockQuantity"));
    if (quantity > maximum) outOfStock(maximum);
    const prices = productPrices(product.data()!);
    transaction.set(lineRef, {
      quantity, name: text(product.get("name")), categoryId: text(product.get("categoryId")),
      unit: text(product.get("unit"), "PIECE"), imageUrl: firstImage(product.get("imageUrls")),
      ...prices, updatedAt: FieldValue.serverTimestamp(),
    }, {merge: true});
  });
  return {cart: await cartResponse(context)};
});

export const customerRemoveCartItem = onCall(catalogOptions, async (request) => {
  const context = await requireCustomer(request);
  const productId = text(request.data?.productId);
  if (!productId) invalid("Product is required");
  await cart(context.uid).doc(productId).delete();
  return {cart: await cartResponse(context)};
});

export async function cartResponse(context: Context) {
  const firestore = getFirestore();
  const [cartSnapshot, delivery, addresses] = await Promise.all([
    cart(context.uid).limit(100).get(),
    shop(context).collection("config").doc("delivery").get(),
    firestore.collection("users").doc(context.uid).collection("addresses").limit(50).get(),
  ]);
  const productRefs = cartSnapshot.docs.map((line) => shop(context).collection("products").doc(line.id));
  const products = productRefs.length ? await firestore.getAll(...productRefs) : [];
  const productById = new Map(products.map((product) => [product.id, product]));
  const adjustments: Adjustment[] = [];
  const updates: Array<{ref: FirebaseFirestore.DocumentReference; data: FirebaseFirestore.DocumentData}> = [];
  const lines = cartSnapshot.docs.map((line) => {
    const snapshot = line.data();
    const product = productById.get(line.id);
    if (!product?.exists || !isAvailable(product.data())) {
      adjustments.push({productId: line.id, type: "UNAVAILABLE", message: `${text(snapshot.name, "Item")} is unavailable`});
      return lineResponse(line.id, snapshot, false, 0);
    }
    const maximum = maximumQuantity(product.get("stockQuantity"));
    const requested = Math.max(1, integer(snapshot.quantity, 1));
    const quantity = Math.min(requested, maximum);
    const prices = productPrices(product.data()!);
    if (quantity < requested) adjustments.push({
      productId: line.id, type: "QUANTITY_REDUCED", message: `${text(product.get("name"), "Item")} quantity was reduced to ${quantity}`,
    });
    if (integer(snapshot.unitPriceMinor) !== prices.unitPriceMinor) adjustments.push({
      productId: line.id, type: "PRICE_CHANGED", message: `Price for ${text(product.get("name"), "item")} has changed`,
    });
    const fresh = {
      ...snapshot, quantity, name: text(product.get("name")), categoryId: text(product.get("categoryId")),
      unit: text(product.get("unit"), "PIECE"), imageUrl: firstImage(product.get("imageUrls")), ...prices,
    };
    if (quantity !== requested || integer(snapshot.unitPriceMinor) !== prices.unitPriceMinor) {
      updates.push({ref: line.ref, data: {...fresh, updatedAt: FieldValue.serverTimestamp()}});
    }
    return lineResponse(line.id, fresh, maximum > 0, maximum);
  });
  if (updates.length) {
    const batch = firestore.batch(); updates.forEach((update) => batch.set(update.ref, update.data, {merge: true}));
    await batch.commit();
  }
  const subtotalMinor = lines.reduce((total, line) => total + line.unitPriceMinor * line.quantity, 0);
  const discountMinor = 0;
  const threshold = nullableInteger(delivery.get("freeDeliveryThresholdMinor"));
  const configuredCharge = Math.max(0, integer(delivery.get("deliveryChargeMinor")));
  const deliveryFeeMinor = calculateDeliveryFee(subtotalMinor, configuredCharge, threshold);
  const minimumOrderMinor = Math.max(0, integer(delivery.get("minimumOrderMinor")));
  const address = addresses.docs.sort((a, b) => Number(b.get("isDefault") === true) - Number(a.get("isDefault") === true))[0];
  return {
    lines, subtotalMinor, discountMinor, deliveryFeeMinor,
    totalMinor: subtotalMinor - discountMinor + deliveryFeeMinor,
    deliveryEstimate: null, minimumOrderMinor,
    deliveryAddress: address ? addressResponse(address.id, address.data()) : null,
    adjustments,
  };
}

function lineResponse(id: string, value: FirebaseFirestore.DocumentData, available: boolean, maximum: number) {
  return {
    productId: id, name: text(value.name, "Product"), imageUrl: value.imageUrl ?? null,
    unit: text(value.unit, "PIECE"), unitPriceMinor: integer(value.unitPriceMinor),
    regularPriceMinor: integer(value.regularPriceMinor), quantity: Math.max(1, integer(value.quantity, 1)),
    maxQuantity: maximum, available,
  };
}
function addressResponse(id: string, value: FirebaseFirestore.DocumentData) {
  return {id, type: text(value.type, "OTHER"), name: text(value.name), mobile: text(value.mobile),
    address: text(value.address), landmark: text(value.landmark), city: text(value.city),
    state: text(value.state), postalCode: text(value.postalCode), isDefault: value.isDefault === true,
    revision: integer(value.revision)};
}
function productPrices(value: FirebaseFirestore.DocumentData) {
  const regularPriceMinor = integer(value.priceMinor);
  if (regularPriceMinor <= 0) unavailable();
  const offer = integer(value.offerPriceMinor);
  return {regularPriceMinor, unitPriceMinor: offer > 0 && offer < regularPriceMinor ? offer : regularPriceMinor};
}
function isAvailable(value: FirebaseFirestore.DocumentData | undefined) {
  return !!value && value.active === true && value.archived !== true && number(value.stockQuantity) >= 1;
}
function maximumQuantity(value: unknown) { return Math.min(99, Math.max(0, Math.floor(number(value)))); }
function firstImage(value: unknown) { return Array.isArray(value) && typeof value[0] === "string" ? value[0] : null; }
function nullableInteger(value: unknown) { return value == null ? null : Math.max(0, integer(value)); }
export function calculateDeliveryFee(subtotal: number, charge: number, freeThreshold: number | null) {
  return freeThreshold != null && subtotal >= freeThreshold ? 0 : Math.max(0, charge);
}
function cart(uid: string) { return getFirestore().collection("users").doc(uid).collection("cartItems"); }
function shop(context: Context) { return getFirestore().collection("shops").doc(context.shopId); }
function invalid(message: string): never { throw new HttpsError("invalid-argument", message); }
function unavailable(): never { throw new HttpsError("failed-precondition", "Product unavailable", {reason: "PRODUCT_UNAVAILABLE"}); }
function outOfStock(available: number): never {
  throw new HttpsError("failed-precondition", "Requested quantity is unavailable", {reason: "OUT_OF_STOCK", available});
}
