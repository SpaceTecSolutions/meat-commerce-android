import {randomUUID} from "node:crypto";
import {getAuth} from "firebase-admin/auth";
import {FieldValue} from "firebase-admin/firestore";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {getFirestore} from "./database.js";
import {text} from "./catalog-common.js";

const options = {region: "asia-south1", enforceAppCheck: true, invoker: "public" as const,
  timeoutSeconds: 540, memory: "512MiB" as const};
const activeStatuses = new Set(["PENDING", "CONFIRMED", "PREPARING", "OUT_FOR_DELIVERY"]);

/** Resumable, authenticated deletion. The marker survives partial failures until Auth is removed. */
export const customerDeleteAccount = onCall(options, async (request) => {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Sign in to delete your account");
  const db = getFirestore();
  const profileRef = db.collection("users").doc(uid);
  const markerRef = db.collection("accountDeletions").doc(uid);
  const marker = await db.runTransaction(async (transaction) => {
    const [profile, existing] = await Promise.all([
      transaction.get(profileRef), transaction.get(markerRef),
    ]);
    if (existing.exists) return existing.data()!;
    if (!profile.exists || profile.get("role") !== "CUSTOMER" || profile.get("active") !== true) {
      throw new HttpsError("permission-denied", "Active customer account required");
    }
    const shopId = text(profile.get("shopId"), "default");
    // Reading the customer query inside the transaction conflicts with concurrent checkout writes.
    const orders = await transaction.get(db.collection("shops").doc(shopId).collection("orders")
      .where("customerId", "==", uid));
    if (orders.docs.some((order) => activeStatuses.has(text(order.get("orderStatus"))))) {
      throw new HttpsError("failed-precondition", "Complete or cancel your active order first",
        {reason: "ACTIVE_ORDER"});
    }
    const value = {shopId, mobile: text(profile.get("mobileNumber")),
      joinedAt: profile.get("createdAt") ?? null,
      anonymousCustomerKey: randomUUID(), createdAt: FieldValue.serverTimestamp()};
    transaction.create(markerRef, value);
    transaction.update(profileRef, {deletionPending: true,
      updatedAt: FieldValue.serverTimestamp()});
    return value;
  });
  const shopId = text(marker.shopId); const anonymousCustomerKey = text(marker.anonymousCustomerKey);
  if (!shopId || !anonymousCustomerKey) throw new HttpsError("internal", "Deletion state unavailable");
  const shop = db.collection("shops").doc(shopId);
  // Remove identity and delivery details, retaining only order/line-item/payment business facts.
  await drainQuery(shop.collection("orders").where("customerId", "==", uid), (batch, doc) => {
    batch.update(doc.ref, {customerId: FieldValue.delete(), anonymousCustomerKey,
      customerName: FieldValue.delete(), customerPhone: FieldValue.delete(),
      customerMobile: FieldValue.delete(), deliveryAddressSnapshot: FieldValue.delete(),
      addressSummary: FieldValue.delete(), addressLabel: FieldValue.delete(),
      deliveryInstructions: FieldValue.delete(), instructions: FieldValue.delete(),
      customerNote: FieldValue.delete(), deliveryContactName: FieldValue.delete(),
      deliveryContactMobile: FieldValue.delete(), deliveryDestinationLatitude: FieldValue.delete(),
      deliveryDestinationLongitude: FieldValue.delete(), cancelReason: FieldValue.delete(),
      razorpayAttemptId: FieldValue.delete(), updatedAt: FieldValue.serverTimestamp()});
  });
  await drainQuery(shop.collection("paymentAttempts").where("customerId", "==", uid), (batch, doc) => {
    batch.delete(doc.ref);
  });
  await drainQuery(shop.collection("auditLogs").where("targetCustomerId", "==", uid),
    (batch, doc) => batch.update(doc.ref,
      {targetCustomerId: FieldValue.delete(), anonymousCustomerKey}));
  await drainQuery(db.collection("authCredentials").where("uid", "==", uid),
    (batch, doc) => batch.delete(doc.ref));
  await drainQuery(db.collection("notificationDeviceOwners").where("uid", "==", uid),
    (batch, doc) => batch.delete(doc.ref));
  await drainQuery(db.collection("authFlowTokens").where("uid", "==", uid),
    (batch, doc) => batch.delete(doc.ref));
  const mobile = text(marker.mobile);
  if (mobile) await drainQuery(db.collection("authFlowTokens").where("mobile", "==", mobile),
    (batch, doc) => batch.delete(doc.ref));
  await db.collection("customerAccountFacts").doc(anonymousCustomerKey).set({
    shopId, joinedAt: marker.joinedAt ?? null, deletedAt: FieldValue.serverTimestamp(),
  }, {merge: true});
  // recursiveDelete covers addresses, cartItems, checkoutQuotes, orderRequests, notifications and devices.
  await db.recursiveDelete(profileRef);
  try {
    await getAuth().deleteUser(uid);
  } catch (error) {
    if ((error as {code?: string}).code !== "auth/user-not-found") throw error;
  }
  await markerRef.delete().catch(() => undefined);
  return {success: true};
});

async function drainQuery(query: FirebaseFirestore.Query, apply: (batch: FirebaseFirestore.WriteBatch,
  doc: FirebaseFirestore.QueryDocumentSnapshot) => void) {
  const db = getFirestore();
  while (true) {
    const page = await query.limit(200).get();
    if (page.empty) return;
    const batch = db.batch();
    page.docs.forEach((doc) => apply(batch, doc));
    await batch.commit();
  }
}
