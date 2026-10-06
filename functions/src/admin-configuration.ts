import {FieldValue} from "firebase-admin/firestore";
import {getFirestore} from "./database.js";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {catalogOptions, integer, requireAdmin, text} from "./catalog-common.js";

const featuresRef = () => getFirestore().collection("appConfig").doc("features");
const paymentRef = (shopId: string) => getFirestore().collection("shops").doc(shopId)
  .collection("config").doc("payment");
const deliveryRef = (shopId: string) => getFirestore().collection("shops").doc(shopId)
  .collection("config").doc("delivery");

export const adminGetPaymentConfig = onCall(catalogOptions, async (request) => {
  const context = await requireAdmin(request);
  const [config, features] = await Promise.all([paymentRef(context.shopId).get(), featuresRef().get()]);
  return {config: paymentResponse(config.data() ?? {}, features.data() ?? {})};
});

export const adminUpdatePaymentConfig = onCall(catalogOptions, async (request) => {
  const context = await requireAdmin(request); const expected = integer(request.data?.expectedRevision, -1);
  if (expected < 0) invalid("Payment revision required");
  const firestore = getFirestore();
  const result = await firestore.runTransaction(async (transaction) => {
    const [current, features] = await Promise.all([
      transaction.get(paymentRef(context.shopId)), transaction.get(featuresRef()),
    ]);
    const revision = integer(current.get("revision"));
    if (revision !== expected) throw new HttpsError("aborted", "Payment settings changed");
    const razorpay = request.data?.razorpayEnabled === true;
    const upi = request.data?.upiEnabled === true;
    if ((razorpay && features.get("razorpayAllowed") !== true) ||
        (upi && features.get("upiAllowed") !== true)) deniedFeature();
    if (razorpay && current.get("razorpayConfigured") !== true) {
      throw new HttpsError("failed-precondition", "Razorpay backend is not configured");
    }
    const update = {
      codEnabled: true,
      razorpayEnabled: razorpay && current.get("razorpayConfigured") === true,
      upiEnabled: upi && features.get("upiAllowed") === true,
      revision: revision + 1, updatedAt: FieldValue.serverTimestamp(), updatedBy: context.uid,
    };
    transaction.set(paymentRef(context.shopId), update, {merge: true});
    return paymentResponse({...current.data(), ...update}, features.data() ?? {});
  });
  return {config: result};
});

export const adminGetDeliveryConfig = onCall(catalogOptions, async (request) => {
  const context = await requireAdmin(request);
  const [config, features] = await Promise.all([deliveryRef(context.shopId).get(), featuresRef().get()]);
  return {config: deliveryResponse(config.data() ?? {}, features.data() ?? {})};
});

export const adminUpdateDeliveryConfig = onCall(catalogOptions, async (request) => {
  const context = await requireAdmin(request); const expected = integer(request.data?.expectedRevision, -1);
  if (expected < 0) invalid("Delivery revision required");
  const amounts = {
    deliveryChargeMinor: money(request.data?.deliveryChargeMinor, "delivery charge"),
    freeDeliveryThresholdMinor: request.data?.freeDeliveryThresholdMinor == null ? null :
      money(request.data.freeDeliveryThresholdMinor, "free delivery threshold"),
    minimumOrderMinor: money(request.data?.minimumOrderMinor, "minimum order"),
  };
  const slots = validatedSlots(request.data?.slots);
  const firestore = getFirestore();
  const result = await firestore.runTransaction(async (transaction) => {
    const [current, features] = await Promise.all([
      transaction.get(deliveryRef(context.shopId)), transaction.get(featuresRef()),
    ]);
    const revision = integer(current.get("revision"));
    if (revision !== expected) throw new HttpsError("aborted", "Delivery settings changed");
    const tracking = request.data?.realtimeTrackingEnabled === true;
    if (tracking && features.get("realtimeTrackingAllowed") !== true) deniedFeature();
    const update = {
      normalDeliveryEnabled: false,
      ...amounts, slots,
      scheduledDeliveryEnabled: true,
      realtimeTrackingEnabled: tracking && features.get("realtimeTrackingAllowed") === true,
      currencyCode: "INR", revision: revision + 1,
      updatedAt: FieldValue.serverTimestamp(), updatedBy: context.uid,
    };
    transaction.set(deliveryRef(context.shopId), update, {merge: true});
    return deliveryResponse(update, features.data() ?? {});
  });
  return {config: result};
});

function paymentResponse(value: FirebaseFirestore.DocumentData, features: FirebaseFirestore.DocumentData) {
  const configured = value.razorpayConfigured === true;
  return {
    codEnabled: true,
    razorpayEnabled: value.razorpayEnabled === true && configured && features.razorpayAllowed === true,
    razorpayConfigured: configured,
    upiEnabled: value.upiEnabled === true && features.upiAllowed === true,
    upiVpa: typeof value.upiVpa === "string" ? value.upiVpa : null,
    revision: integer(value.revision),
  };
}

function deliveryResponse(value: FirebaseFirestore.DocumentData, features: FirebaseFirestore.DocumentData) {
  return {
    normalDeliveryEnabled: false,
    deliveryChargeMinor: integer(value.deliveryChargeMinor),
    freeDeliveryThresholdMinor: value.freeDeliveryThresholdMinor == null ? null : integer(value.freeDeliveryThresholdMinor),
    minimumOrderMinor: integer(value.minimumOrderMinor),
    slots: Array.isArray(value.slots) ? value.slots : [],
    scheduledDeliveryEnabled: true,
    realtimeTrackingEnabled: value.realtimeTrackingEnabled === true && features.realtimeTrackingAllowed === true,
    currencyCode: text(value.currencyCode, "INR"), revision: integer(value.revision),
  };
}

function validatedSlots(raw: unknown) {
  if (!Array.isArray(raw) || raw.length > 30) invalid("Invalid delivery slots");
  return raw.map((item: any) => {
    const id = text(item?.id); const label = text(item?.label).trim().slice(0, 60);
    const startMinutes = integer(item?.startMinutes, -1); const endMinutes = integer(item?.endMinutes, -1);
    if (!id || !label || startMinutes < 0 || startMinutes > 1439 || endMinutes < 1 ||
        endMinutes > 1440 || startMinutes >= endMinutes) invalid("Invalid delivery slot");
    return {id, label, startMinutes, endMinutes, active: item?.active !== false};
  });
}

function money(value: unknown, label: string): number {
  const parsed = Number(value);
  if (!Number.isSafeInteger(parsed) || parsed < 0 || parsed > 100_000_000) invalid(`Invalid ${label}`);
  return parsed;
}
function invalid(message: string): never { throw new HttpsError("invalid-argument", message); }
function deniedFeature(): never { throw new HttpsError("failed-precondition", "Feature is disabled by Super Admin"); }
