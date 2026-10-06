import {createHmac, randomUUID, timingSafeEqual} from "node:crypto";
import {FieldValue} from "firebase-admin/firestore";
import {defineSecret} from "firebase-functions/params";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {getFirestore} from "./database.js";
import {catalogOptions, requireAdmin, requireCustomer, text} from "./catalog-common.js";

const keyId = defineSecret("RAZORPAY_KEY_ID");
const keySecret = defineSecret("RAZORPAY_KEY_SECRET");
const options = {...catalogOptions, secrets: [keyId, keySecret]};

type Context = {uid: string; shopId: string};
type AttemptRecord = FirebaseFirestore.DocumentData & {id: string};

export const adminVerifyRazorpayConfiguration = onCall(options, async (request) => {
  const context = await requireAdmin(request);
  await razorpayGet("/orders?count=1");
  const reference = getFirestore().collection("shops").doc(context.shopId).collection("config").doc("payment");
  await reference.set({razorpayConfigured: true, razorpayEnabled: false,
    revision: FieldValue.increment(1), updatedAt: FieldValue.serverTimestamp(), updatedBy: context.uid}, {merge: true});
  return {configured: true};
});

export const customerCreateRazorpayOrder = onCall(options, async (request) => {
  const context = await requireCustomer(request);
  return {session: await createSession(context, validId(request.data?.appOrderId), validId(request.data?.idempotencyKey))};
});

export const customerRetryRazorpayPayment = onCall(options, async (request) => {
  const context = await requireCustomer(request); const prior = await ownedAttempt(context, validId(request.data?.attemptId));
  if (!["FAILED", "CANCELLED"].includes(text(prior.status))) {
    throw failure("Only a failed or cancelled payment can be retried", "PAYMENT_NOT_RETRYABLE");
  }
  return {session: await createSession(context, text(prior.appOrderId), validId(request.data?.idempotencyKey))};
});

export const customerMarkRazorpaySdkOpened = onCall(options, async (request) => {
  const context = await requireCustomer(request); const attempt = await ownedAttempt(context, validId(request.data?.attemptId));
  if (attempt.status === "CREATED") await attempt.ref.update({status: "SDK_OPEN", updatedAt: FieldValue.serverTimestamp()});
  return {attempt: attemptResponse({...attempt, status: attempt.status === "CREATED" ? "SDK_OPEN" : attempt.status})};
});

export const customerVerifyRazorpayPayment = onCall(options, async (request) => {
  const context = await requireCustomer(request); const attempt = await ownedAttempt(context, validId(request.data?.attemptId));
  if (attempt.status === "PAID") return {attempt: attemptResponse(attempt)};
  const paymentId = validId(request.data?.razorpayPaymentId); const callbackOrderId = validId(request.data?.razorpayOrderId);
  const signature = text(request.data?.razorpaySignature); const callbackId = validId(request.data?.callbackId);
  if (callbackOrderId !== attempt.razorpayOrderId || !validSignature(callbackOrderId, paymentId, signature)) {
    throw failure("Payment verification failed", "INVALID_PAYMENT_SIGNATURE");
  }
  const payment = await razorpayGet(`/payments/${encodeURIComponent(paymentId)}`);
  if (payment.order_id !== attempt.razorpayOrderId || Number(payment.amount) !== Number(attempt.amountMinor) ||
      text(payment.currency) !== text(attempt.currencyCode)) throw failure("Payment details do not match the order", "PAYMENT_MISMATCH");
  if (payment.status !== "captured") {
    await attempt.ref.update({status: "VERIFICATION_PENDING", callbackId, razorpayPaymentId: paymentId,
      updatedAt: FieldValue.serverTimestamp()});
    return {attempt: attemptResponse({...attempt, status: "VERIFICATION_PENDING"})};
  }
  return {attempt: await markPaid(attempt, paymentId, callbackId)};
});

export const customerRecordRazorpayFailure = onCall(options, async (request) => {
  const context = await requireCustomer(request); const attempt = await ownedAttempt(context, validId(request.data?.attemptId));
  if (attempt.status === "PAID") return {attempt: attemptResponse(attempt)};
  const status = request.data?.cancelled === true ? "CANCELLED" : "FAILED";
  const failureMessage = text(request.data?.description, "Payment was not completed").slice(0, 500);
  await attempt.ref.update({status, paymentStatus: "FAILED", failureMessage,
    callbackId: validId(request.data?.callbackId), failureCode: Number(request.data?.code) || 0,
    retryAllowed: true, revision: FieldValue.increment(1), updatedAt: FieldValue.serverTimestamp()});
  return {attempt: attemptResponse({...attempt, status, paymentStatus: "FAILED", failureMessage,
    retryAllowed: true, revision: Number(attempt.revision ?? 0) + 1})};
});

export const customerReconcilePendingRazorpayPayment = onCall(options, async (request) => {
  const context = await requireCustomer(request); const query = await attempts(context)
    .where("customerId", "==", context.uid)
    .where("status", "in", ["CREATED", "SDK_OPEN", "VERIFICATION_PENDING"]).limit(10).get();
  const latest = query.docs.sort((a, b) => Number(b.get("createdAtEpochMillis")) - Number(a.get("createdAtEpochMillis")))[0];
  if (!latest) return {attempt: null};
  const attempt: any = {id: latest.id, ref: latest.ref, ...latest.data()};
  const payments = await razorpayGet(`/orders/${encodeURIComponent(text(attempt.razorpayOrderId))}/payments`);
  const captured = Array.isArray(payments.items) ? payments.items.find((item: any) => item?.status === "captured") : null;
  return {attempt: captured ? await markPaid(attempt, validId(captured.id), "reconcile") : attemptResponse(attempt)};
});

async function createSession(context: Context, appOrderId: string, idempotencyKey: string) {
  const firestore = getFirestore(); const orderRef = firestore.collection("shops").doc(context.shopId).collection("orders").doc(appOrderId);
  const requestRef = attempts(context).doc(`request_${context.uid}_${idempotencyKey}`); const [order, previous] = await Promise.all([
    orderRef.get(), requestRef.get(),
  ]);
  if (previous.exists) return sessionResponse({id: previous.id, ...previous.data()});
  if (!order.exists || order.get("customerId") !== context.uid || order.get("paymentMethod") !== "RAZORPAY") {
    throw new HttpsError("not-found", "Razorpay order is unavailable");
  }
  if (order.get("paymentStatus") === "PAID") throw failure("This order is already paid", "ALREADY_PAID");
  await verifyEnabled(context.shopId);
  const amountMinor = Number(order.get("totalMinor")); const currencyCode = text(order.get("currencyCode"), "INR");
  const remote = await razorpayPost("/orders", {amount: amountMinor, currency: currencyCode,
    receipt: appOrderId.slice(0, 40), notes: {appOrderId, shopId: context.shopId, customerId: context.uid}});
  const value = {attemptId: requestRef.id, appOrderId, razorpayOrderId: validId(remote.id), publicKeyId: keyId.value(),
    customerId: context.uid, shopId: context.shopId, amountMinor, currencyCode,
    merchantName: "MeatBush", description: text(order.get("orderNumber"), "MeatBush order"),
    customerName: text(order.get("customerName")) || null, customerMobile: text(order.get("customerMobile")) || null,
    customerEmail: null, status: "CREATED", paymentStatus: "PROCESSING", retryAllowed: false, revision: 0,
    createdAtEpochMillis: Date.now(), createdAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp()};
  await requestRef.create(value); await orderRef.update({paymentStatus: "PROCESSING", razorpayAttemptId: requestRef.id,
    razorpayOrderId: value.razorpayOrderId, updatedAt: FieldValue.serverTimestamp()});
  return sessionResponse({id: requestRef.id, ...value});
}

async function verifyEnabled(shopId: string) {
  const firestore = getFirestore(); const [payment, features] = await Promise.all([
    firestore.collection("shops").doc(shopId).collection("config").doc("payment").get(),
    firestore.collection("appConfig").doc("features").get(),
  ]);
  if (payment.get("razorpayEnabled") !== true || payment.get("razorpayConfigured") !== true ||
      features.get("razorpayAllowed") !== true) throw failure("Razorpay is disabled", "RAZORPAY_DISABLED");
}

async function ownedAttempt(context: Context, attemptId: string): Promise<any> {
  const snapshot = await attempts(context).doc(attemptId).get();
  if (!snapshot.exists || snapshot.get("customerId") !== context.uid) throw new HttpsError("not-found", "Payment attempt not found");
  return {id: snapshot.id, ref: snapshot.ref, ...snapshot.data()};
}
function attempts(context: Context) { return getFirestore().collection("shops").doc(context.shopId).collection("paymentAttempts"); }
async function markPaid(attempt: any, paymentId: string, callbackId: string) {
  const attemptRef = getFirestore().collection("shops").doc(text(attempt.shopId))
    .collection("paymentAttempts").doc(text(attempt.id));
  await getFirestore().runTransaction(async (transaction) => {
    const fresh = await transaction.get(attemptRef); if (fresh.get("status") === "PAID") return;
    const orderRef = getFirestore().collection("shops").doc(text(fresh.get("shopId"))).collection("orders").doc(text(fresh.get("appOrderId")));
    transaction.update(attemptRef, {status: "PAID", paymentStatus: "PAID", razorpayPaymentId: paymentId,
      callbackId, retryAllowed: false, paidAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp(),
      revision: FieldValue.increment(1)});
    transaction.update(orderRef, {paymentStatus: "PAID", razorpayPaymentId: paymentId,
      paymentPaidAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp()});
  });
  return attemptResponse({...attempt, status: "PAID", paymentStatus: "PAID", retryAllowed: false,
    revision: Number(attempt.revision ?? 0) + 1});
}
function sessionResponse(value: any) { return {attemptId: text(value.attemptId, value.id), appOrderId: text(value.appOrderId),
  razorpayOrderId: text(value.razorpayOrderId), publicKeyId: text(value.publicKeyId, keyId.value()),
  amountMinor: Number(value.amountMinor), currencyCode: text(value.currencyCode, "INR"),
  merchantName: text(value.merchantName, "MeatBush"), description: text(value.description, "MeatBush order"),
  customerName: value.customerName ?? null, customerMobile: value.customerMobile ?? null, customerEmail: value.customerEmail ?? null}; }
function attemptResponse(value: any) { return {attemptId: text(value.attemptId, value.id), appOrderId: text(value.appOrderId),
  status: text(value.status, "VERIFICATION_PENDING"), paymentStatus: text(value.paymentStatus, "PROCESSING"),
  failureMessage: value.failureMessage ?? null, retryAllowed: value.retryAllowed === true, revision: Number(value.revision ?? 0)}; }
function validSignature(orderId: string, paymentId: string, signature: string) {
  return isRazorpaySignatureValid(orderId, paymentId, signature, keySecret.value());
}
export function isRazorpaySignatureValid(orderId: string, paymentId: string, signature: string, secret: string) {
  const expected = createHmac("sha256", secret).update(`${orderId}|${paymentId}`).digest("hex");
  const left = Buffer.from(expected); const right = Buffer.from(signature); return left.length === right.length && timingSafeEqual(left, right);
}
async function razorpayGet(path: string) { return razorpay(path, {method: "GET"}); }
async function razorpayPost(path: string, body: unknown) { return razorpay(path, {method: "POST", body: JSON.stringify(body)}); }
async function razorpay(path: string, init: RequestInit) {
  const response = await fetch(`https://api.razorpay.com/v1${path}`, {...init, headers: {Authorization:
    `Basic ${Buffer.from(`${keyId.value()}:${keySecret.value()}`).toString("base64")}`, "Content-Type": "application/json"}});
  const data = await response.json() as any; if (!response.ok) throw new HttpsError("unavailable",
    text(data?.error?.description, "Payment service is unavailable"), {reason: "RAZORPAY_API_ERROR"}); return data;
}
function validId(value: unknown) { const result = text(value); if (!result || result.length > 160 || result.includes("/")) {
  throw new HttpsError("invalid-argument", "Invalid payment request"); } return result; }
function failure(message: string, reason: string) { return new HttpsError("failed-precondition", message, {reason}); }
