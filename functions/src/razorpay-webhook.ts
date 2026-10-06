import {createHmac, timingSafeEqual} from "node:crypto";
import {FieldValue} from "firebase-admin/firestore";
import {defineSecret} from "firebase-functions/params";
import {onRequest} from "firebase-functions/v2/https";
import {getFirestore} from "./database.js";

const webhookSecret = defineSecret("RAZORPAY_WEBHOOK_SECRET");

export const razorpayWebhook = onRequest({
  region: "asia-south1", timeoutSeconds: 30, memory: "256MiB", secrets: [webhookSecret],
}, async (request, response) => {
  if (request.method !== "POST") { response.status(405).send("Method not allowed"); return; }
  const signature = String(request.header("x-razorpay-signature") ?? "");
  const raw = request.rawBody;
  if (!raw?.length || !verify(raw, signature)) { response.status(401).send("Invalid signature"); return; }
  const eventId = String(request.header("x-razorpay-event-id") ?? "").slice(0, 180);
  if (!eventId) { response.status(400).send("Missing event id"); return; }
  const firestore = getFirestore(); const eventRef = firestore.collection("paymentWebhookEvents").doc(eventId);
  if ((await eventRef.get()).exists) { response.status(200).send("Already processed"); return; }
  const payload = request.body as any; const event = String(payload?.event ?? "");
  const payment = payload?.payload?.payment?.entity; const order = payload?.payload?.order?.entity;
  const razorpayOrderId = String(payment?.order_id ?? order?.id ?? "");
  if (!razorpayOrderId) {
    await eventRef.create({event, ignored: true, createdAt: FieldValue.serverTimestamp()});
    response.status(200).send("Ignored"); return;
  }
  const attempts = await firestore.collectionGroup("paymentAttempts")
    .where("razorpayOrderId", "==", razorpayOrderId).limit(1).get();
  const attempt = attempts.docs[0];
  if (!attempt) {
    await eventRef.create({event, razorpayOrderId, unmatched: true, createdAt: FieldValue.serverTimestamp()});
    response.status(200).send("Accepted"); return;
  }
  await firestore.runTransaction(async (transaction) => {
    if ((await transaction.get(eventRef)).exists) return;
    const fresh = await transaction.get(attempt.ref); const shopId = String(fresh.get("shopId") ?? "");
    const appOrderId = String(fresh.get("appOrderId") ?? "");
    const orderRef = firestore.collection("shops").doc(shopId).collection("orders").doc(appOrderId);
    if (event === "payment.captured" || event === "order.paid") {
      const paymentId = String(payment?.id ?? payload?.payload?.payment?.entity?.id ?? "");
      transaction.update(attempt.ref, {status: "PAID", paymentStatus: "PAID", razorpayPaymentId: paymentId,
        retryAllowed: false, paidAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp(),
        revision: FieldValue.increment(1)});
      transaction.update(orderRef, {paymentStatus: "PAID", razorpayPaymentId: paymentId,
        paymentPaidAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp()});
    } else if (event === "payment.failed" && fresh.get("status") !== "PAID") {
      transaction.update(attempt.ref, {status: "FAILED", paymentStatus: "FAILED", retryAllowed: true,
        failureMessage: String(payment?.error_description ?? "Payment failed").slice(0, 500),
        updatedAt: FieldValue.serverTimestamp(), revision: FieldValue.increment(1)});
      transaction.update(orderRef, {paymentStatus: "FAILED", updatedAt: FieldValue.serverTimestamp()});
    } else if (event === "refund.created" || event === "refund.processed") {
      const status = event === "refund.processed" ? "REFUNDED" : "REFUND_PENDING";
      transaction.update(attempt.ref, {status, paymentStatus: status,
        updatedAt: FieldValue.serverTimestamp(), revision: FieldValue.increment(1)});
      transaction.update(orderRef, {paymentStatus: status, updatedAt: FieldValue.serverTimestamp()});
    }
    transaction.create(eventRef, {event, razorpayOrderId, attemptId: attempt.id,
      createdAt: FieldValue.serverTimestamp()});
  });
  response.status(200).send("OK");
});

function verify(rawBody: Buffer, signature: string) {
  const expected = createHmac("sha256", webhookSecret.value()).update(rawBody).digest("hex");
  const left = Buffer.from(expected); const right = Buffer.from(signature);
  return left.length === right.length && timingSafeEqual(left, right);
}
