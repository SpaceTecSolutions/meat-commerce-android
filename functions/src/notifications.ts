import {FieldValue, Timestamp} from "firebase-admin/firestore";
import {onDocumentCreated, onDocumentUpdated, onDocumentWritten} from "firebase-functions/v2/firestore";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {FIRESTORE_DATABASE_ID, getFirestore} from "./database.js";
import {money, notify, orderNumber, text, tokenId, usersForRole, usersForShop} from "./notification-core.js";

const region = "asia-south1";
const callable = {region, enforceAppCheck: true, timeoutSeconds: 30, memory: "256MiB" as const};
const trigger = {region, database: FIRESTORE_DATABASE_ID, document: "shops/{shopId}/orders/{orderId}"};

export const registerNotificationDevice = onCall(callable, async (request) => {
  const profile = await activeProfile(request);
  const token = text(request.data?.token).trim();
  if (!token || token.length > 4096) throw new HttpsError("invalid-argument", "Valid token required");
  const id = tokenId(token); const firestore = getFirestore();
  const target = firestore.collection("users").doc(request.auth!.uid).collection("devices").doc(id);
  const owner = firestore.collection("notificationDeviceOwners").doc(id);
  await firestore.runTransaction(async (transaction) => {
    const ownerSnapshot = await transaction.get(owner);
    const current = await transaction.get(target);
    const previousUid = text(ownerSnapshot.get("uid"));
    if (previousUid && previousUid !== request.auth!.uid) {
      transaction.delete(firestore.collection("users").doc(previousUid).collection("devices").doc(id));
    }
    transaction.set(target, {
      fcmToken: token, platform: "ANDROID", role: profile.role, shopId: profile.shopId,
      isActive: true, updatedAt: FieldValue.serverTimestamp(),
      ...(current.exists ? {} : {createdAt: FieldValue.serverTimestamp()}),
    }, {merge: true});
    transaction.set(owner, {uid: request.auth!.uid, updatedAt: FieldValue.serverTimestamp()});
  });
  return {registered: true};
});

export const unregisterNotificationDevice = onCall(callable, async (request) => {
  await activeProfile(request); const token = text(request.data?.token).trim();
  if (token) {
    const firestore = getFirestore(); const id = tokenId(token);
    const device = firestore.collection("users").doc(request.auth!.uid).collection("devices").doc(id);
    const owner = firestore.collection("notificationDeviceOwners").doc(id);
    await firestore.runTransaction(async (transaction) => {
      const ownerSnapshot = await transaction.get(owner);
      transaction.set(device, {isActive: false, updatedAt: FieldValue.serverTimestamp()}, {merge: true});
      if (ownerSnapshot.get("uid") === request.auth!.uid) transaction.delete(owner);
    });
  }
  return {unregistered: true};
});

export const getNotificationHistory = onCall(callable, async (request) => {
  await activeProfile(request); const limit = Math.min(100, Math.max(1, Number(request.data?.limit ?? 50)));
  if ((await getFirestore().collection("appConfig").doc("features").get())
    .get("inAppNotificationsEnabled") === false) return {notifications: []};
  const snapshot = await getFirestore().collection("users").doc(request.auth!.uid).collection("notifications")
    .orderBy("createdAt", "desc").limit(limit).get();
  return {notifications: snapshot.docs.map((doc) => response(doc))};
});

export const markNotificationRead = onCall(callable, async (request) => {
  await activeProfile(request); const id = text(request.data?.notificationId);
  if (!id) throw new HttpsError("invalid-argument", "Notification required");
  const reference = getFirestore().collection("users").doc(request.auth!.uid).collection("notifications").doc(id);
  if (!(await reference.get()).exists) throw new HttpsError("not-found", "Notification not found");
  await reference.update({readAt: FieldValue.serverTimestamp()}); return {success: true};
});

export const markAllNotificationsRead = onCall(callable, async (request) => {
  await activeProfile(request); const base = getFirestore().collection("users").doc(request.auth!.uid).collection("notifications");
  let updated = 0;
  while (true) {
    const snapshot = await base.where("readAt", "==", null).limit(400).get();
    if (snapshot.empty) break;
    const batch = getFirestore().batch(); snapshot.docs.forEach((doc) => batch.update(doc.ref, {readAt: FieldValue.serverTimestamp()}));
    await batch.commit(); updated += snapshot.size;
  }
  return {updated};
});

export const clearAllNotifications = onCall(callable, async (request) => {
  await activeProfile(request);
  const cutoff = Timestamp.now();
  const base = getFirestore().collection("users").doc(request.auth!.uid).collection("notifications");
  let deleted = 0;
  // Fixed cutoff preserves notifications arriving during this operation.
  for (let page = 0; page < 20; page++) {
    const snapshot = await base.where("createdAt", "<=", cutoff).limit(400).get();
    if (snapshot.empty) return {deleted};
    const batch = getFirestore().batch();
    snapshot.docs.forEach((doc) => batch.delete(doc.ref));
    await batch.commit(); deleted += snapshot.size;
  }
  return {deleted};
});

export const notifyOrderCreated = onDocumentCreated(trigger, async (event) => {
  const value = event.data?.data(); if (!value) return;
  if (!isOrderAccepted(value)) return;
  await notifyAcceptedOrder(value, event.params.shopId, event.params.orderId);
});

async function notifyAcceptedOrder(value: Record<string, any>, shopId: string, orderId: string) {
  const number = orderNumber(value, orderId);
  await notify({recipientUid: text(value.customerId), recipientRole: "CUSTOMER", event: "ORDER_PLACED",
    category: "ORDER", priority: "HIGH", title: "Order Placed",
    body: `Your order ${number} has been placed and is awaiting confirmation.`, eventKey: `${orderId}:created:customer`,
    shopId, orderId, deepLinkRoute: "customer/orders"});
  const admins = await usersForShop(shopId, "ADMIN");
  await Promise.all(admins.map((uid) => notify({recipientUid: uid, recipientRole: "ADMIN", event: "NEW_ORDER",
    category: "ORDER", priority: "HIGH", title: "New Order",
    body: `${number} • ${money(value.totalMinor ?? value.amountDueMinor, text(value.currencyCode, "INR"))} • ${items(value).length} items`,
    eventKey: `${orderId}:created:admin:${uid}`, shopId, orderId, deepLinkRoute: "admin/orders"})));
}

export function isOrderAccepted(value: Record<string, any>): boolean {
  return value.paymentMethod === "COD" || value.paymentStatus === "PAID";
}

export const notifyOrderUpdated = onDocumentUpdated(trigger, async (event) => {
  const before = event.data?.before.data(); const after = event.data?.after.data();
  if (!before || !after) return;
  if (after.paymentMethod !== "COD" && before.paymentStatus !== "PAID" && after.paymentStatus === "PAID") {
    await notifyAcceptedOrder(after, event.params.shopId, event.params.orderId);
  }
  const shopId = event.params.shopId; const orderId = event.params.orderId; const number = orderNumber(after, orderId);
  const revision = Number(after.revision ?? 0); const customerUid = text(after.customerId);
  const finalAmountChanged = Number(before.totalMinor ?? before.amountDueMinor ?? 0) !==
    Number(after.finalTotalMinor ?? after.totalMinor ?? after.amountDueMinor ?? 0);
  if (before.orderStatus === "PENDING" && after.orderStatus === "CONFIRMED" &&
      after.finalBillAdjusted === true && finalAmountChanged) {
    await notify({recipientUid: customerUid, recipientRole: "CUSTOMER", event: "ORDER_FINAL_AMOUNT_UPDATED",
      category: "ORDER", priority: "HIGH", title: "Order updated",
      body: `The final weight was adjusted. Your final amount is ${money(after.finalTotalMinor ?? after.totalMinor,
        text(after.currencyCode, "INR"))} and ${number} is confirmed.`,
      eventKey: `${orderId}:final-amount:${revision}`, shopId, orderId, deepLinkRoute: "customer/orders"});
  }
  if (before.orderStatus !== after.orderStatus) {
    const status = statusNotification(text(after.orderStatus), number);
    if (status) await notify({recipientUid: customerUid, recipientRole: "CUSTOMER", ...status,
      eventKey: `${orderId}:status:${after.orderStatus}:${revision}`, shopId, orderId, deepLinkRoute: "customer/orders"});
    if (after.orderStatus === "CANCELLED" && after.cancelledByRole === "CUSTOMER") {
      await notifyAdmins(shopId, orderId, "CUSTOMER_CANCELLED_ORDER", "Customer Cancelled Order",
        `Customer cancelled order ${number}.`, `${orderId}:customer-cancelled:${revision}`);
    }
    if (after.orderStatus === "CANCELLED" && text(before.assignedDeliveryUserId)) {
      await notify({recipientUid: text(before.assignedDeliveryUserId), recipientRole: "DELIVERY",
        event: "DELIVERY_ASSIGNMENT_CANCELLED", category: "DELIVERY", priority: "HIGH",
        title: "Assignment Cancelled", body: `${number} is no longer assigned to you.`,
        eventKey: `${orderId}:assignment-cancelled:${revision}`, shopId, orderId, deepLinkRoute: "delivery/orders"});
    }
  }
  if (before.paymentStatus !== after.paymentStatus) await paymentNotifications(shopId, orderId, number, after, revision);
  if (before.assignedDeliveryUserId !== after.assignedDeliveryUserId) {
    const oldUid = text(before.assignedDeliveryUserId); const newUid = text(after.assignedDeliveryUserId);
    if (oldUid) await notify({recipientUid: oldUid, recipientRole: "DELIVERY", event: "DELIVERY_REASSIGNED",
      category: "DELIVERY", priority: "HIGH", title: "Delivery Reassigned", body: `${number} is no longer assigned to you.`,
      eventKey: `${orderId}:unassigned:${revision}:${oldUid}`, shopId, orderId, deepLinkRoute: "delivery/orders"});
    if (newUid) await notify({recipientUid: newUid, recipientRole: "DELIVERY", event: "DELIVERY_ASSIGNED",
      category: "DELIVERY", priority: "HIGH", title: "New Assignment", body: `${number} has been assigned to you.`,
      eventKey: `${orderId}:assigned:${revision}:${newUid}`, shopId, orderId, deepLinkRoute: "delivery/orders"});
  }
  if (before.codMismatchReported !== true && after.codMismatchReported === true) {
    await notifyAdmins(shopId, orderId, "DELIVERY_ISSUE", "Delivery Issue",
      `Collection issue reported for order ${number}.`, `${orderId}:delivery-issue:${revision}`);
  }
});

export const notifyLowStock = onDocumentUpdated(
  {region, database: FIRESTORE_DATABASE_ID, document: "shops/{shopId}/products/{productId}"},
  async (event) => {
    const before = event.data?.before.data(); const after = event.data?.after.data(); if (!before || !after) return;
    const threshold = Number(after.lowStockThreshold); const stock = Number(after.stockQuantity);
    if (!Number.isFinite(threshold) || threshold < 0 || stock > threshold || Number(before.stockQuantity) <= threshold) return;
    const admins = await usersForShop(event.params.shopId, "ADMIN");
    await Promise.all(admins.map((uid) => notify({recipientUid: uid, recipientRole: "ADMIN", event: "LOW_STOCK",
      category: "STOCK", priority: "HIGH", title: "Low Stock",
      body: `${text(after.name, "Product")} has ${stock} ${text(after.unit).toLowerCase()} remaining.`,
      eventKey: `${event.params.productId}:low-stock:${event.data!.after.updateTime.toMillis()}:${uid}`,
      shopId: event.params.shopId, productId: event.params.productId, deepLinkRoute: "admin/products"})));
  },
);

export const notifyFeatureConfigChanged = onDocumentUpdated(
  {region, database: FIRESTORE_DATABASE_ID, document: "appConfig/features"},
  async (event) => {
    const before = event.data?.before.data() ?? {}; const after = event.data?.after.data() ?? {};
    const changed = Object.keys(after).filter((key) => key !== "revision" && key !== "updatedAt" && before[key] !== after[key]);
    if (!changed.length) return; const users = await usersForRole("SUPER_ADMIN"); const revision = Number(after.revision ?? 0);
    const body = changed.slice(0, 3).map((key) => `${label(key)} ${formatChange(before[key], after[key])}`).join(" • ");
    await Promise.all(users.map((uid) => notify({recipientUid: uid, recipientRole: "SUPER_ADMIN",
      event: "FEATURE_CONFIG_CHANGED", category: "SYSTEM", title: "Feature Configuration Updated", body,
      eventKey: `features:${revision}:${uid}`, deepLinkRoute: "super_admin/feature-management"})));
  },
);

export const notifyOfferActivated = onDocumentWritten(
  {region, database: FIRESTORE_DATABASE_ID, document: "shops/{shopId}/offers/{promotionId}"},
  async (event) => {
    const before = event.data?.before.data(); const after = event.data?.after.data();
    if (!after || after.active !== true || before?.active === true) return;
    await promotionNotifications(event.params.shopId, event.params.promotionId, after, "OFFER");
  },
);

export const notifyCouponActivated = onDocumentWritten(
  {region, database: FIRESTORE_DATABASE_ID, document: "shops/{shopId}/coupons/{promotionId}"},
  async (event) => {
    const before = event.data?.before.data(); const after = event.data?.after.data();
    if (!after || after.active !== true || before?.active === true) return;
    await promotionNotifications(event.params.shopId, event.params.promotionId, after, "COUPON");
  },
);

async function activeProfile(request: any) {
  if (!request.auth) throw new HttpsError("unauthenticated", "Authentication required");
  const profile = await getFirestore().collection("users").doc(request.auth.uid).get();
  if (!profile.exists || profile.get("active") !== true) throw new HttpsError("permission-denied", "Active account required");
  return {role: text(profile.get("role")), shopId: text(profile.get("shopId"), "default")};
}
async function promotionNotifications(shopId: string, id: string, value: FirebaseFirestore.DocumentData, kind: string) {
  const features = await getFirestore().collection("appConfig").doc("features").get();
  if (kind === "OFFER" ? features.get("offersAllowed") !== true : features.get("couponsAllowed") !== true) return;
  const customers = await getFirestore().collection("users").where("shopId", "==", shopId)
    .where("role", "==", "CUSTOMER").where("active", "==", true).get();
  const title = text(value.title ?? value.name, kind === "OFFER" ? "New Offer" : "New Coupon").slice(0, 80);
  const body = text(value.message ?? value.description, "A new promotion is available.").slice(0, 240);
  const revision = Number(value.revision ?? 0);
  await Promise.all(customers.docs.filter((doc) => doc.get("notificationPreferences.promotions") !== false).map((doc) =>
    notify({recipientUid: doc.id, recipientRole: "CUSTOMER", event: "PROMOTION", category: "PROMOTION",
      title, body, eventKey: `${kind}:${id}:active:${revision}:${doc.id}`, shopId,
      deepLinkRoute: "customer/categories", metadata: {promotionId: id, kind}})));
}
function response(doc: FirebaseFirestore.QueryDocumentSnapshot) { const value = doc.data(); return {id: doc.id, ...value,
  createdAtEpochMillis: value.createdAt instanceof Timestamp ? value.createdAt.toMillis() : 0,
  readAtEpochMillis: value.readAt instanceof Timestamp ? value.readAt.toMillis() : null}; }
function items(value: FirebaseFirestore.DocumentData) { return Array.isArray(value.items) ? value.items : []; }
export function statusNotification(status: string, number: string): any {
  const values: Record<string, [string, string, string]> = {
    CONFIRMED: ["ORDER_CONFIRMED", "Order Confirmed", `Your order ${number} is confirmed.`],
    PREPARING: ["ORDER_PREPARING", "Preparing", `We're preparing your order ${number}.`],
    OUT_FOR_DELIVERY: ["ORDER_OUT_FOR_DELIVERY", "Out for Delivery", `Your order ${number} is on the way.`],
    DELIVERED: ["ORDER_DELIVERED", "Delivered", `Your order ${number} has been delivered.`],
    CANCELLED: ["ORDER_CANCELLED", "Order Cancelled", `Your order ${number} has been cancelled.`],
  }; const value = values[status]; return value && {event: value[0], category: "ORDER", priority: "HIGH", title: value[1], body: value[2]};
}
async function notifyAdmins(shopId: string, orderId: string, event: string, title: string, body: string, key: string) {
  const admins = await usersForShop(shopId, "ADMIN"); await Promise.all(admins.map((uid) => notify({recipientUid: uid,
    recipientRole: "ADMIN", event, category: "ORDER", priority: "HIGH", title, body, eventKey: `${key}:${uid}`,
    shopId, orderId, deepLinkRoute: "admin/orders"})));
}
async function paymentNotifications(shopId: string, orderId: string, number: string, value: FirebaseFirestore.DocumentData, revision: number) {
  const status = text(value.paymentStatus); const paid = status === "PAID" || status === "COLLECTED";
  const title = paid ? "Payment Received" : status === "REFUNDED" ? "Payment Refunded" : status === "FAILED" ? "Payment Failed" : "Payment Update";
  await notify({recipientUid: text(value.customerId), recipientRole: "CUSTOMER", event: "PAYMENT_STATUS", category: "PAYMENT",
    title, body: paid ? `Payment received for order ${number}.` : `Payment for order ${number} is ${status.toLowerCase()}.`,
    eventKey: `${orderId}:payment:${status}:${revision}`, shopId, orderId, deepLinkRoute: "customer/orders"});
  if (paid) await notifyAdmins(shopId, orderId, "PAYMENT_RECEIVED", "Payment Received",
    `${money(value.totalMinor ?? value.amountDueMinor, text(value.currencyCode, "INR"))} received for order ${number}.`,
    `${orderId}:payment:${status}:${revision}:admin`);
}
function label(key: string) { return key.replace(/([A-Z])/g, " $1").replace(/^./, (it) => it.toUpperCase()); }
function formatChange(before: unknown, after: unknown) { return typeof after === "boolean" ? (after ? "enabled" : "disabled") :
  `changed from ${String(before ?? "unset")} to ${String(after)}`; }
