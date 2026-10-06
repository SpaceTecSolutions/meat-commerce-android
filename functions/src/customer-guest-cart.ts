import {FieldValue} from "firebase-admin/firestore";
import {getFirestore} from "./database.js";
import {catalogOptions, integer, number, requireCustomer, publicCustomerShop, text} from "./catalog-common.js";
import {cartResponse, calculateDeliveryFee} from "./customer-cart.js";
import {HttpsError, onCall} from "firebase-functions/v2/https";

function requestedLines(value: unknown): Array<{productId: string; quantity: number}> {
  if (!Array.isArray(value) || value.length > 100) throw new HttpsError("invalid-argument", "Invalid cart");
  const ids = new Set<string>();
  return value.map((line) => {
    const productId = text(line?.productId);
    const quantity = integer(line?.quantity, -1);
    if (!/^[a-zA-Z0-9_-]{1,128}$/.test(productId) || quantity < 1 || quantity > 99 || ids.has(productId)) {
      throw new HttpsError("invalid-argument", "Invalid cart line");
    }
    ids.add(productId);
    return {productId, quantity};
  });
}

export const customerQuoteGuestCart = onCall(catalogOptions, async (request) => {
  const lines = requestedLines(request.data?.lines);
  const shop = getFirestore().collection("shops").doc(await publicCustomerShop(request));
  const [products, delivery] = await Promise.all([
    lines.length ? getFirestore().getAll(...lines.map((line) => shop.collection("products").doc(line.productId))) : [],
    shop.collection("config").doc("delivery").get(),
  ]);
  const output = lines.map((line, index) => {
    const product = products[index]; const value = product?.data();
    const available = value?.active === true && value.archived !== true && number(value.stockQuantity) > 0;
    const maxQuantity = available ? Math.min(99, Math.floor(number(value.stockQuantity))) : 0;
    const regularPriceMinor = integer(value?.priceMinor);
    const offer = integer(value?.offerPriceMinor);
    return {productId: line.productId, quantity: Math.min(line.quantity, Math.max(1, maxQuantity)),
      name: available ? text(value?.name, "Product") : "Product unavailable",
      imageUrl: available && Array.isArray(value?.imageUrls) ? value.imageUrls[0] ?? null : null,
      unit: available ? text(value?.unit, "PIECE") : "PIECE", regularPriceMinor: available ? regularPriceMinor : 0,
      unitPriceMinor: available ? (offer > 0 && offer < regularPriceMinor ? offer : regularPriceMinor) : 0,
      maxQuantity, available: available && regularPriceMinor > 0};
  });
  const subtotalMinor = output.reduce((sum, line) => sum + line.unitPriceMinor * line.quantity, 0);
  const threshold = delivery.get("freeDeliveryThresholdMinor");
  const deliveryFeeMinor = calculateDeliveryFee(subtotalMinor,
    Math.max(0, integer(delivery.get("deliveryChargeMinor"))), threshold == null ? null : Math.max(0, integer(threshold)));
  return {cart: {lines: output, subtotalMinor, discountMinor: 0, deliveryFeeMinor,
    totalMinor: subtotalMinor + deliveryFeeMinor, minimumOrderMinor: Math.max(0, integer(delivery.get("minimumOrderMinor"))),
    deliveryAddress: null, adjustments: [], deliveryEstimate: null}};
});

export const customerMergeGuestCart = onCall(catalogOptions, async (request) => {
  const context = await requireCustomer(request);
  const lines = requestedLines(request.data?.lines);
  const firestore = getFirestore();
  const shop = firestore.collection("shops").doc(context.shopId);
  await firestore.runTransaction(async (transaction) => {
    const refs = lines.map((line) => ({...line, product: shop.collection("products").doc(line.productId),
      cart: firestore.collection("users").doc(context.uid).collection("cartItems").doc(line.productId)}));
    const snapshots = await Promise.all(refs.map(async (item) => ({
      product: await transaction.get(item.product), cart: await transaction.get(item.cart),
    })));
    refs.forEach((item, index) => {
      const {product, cart} = snapshots[index]; const value = product.data();
      if (value?.active !== true || value.archived === true || number(value.stockQuantity) < 1) return;
      // Retry-safe merge: retain the larger quantity rather than adding twice.
      const quantity = Math.min(99, Math.floor(number(value.stockQuantity)),
        Math.max(item.quantity, integer(cart.get("quantity"))));
      const regularPriceMinor = integer(value.priceMinor);
      const offer = integer(value.offerPriceMinor);
      if (regularPriceMinor <= 0) return;
      transaction.set(item.cart, {productId: item.productId, shopId: context.shopId, quantity,
        name: text(value.name), categoryId: text(value.categoryId), unit: text(value.unit, "PIECE"),
        imageUrl: Array.isArray(value.imageUrls) ? value.imageUrls[0] ?? null : null,
        regularPriceMinor, unitPriceMinor: offer > 0 && offer < regularPriceMinor ? offer : regularPriceMinor,
        updatedAt: FieldValue.serverTimestamp()}, {merge: true});
    });
  });
  return {cart: await cartResponse(context)};
});
