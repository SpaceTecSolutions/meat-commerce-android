import {createHmac, randomBytes, timingSafeEqual} from "node:crypto";
import {defineSecret} from "firebase-functions/params";
import {CallableRequest, HttpsError, onCall} from "firebase-functions/v2/https";
import {FieldValue} from "firebase-admin/firestore";
import {getFirestore} from "./database.js";
import {catalogOptions, requireCustomer, text} from "./catalog-common.js";

export const deliveryOtpKey = defineSecret("DELIVERY_OTP_KEY");
const otpLifetimeMillis = 2 * 60 * 60_000;
const maxAttempts = 5;

type OtpContext = {uid: string; shopId: string; role: "ADMIN" | "DELIVERY"};
type OrderValue = FirebaseFirestore.DocumentData;

function orderRef(shopId: string, orderId: string) {
  return getFirestore().collection("shops").doc(shopId).collection("orders").doc(orderId);
}
export function deliveryOtpRef(shopId: string, orderId: string) {
  return getFirestore().collection("shops").doc(shopId).collection("deliveryOtps").doc(orderId);
}
function requiredOrderId(value: unknown): string {
  const id = text(value);
  if (!id || id.includes("/") || id.length > 128) {
    throw new HttpsError("invalid-argument", "Valid order required");
  }
  return id;
}
function secretValue(): string {
  const value = deliveryOtpKey.value();
  if (value.length < 32) throw new HttpsError("internal", "Delivery verification unavailable");
  return value;
}
function codeFor(orderId: string, sessionId: string, nonce: string): string {
  const digest = createHmac("sha256", secretValue())
    .update(`code|${orderId}|${sessionId}|${nonce}`).digest();
  return (digest.readUInt32BE(0) % 10_000).toString().padStart(4, "0");
}
function hashFor(orderId: string, sessionId: string, code: string): Buffer {
  return createHmac("sha256", secretValue())
    .update(`verify|${orderId}|${sessionId}|${code}`).digest();
}

export function effectiveDeliveryOtpRequired(
  value: OrderValue, superAllowed: boolean, adminEnabled: boolean,
): boolean {
  const role = text(value.assignedDeliveryRole);
  const startedBy = text(value.deliveryStartedByUserId);
  const assigned = role === "ADMIN" ? value.adminDeliveringPersonally === true && !!startedBy :
    role === "DELIVERY" && !!text(value.assignedDeliveryUserId) &&
      text(value.assignedDeliveryUserId) === startedBy;
  return superAllowed && adminEnabled && value.orderStatus === "OUT_FOR_DELIVERY" &&
    value.trackingLifecycle === "ACTIVE" && !!text(value.trackingSessionId) && assigned;
}

export async function otpRequiredInTransaction(
  transaction: FirebaseFirestore.Transaction, shopId: string, value: OrderValue,
): Promise<boolean> {
  const firestore = getFirestore();
  const [features, delivery] = await Promise.all([
    transaction.get(firestore.collection("appConfig").doc("features")),
    transaction.get(firestore.collection("shops").doc(shopId).collection("config").doc("delivery")),
  ]);
  return effectiveDeliveryOtpRequired(value,
    features.get("realtimeTrackingAllowed") === true,
    delivery.get("realtimeTrackingEnabled") === true);
}

export async function prepareDeliveryOtp(shopId: string, orderId: string, sessionId: string) {
  const reference = deliveryOtpRef(shopId, orderId);
  await getFirestore().runTransaction(async (transaction) => {
    const current = await transaction.get(reference);
    if (current.get("sessionId") === sessionId &&
        Number(current.get("expiresAtEpochMillis")) > Date.now()) return;
    const nonce = randomBytes(16).toString("hex");
    transaction.set(reference, {sessionId, nonce,
      otpHash: hashFor(orderId, sessionId, codeFor(orderId, sessionId, nonce)).toString("hex"),
      expiresAtEpochMillis: Date.now() + otpLifetimeMillis,
      attempts: 0, verifiedAtEpochMillis: null, verifiedByUserId: null,
      createdAt: FieldValue.serverTimestamp()});
  });
}

export const customerGetDeliveryOtp = onCall(
  {...catalogOptions, secrets: [deliveryOtpKey]}, async (request) => {
    const customer = await requireCustomer(request);
    const orderId = requiredOrderId(request.data?.orderId);
    const firestore = getFirestore();
    const [order, features, delivery] = await Promise.all([
      orderRef(customer.shopId, orderId).get(),
      firestore.collection("appConfig").doc("features").get(),
      firestore.collection("shops").doc(customer.shopId).collection("config").doc("delivery").get(),
    ]);
    if (!order.exists || order.get("customerId") !== customer.uid) {
      throw new HttpsError("permission-denied", "Order unavailable");
    }
    const value = order.data()!;
    if (!effectiveDeliveryOtpRequired(value,
      features.get("realtimeTrackingAllowed") === true,
      delivery.get("realtimeTrackingEnabled") === true)) {
      throw new HttpsError("failed-precondition", "Delivery verification is unavailable");
    }
    const sessionId = text(value.trackingSessionId);
    await prepareDeliveryOtp(customer.shopId, orderId, sessionId);
    const otp = await deliveryOtpRef(customer.shopId, orderId).get();
    if (otp.get("sessionId") !== sessionId ||
        Number(otp.get("expiresAtEpochMillis")) <= Date.now()) {
      throw new HttpsError("failed-precondition", "Delivery verification is unavailable");
    }
    return {code: codeFor(orderId, sessionId, text(otp.get("nonce"))),
      expiresAtEpochMillis: Number(otp.get("expiresAtEpochMillis")), sessionId};
  });

export const verifyDeliveryOtp = onCall(
  {...catalogOptions, secrets: [deliveryOtpKey]}, async (request) => {
    const actor = await requireDeliveryActor(request);
    const orderId = requiredOrderId(request.data?.orderId);
    const entered = text(request.data?.code);
    if (!/^\d{4}$/.test(entered)) {
      throw new HttpsError("invalid-argument", "Enter the four-digit delivery code");
    }
    const result = await getFirestore().runTransaction(async (transaction) => {
      const [order, otp] = await Promise.all([
        transaction.get(orderRef(actor.shopId, orderId)),
        transaction.get(deliveryOtpRef(actor.shopId, orderId)),
      ]);
      if (!order.exists || !canActorComplete(order.data()!, actor)) {
        throw new HttpsError("permission-denied", "Delivery assignment unavailable");
      }
      const value = order.data()!;
      if (!await otpRequiredInTransaction(transaction, actor.shopId, value)) {
        throw new HttpsError("failed-precondition", "Delivery verification is unavailable");
      }
      if (!otp.exists || otp.get("sessionId") !== value.trackingSessionId ||
          Number(otp.get("expiresAtEpochMillis")) <= Date.now()) {
        return {verified: false, reason: "OTP_EXPIRED"};
      }
      const attempts = Number(otp.get("attempts")) || 0;
      if (attempts >= maxAttempts) return {verified: false, reason: "OTP_LOCKED"};
      const expected = Buffer.from(text(otp.get("otpHash")), "hex");
      const actual = hashFor(orderId, text(value.trackingSessionId), entered);
      if (expected.length !== actual.length || !timingSafeEqual(expected, actual)) {
        transaction.update(otp.ref, {attempts: attempts + 1});
        return {verified: false, reason: attempts + 1 >= maxAttempts ? "OTP_LOCKED" : "INCORRECT_OTP"};
      }
      transaction.update(otp.ref, {verifiedAtEpochMillis: Date.now(), verifiedByUserId: actor.uid});
      return {verified: true};
    });
    return result;
  });

export async function requireVerifiedOtpForCompletion(
  transaction: FirebaseFirestore.Transaction, shopId: string, orderId: string,
  value: OrderValue, actorUid: string,
) {
  if (!await otpRequiredInTransaction(transaction, shopId, value)) return;
  const otp = await transaction.get(deliveryOtpRef(shopId, orderId));
  if (!otp.exists || otp.get("sessionId") !== value.trackingSessionId ||
      Number(otp.get("expiresAtEpochMillis")) <= Date.now() ||
      Number(otp.get("verifiedAtEpochMillis")) <= 0 ||
      otp.get("verifiedByUserId") !== actorUid) {
    throw new HttpsError("failed-precondition", "Verify the customer delivery code first",
      {reason: "DELIVERY_OTP_REQUIRED"});
  }
}

function canActorComplete(value: OrderValue, actor: OtpContext) {
  return value.orderStatus === "OUT_FOR_DELIVERY" && value.deliveryStartedByUserId === actor.uid &&
    (actor.role === "ADMIN" ? value.adminDeliveringPersonally === true &&
      value.assignedDeliveryRole === "ADMIN" :
      value.assignedDeliveryRole === "DELIVERY" && value.assignedDeliveryUserId === actor.uid);
}
async function requireDeliveryActor(request: CallableRequest): Promise<OtpContext> {
  if (!request.auth) throw new HttpsError("unauthenticated", "Authentication required");
  const uid = request.auth.uid;
  const profile = await getFirestore().collection("users").doc(uid).get();
  const role = text(profile.get("role"));
  if (!profile.exists || profile.get("active") !== true || !["ADMIN", "DELIVERY"].includes(role)) {
    throw new HttpsError("permission-denied", "Delivery access required");
  }
  return {uid, shopId: text(profile.get("shopId"), "default"), role: role as OtpContext["role"]};
}
