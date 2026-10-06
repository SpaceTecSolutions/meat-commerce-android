import {FieldValue} from "firebase-admin/firestore";
import {getFirestore} from "./database.js";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {orderResponse} from "./admin-orders.js";
import {catalogOptions, integer, number, requireCustomer, text} from "./catalog-common.js";

export const customerGetOrders = onCall(catalogOptions, async (request) => {
  const context = await requireCustomer(request);
  const firestore = getFirestore();
  const [snapshot, features, delivery, shopProfile] = await Promise.all([
    orders(context.shopId).where("customerId", "==", context.uid).limit(250).get(),
    firestore.collection("appConfig").doc("features").get(),
    firestore.collection("shops").doc(context.shopId).collection("config").doc("delivery").get(),
    firestore.collection("shops").doc(context.shopId).get(),
  ]);
  const trackingEnabled = features.get("realtimeTrackingAllowed") === true &&
    delivery.get("realtimeTrackingEnabled") === true;
  return {orders: snapshot.docs.map((document) => {
    const value = document.data();
    return {...orderResponse(document.id, value),
      shopName: text(shopProfile.get("shopName"), text(shopProfile.get("displayName"), "Meat Station")),
      shopAddress: text(shopProfile.get("address")),
      realtimeTrackingAvailable: trackingEnabled && realtimeOrderEligible(value)};
  })
    .sort((a, b) => (b.createdAtEpochMillis ?? 0) - (a.createdAtEpochMillis ?? 0))};
});

export const customerCancelOrder = onCall(catalogOptions, async (request) => {
  const context = await requireCustomer(request); const orderId = text(request.data?.orderId);
  const expectedRevision = integer(request.data?.expectedRevision, -1);
  const reason = text(request.data?.reason).trim();
  if (!orderId || expectedRevision < 0 || !reason || reason.length > 200) {
    throw new HttpsError("invalid-argument", "Order, revision and cancellation reason required");
  }
  const reference = orders(context.shopId).doc(orderId);
  const result = await getFirestore().runTransaction(async (transaction) => {
    const snapshot = await transaction.get(reference);
    if (!snapshot.exists || snapshot.get("customerId") !== context.uid) notFound();
    if (integer(snapshot.get("revision")) !== expectedRevision) changed();
    if (snapshot.get("customerCancellationAllowed") !== true ||
        !["PENDING", "CONFIRMED"].includes(text(snapshot.get("orderStatus")))) {
      throw new HttpsError("failed-precondition", "Cancellation window closed", {reason: "CANCELLATION_WINDOW_CLOSED"});
    }
    const revision = expectedRevision + 1; const update = {orderStatus: "CANCELLED", cancelReason: reason,
      cancelledAtEpochMillis: Date.now(), cancelledAt: FieldValue.serverTimestamp(),
      cancelledByUserId: context.uid, cancelledByRole: "CUSTOMER", customerCancellationAllowed: false,
      revision, updatedAt: FieldValue.serverTimestamp(), updatedBy: context.uid};
    transaction.update(reference, update);
    return orderResponse(orderId, {...snapshot.data(), ...update});
  });
  return {order: result};
});

export const customerReorder = onCall(catalogOptions, async (request) => {
  const context = await requireCustomer(request); const orderId = text(request.data?.orderId);
  if (!orderId) throw new HttpsError("invalid-argument", "Order required");
  const firestore = getFirestore(); const userCart = firestore.collection("users").doc(context.uid).collection("cartItems");
  const shop = firestore.collection("shops").doc(context.shopId);
  const result = await firestore.runTransaction(async (transaction) => {
    const order = await transaction.get(orders(context.shopId).doc(orderId));
    if (!order.exists || order.get("customerId") !== context.uid) notFound();
    if (!["DELIVERED", "CANCELLED"].includes(text(order.get("orderStatus")))) {
      throw new HttpsError("failed-precondition", "Order cannot be reordered", {reason: "NO_REORDERABLE_ITEMS"});
    }
    const snapshots = Array.isArray(order.get("items")) ? order.get("items") as FirebaseFirestore.DocumentData[] : [];
    const productIds = [...new Set(snapshots.map((item) => text(item.productId)).filter(Boolean))].slice(0, 100);
    const [products, currentCart] = await Promise.all([
      Promise.all(productIds.map((id) => transaction.get(shop.collection("products").doc(id)))),
      transaction.get(userCart.limit(100)),
    ]);
    const current = new Map(currentCart.docs.map((line) => [line.id, integer(line.get("quantity"), 1)]));
    let addedItemCount = 0;
    products.forEach((product) => {
      if (!product.exists || product.get("active") !== true || product.get("archived") === true) return;
      const stock = Math.min(99, Math.floor(number(product.get("stockQuantity"))));
      const ordered = Math.max(1, integer(snapshots.find((item) => text(item.productId) === product.id)?.quantity, 1));
      const quantity = Math.min(stock, (current.get(product.id) ?? 0) + ordered);
      const regularPriceMinor = integer(product.get("priceMinor")); const offer = integer(product.get("offerPriceMinor"));
      if (quantity < 1 || regularPriceMinor < 1) return;
      const unitPriceMinor = offer > 0 && offer < regularPriceMinor ? offer : regularPriceMinor;
      transaction.set(userCart.doc(product.id), {productId: product.id, shopId: context.shopId,
        name: text(product.get("name")), categoryId: text(product.get("categoryId")),
        unit: text(product.get("unit"), "PIECE"), imageUrl: firstImage(product.get("imageUrls")),
        regularPriceMinor, unitPriceMinor, quantity, updatedAt: FieldValue.serverTimestamp()}, {merge: true});
      addedItemCount += 1; current.set(product.id, quantity);
    });
    if (!addedItemCount) {
      throw new HttpsError("failed-precondition", "No items are currently available", {reason: "NO_REORDERABLE_ITEMS"});
    }
    return {addedItemCount, cartQuantity: current.size};
  });
  return result;
});

function orders(shopId: string) {
  return getFirestore().collection("shops").doc(shopId).collection("orders");
}
export function realtimeOrderEligible(value: FirebaseFirestore.DocumentData) {
  const assigned = value.adminDeliveringPersonally === true ||
    (text(value.assignedDeliveryUserId) && ["ADMIN", "DELIVERY"].includes(text(value.assignedDeliveryRole)));
  return value.orderStatus === "OUT_FOR_DELIVERY" && Boolean(assigned) &&
    Boolean(text(value.deliveryStartedByUserId)) && Boolean(text(value.trackingSessionId)) &&
    text(value.trackingLifecycle) === "ACTIVE";
}
function firstImage(value: unknown) {
  return Array.isArray(value) && typeof value[0] === "string" ? value[0] : null;
}
function notFound(): never { throw new HttpsError("not-found", "Order not found"); }
function changed(): never { throw new HttpsError("aborted", "Order changed", {reason: "INVALID_TRANSITION"}); }
