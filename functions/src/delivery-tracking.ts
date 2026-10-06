import {randomUUID} from "node:crypto";
import {getDatabase} from "firebase-admin/database";
import {FieldValue} from "firebase-admin/firestore";
import {CallableRequest, HttpsError, onCall} from "firebase-functions/v2/https";
import {catalogOptions, integer, number, text} from "./catalog-common.js";
import {getFirestore} from "./database.js";
import {orderResponse} from "./admin-orders.js";
import {deliveryOtpKey, deliveryOtpRef, prepareDeliveryOtp,
  requireVerifiedOtpForCompletion} from "./delivery-otp.js";

type ActorContext = {uid: string; shopId: string; role: "ADMIN" | "DELIVERY"};

export const deliveryGetAssignedCodOrders = onCall(catalogOptions, async (request) => {
  const context = await requireActor(request, "DELIVERY");
  const snapshot = await orders(context.shopId)
    .where("assignedDeliveryUserId", "==", context.uid).limit(250).get();
  return {orders: snapshot.docs.map((doc) => orderResponse(doc.id, doc.data()))
    .sort((a, b) => Number(b.createdAtEpochMillis) - Number(a.createdAtEpochMillis))};
});

export const deliveryStartAssignedOrder = onCall(catalogOptions, async (request) => {
  const context = await requireActor(request, "DELIVERY");
  return mutateAssigned(request, context, async (value, transaction) => {
    if (value.orderStatus !== "PREPARING" ||
        number(value.readyForDeliveryAtEpochMillis) <= 0) invalidTransition();
    const tracking = await trackingPreparation(context.shopId, transaction);
    const now = Date.now();
    return {orderStatus: "OUT_FOR_DELIVERY", deliveryStartedAtEpochMillis: now,
      outForDeliveryAtEpochMillis: now, deliveryStartedByUserId: context.uid, ...tracking};
  }, "ORDER_OUT_FOR_DELIVERY");
});

export const deliveryCompleteAssignedOrder = onCall(catalogOptions, async (request) => {
  const context = await requireActor(request, "DELIVERY");
  return mutateAssigned(request, context, async (value, transaction) => {
    if (value.orderStatus !== "OUT_FOR_DELIVERY" ||
        value.deliveryStartedByUserId !== context.uid) invalidTransition();
    const method = text(request.data?.paymentMethod);
    if (method !== text(value.paymentMethod)) invalidTransition();
    const orderId = requiredId(request.data?.orderId);
    await requireVerifiedOtpForCompletion(transaction, context.shopId, orderId, value, context.uid);
    const update: Record<string, unknown> = {};
    let paymentStatus = text(value.paymentStatus);
    if (method === "COD") {
      const collected = number(request.data?.collectedMinor, -1);
      const due = number(value.amountDueMinor);
      if (paymentStatus !== "PENDING" || collected < due) {
        throw new HttpsError("failed-precondition", "Collected amount is below COD due",
          {reason: "COD_AMOUNT_MISMATCH"});
      }
      paymentStatus = "COLLECTED";
      Object.assign(update, {collectedMinor: collected, changeReturnedMinor: collected - due,
        collectedAtEpochMillis: Date.now(),
        collectedByUserId: context.uid});
    } else if (paymentStatus !== "PAID") invalidTransition();
    transaction.delete(deliveryOtpRef(context.shopId, orderId));
    return {...update, orderStatus: "DELIVERED", paymentStatus, deliveredAtEpochMillis: Date.now(),
      deliveredAt: FieldValue.serverTimestamp(), deliveredByUserId: context.uid,
      trackingLifecycle: "STOPPED"};
  }, "ORDER_DELIVERED");
});

export const deliveryReportCodMismatch = onCall(catalogOptions, async (request) => {
  const context = await requireActor(request, "DELIVERY");
  return mutateAssigned(request, context, async (value) => {
    const actual = number(request.data?.actualMinor, -1);
    if (value.orderStatus !== "OUT_FOR_DELIVERY" || value.paymentMethod !== "COD" ||
        actual < 0 || actual === number(value.amountDueMinor)) invalidTransition();
    return {codMismatchReported: true, codMismatchActualMinor: actual,
      codMismatchReportedAtEpochMillis: Date.now(), codMismatchReportedBy: context.uid};
  }, "COD_AMOUNT_MISMATCH_REPORTED");
});

export const activateDeliveryTracking = onCall(
  {...catalogOptions, secrets: [deliveryOtpKey]}, async (request) => {
  const context = await requireActor(request);
  const orderId = requiredId(request.data?.orderId);
  const sessionId = requiredId(request.data?.trackingSessionId);
  const firestore = getFirestore();
  const reference = orders(context.shopId).doc(orderId);
  const [order, features, delivery] = await Promise.all([
    reference.get(),
    firestore.collection("appConfig").doc("features").get(),
    firestore.collection("shops").doc(context.shopId).collection("config").doc("delivery").get(),
  ]);
  if (!order.exists || features.get("realtimeTrackingAllowed") !== true ||
      delivery.get("realtimeTrackingEnabled") !== true) trackingUnavailable();
  const value = order.data()!;
  if (value.orderStatus !== "OUT_FOR_DELIVERY" || value.trackingSessionId !== sessionId ||
      !["READY", "ACTIVE"].includes(text(value.trackingLifecycle)) ||
      value.deliveryStartedByUserId !== context.uid || !writerMatches(value, context)) {
    trackingUnavailable();
  }
  const customerUid = text(value.customerId);
  if (!customerUid) trackingUnavailable();
  await prepareDeliveryOtp(context.shopId, orderId, sessionId);
  await getDatabase().ref("trackingAuthorizations").child(sessionId).set({
    orderId, customerUid, writerUid: context.uid, writerRole: context.role,
    active: true, updatedAtEpochMillis: Date.now(),
  });
  if (value.trackingLifecycle !== "ACTIVE") {
    await reference.update({trackingLifecycle: "ACTIVE", updatedAt: FieldValue.serverTimestamp()});
  }
  return {active: true};
});

export const stopDeliveryTracking = onCall(catalogOptions, async (request) => {
  const context = await requireActor(request);
  const orderId = requiredId(request.data?.orderId);
  const sessionId = requiredId(request.data?.trackingSessionId);
  const reference = orders(context.shopId).doc(orderId);
  const order = await reference.get();
  if (!order.exists || order.get("trackingSessionId") !== sessionId ||
      order.get("deliveryStartedByUserId") !== context.uid) trackingUnavailable();
  await Promise.all([
    getDatabase().ref("activeDeliveryTracking").child(sessionId).remove(),
    getDatabase().ref("trackingAuthorizations").child(sessionId).update({
      active: false, updatedAtEpochMillis: Date.now(),
    }),
    reference.update({trackingLifecycle: "STOPPED", updatedAt: FieldValue.serverTimestamp()}),
  ]);
  return {active: false};
});

async function mutateAssigned(
  request: CallableRequest,
  context: ActorContext,
  build: (value: FirebaseFirestore.DocumentData,
    transaction: FirebaseFirestore.Transaction) => Promise<Record<string, unknown>>,
  action: string,
) {
  const orderId = requiredId(request.data?.orderId);
  const expectedRevision = integer(request.data?.expectedRevision, -1);
  if (expectedRevision < 0) throw new HttpsError("invalid-argument", "Revision required");
  const firestore = getFirestore();
  const reference = orders(context.shopId).doc(orderId);
  const result = await firestore.runTransaction(async (transaction) => {
    const snapshot = await transaction.get(reference);
    if (!snapshot.exists || snapshot.get("assignedDeliveryUserId") !== context.uid ||
        snapshot.get("assignedDeliveryRole") !== "DELIVERY") {
      throw new HttpsError("permission-denied", "Order is not assigned to this delivery user");
    }
    if (integer(snapshot.get("revision")) !== expectedRevision) invalidTransition();
    const update = await build(snapshot.data()!, transaction);
    const revision = expectedRevision + 1;
    transaction.update(reference, {...update, revision, updatedAt: FieldValue.serverTimestamp(),
      updatedBy: context.uid});
    transaction.create(firestore.collection("shops").doc(context.shopId)
      .collection("auditLogs").doc(), {action, actorUid: context.uid, targetOrderId: orderId,
      createdAt: FieldValue.serverTimestamp()});
    return orderResponse(orderId, {...snapshot.data(), ...update, revision});
  });
  return {order: result};
}

async function trackingPreparation(
  shopId: string,
  transaction: FirebaseFirestore.Transaction,
) {
  const firestore = getFirestore();
  const [features, delivery] = await Promise.all([
    transaction.get(firestore.collection("appConfig").doc("features")),
    transaction.get(firestore.collection("shops").doc(shopId).collection("config").doc("delivery")),
  ]);
  const enabled = features.get("realtimeTrackingAllowed") === true &&
    delivery.get("realtimeTrackingEnabled") === true;
  return enabled ? {trackingSessionId: randomUUID(), trackingLifecycle: "READY"} :
    {trackingSessionId: null, trackingLifecycle: "DISABLED"};
}

async function requireActor(
  request: CallableRequest,
  requiredRole?: "ADMIN" | "DELIVERY",
): Promise<ActorContext> {
  if (!request.auth) throw new HttpsError("unauthenticated", "Authentication required");
  const profile = await getFirestore().collection("users").doc(request.auth.uid).get();
  const role = text(profile.get("role")) as "ADMIN" | "DELIVERY";
  if (!profile.exists || profile.get("active") !== true ||
      !["ADMIN", "DELIVERY"].includes(role) || (requiredRole && role !== requiredRole)) {
    throw new HttpsError("permission-denied", "Active delivery access required");
  }
  return {uid: request.auth.uid, shopId: text(profile.get("shopId"), "default"), role};
}

function writerMatches(value: FirebaseFirestore.DocumentData, context: ActorContext) {
  return context.role === "ADMIN" ?
    value.adminDeliveringPersonally === true && value.assignedDeliveryRole === "ADMIN" :
    value.assignedDeliveryUserId === context.uid && value.assignedDeliveryRole === "DELIVERY";
}
function orders(shopId: string) {
  return getFirestore().collection("shops").doc(shopId).collection("orders");
}
function requiredId(value: unknown) {
  const id = text(value);
  if (!id || id.includes("/") || id.length > 128) {
    throw new HttpsError("invalid-argument", "Valid order and tracking session required");
  }
  return id;
}
function invalidTransition(): never {
  throw new HttpsError("aborted", "Order changed", {reason: "INVALID_TRANSITION"});
}
function trackingUnavailable(): never {
  throw new HttpsError("failed-precondition", "Live tracking is unavailable",
    {reason: "TRACKING_UNAVAILABLE"});
}
