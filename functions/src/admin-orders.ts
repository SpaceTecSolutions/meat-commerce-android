import {FieldValue, Timestamp} from "firebase-admin/firestore";
import {randomUUID} from "node:crypto";
import {getFirestore} from "./database.js";
import {CallableRequest, HttpsError, onCall} from "firebase-functions/v2/https";
import {deliveryOtpRef, requireVerifiedOtpForCompletion} from "./delivery-otp.js";

const options = {region: "asia-south1", enforceAppCheck: true, timeoutSeconds: 30, memory: "256MiB" as const};
const defaultShopId = "default";
const allowedCancellationStates = new Set(["PENDING", "CONFIRMED", "PREPARING", "OUT_FOR_DELIVERY"]);

type AdminContext = {uid: string; shopId: string};
type Mutation = (value: FirebaseFirestore.DocumentData, context: AdminContext) => Record<string, unknown>;

export const adminGetOrders = onCall(options, async (request) => {
  const context = await requireOperator(request, "VIEW_ORDERS");
  const snapshot = await orders(context.shopId).limit(250).get();
  const records = snapshot.docs.map((doc) => ({id: doc.id, value: doc.data()}));
  const enriched = await enrichOrderCategories(context.shopId, records.map((record) => record.value));
  const values = records.map((record, index) => orderResponse(record.id, enriched[index]))
    .sort((a, b) => Number(b.createdAtEpochMillis) - Number(a.createdAtEpochMillis));
  return {orders: values};
});

async function enrichOrderCategories(shopId: string, values: FirebaseFirestore.DocumentData[]) {
  const missingIds = [...new Set(values.flatMap((value) => rawItems(value))
    .filter((item) => !text(item.categoryId) || !text(item.categoryName))
    .map((item) => text(item.productId)).filter(Boolean))];
  if (!missingIds.length) return values;
  const firestore = getFirestore();
  const snapshots = await firestore.getAll(...missingIds.slice(0, 500)
    .map((id) => firestore.collection("shops").doc(shopId).collection("products").doc(id)));
  const products = new Map(snapshots.filter((item) => item.exists).map((item) => [item.id, item.data()!]));
  return applyCategoryFallback(values, products);
}

export function applyCategoryFallback(
  values: FirebaseFirestore.DocumentData[],
  products: Map<string, FirebaseFirestore.DocumentData>,
) {
  return values.map((value) => ({...value, items: rawItems(value).map((item) => {
    const product = products.get(text(item.productId));
    if (!product) return item;
    return {...item,
      categoryId: text(item.categoryId, text(product.categoryId)) || null,
      categoryName: text(item.categoryName, text(product.categoryName)) || null};
  })}));
}

export const adminConfirmCodOrder = mutate(async (value, context, request) => {
  requireState(value, "PENDING");
  const method = text(value.paymentMethod, "COD");
  const payment = text(value.paymentStatus, "PENDING");
  if ((method === "COD" && payment !== "PENDING") || (method !== "COD" && payment !== "PAID")) invalidTransition();
  const requested = Array.isArray(request.data?.adjustments) ? request.data.adjustments : [];
  if (method !== "COD" && requested.length) {
    throw new HttpsError("failed-precondition", "Paid online orders cannot be repriced");
  }
  const byProduct = new Map<string, number>();
  requested.forEach((raw: any) => {
    const id = text(raw?.productId); const quantity = Number(raw?.finalQuantity);
    if (!id || !Number.isFinite(quantity) || quantity <= 0 || quantity > 1000 || byProduct.has(id)) {
      throw new HttpsError("invalid-argument", "Invalid final quantity");
    }
    byProduct.set(id, Math.round(quantity * 1000) / 1000);
  });
  const items = rawItems(value).map((item) => {
    const unit = text(item.unit, "PIECE");
    const estimated = number(item.quantity, 1);
    const finalQuantity = byProduct.get(text(item.productId)) ?? estimated;
    if (byProduct.has(text(item.productId)) && unit !== "KILOGRAM" && unit !== "GRAM") {
      throw new HttpsError("invalid-argument", "Only weight-based items can be adjusted");
    }
    const unitPrice = number(item.unitPriceMinor);
    const finalLine = Math.max(0, Math.round(unitPrice * finalQuantity));
    return {...item, estimatedQuantity: estimated, finalQuantity,
      estimatedLineTotalMinor: number(item.lineTotalMinor), finalLineTotalMinor: finalLine,
      lineTotalMinor: finalLine};
  });
  if ([...byProduct.keys()].some((id) => !items.some((item) => text((item as FirebaseFirestore.DocumentData).productId) === id))) {
    throw new HttpsError("invalid-argument", "Adjusted item is not in this order");
  }
  const subtotalMinor = items.reduce((sum, item) => sum + number(item.lineTotalMinor), 0);
  const totalMinor = Math.max(0, subtotalMinor - number(value.discountMinor) +
    number(value.deliveryFeeMinor) + number(value.taxMinor));
  return {items, subtotalMinor, totalMinor, amountDueMinor: totalMinor,
    estimatedTotalMinor: number(value.totalMinor), finalTotalMinor: totalMinor,
    finalBillAdjusted: byProduct.size > 0, billFinalizedAtEpochMillis: Date.now(),
    billFinalizedByAdminId: context.uid, confirmedAtEpochMillis: Date.now(),
    confirmedByAdminId: context.uid, orderStatus: "CONFIRMED"};
}, "ORDER_FINAL_BILL_CONFIRMED", "ADJUST_FINAL_BILL");

export const adminStartPreparingOrder = transition("PREPARING", (value, context) => {
  requireState(value, "CONFIRMED");
  return {preparingAtEpochMillis: Date.now(), preparingByAdminId: context.uid};
});

export const adminMarkOrderReadyForDelivery = mutate(async (value, context) => {
  requireState(value, "PREPARING");
  if (number(value.readyForDeliveryAtEpochMillis) > 0) invalidTransition();
  return {readyForDeliveryAtEpochMillis: Date.now(), readyForDeliveryByAdminId: context.uid};
}, "ORDER_READY_FOR_DELIVERY");

export const adminCancelCodOrder = mutate(async (value, context, request, transaction) => {
  if (!allowedCancellationStates.has(text(value.orderStatus))) invalidTransition();
  const reason = text(request.data?.reason).trim();
  if (!reason || reason.length > 200) throw new HttpsError("invalid-argument", "Cancellation reason required");
  const paymentStatus = value.paymentMethod !== "COD" && value.paymentStatus === "PAID" ? "REFUND_PENDING" : value.paymentStatus;
  transaction.delete(deliveryOtpRef(context.shopId, text(request.data?.orderId)));
  return {orderStatus: "CANCELLED", paymentStatus, cancelReason: reason, cancelledAtEpochMillis: Date.now(),
    cancelledByUserId: context.uid, cancelledByRole: "ADMIN", assignedDeliveryUserId: null,
    assignedDeliveryUserName: null, assignedDeliveryRole: null, adminDeliveringPersonally: false,
    trackingLifecycle: "STOPPED"};
}, "ORDER_CANCELLED");

export const adminAssignOrderToSelf = mutate(async (value, context) => {
  requireReadyUnassigned(value);
  return {adminDeliveringPersonally: true, assignedDeliveryRole: "ADMIN", assignedAtEpochMillis: Date.now(),
    assignedByAdminId: context.uid, assignedDeliveryUserName: "Admin"};
}, "ORDER_ASSIGNED_ADMIN");

export const adminAssignDeliveryUser = mutate(async (value, context, request, transaction) => {
  requireReadyUnassigned(value);
  const deliveryUserId = text(request.data?.deliveryUserId);
  if (!deliveryUserId) throw new HttpsError("invalid-argument", "Delivery user required");
  const user = await transaction.get(getFirestore().collection("users").doc(deliveryUserId));
  if (!user.exists || user.get("role") !== "DELIVERY" || user.get("active") !== true ||
      text(user.get("shopId"), defaultShopId) !== context.shopId) {
    throw new HttpsError("failed-precondition", "Delivery user is inactive", {reason: "DELIVERY_USER_INACTIVE"});
  }
  return {assignedDeliveryUserId: deliveryUserId, assignedDeliveryUserName: text(user.get("displayName"), "Delivery User"),
    assignedDeliveryRole: "DELIVERY", assignedAtEpochMillis: Date.now(), assignedByAdminId: context.uid,
    adminDeliveringPersonally: false};
}, "ORDER_ASSIGNED_DELIVERY");

export const adminStartAssignedDelivery = mutate(async (value, context, _request, transaction) => {
  requireState(value, "PREPARING");
  if (number(value.readyForDeliveryAtEpochMillis) <= 0 || value.adminDeliveringPersonally !== true) invalidTransition();
  return {orderStatus: "OUT_FOR_DELIVERY", deliveryStartedAtEpochMillis: Date.now(),
    deliveryStartedByUserId: context.uid, outForDeliveryAtEpochMillis: Date.now(),
    ...await trackingPreparation(context.shopId, transaction)};
}, "ORDER_OUT_FOR_DELIVERY");

export const adminCompleteDelivery = mutate(async (value, context, request, transaction) => {
  requireState(value, "OUT_FOR_DELIVERY");
  if (value.adminDeliveringPersonally !== true || value.deliveryStartedByUserId !== context.uid) forbidden();
  const method = text(request.data?.paymentMethod);
  if (method !== text(value.paymentMethod)) invalidTransition();
  const orderId = text(request.data?.orderId);
  await requireVerifiedOtpForCompletion(transaction, context.shopId, orderId, value, context.uid);
  let paymentStatus = text(value.paymentStatus);
  const update: Record<string, unknown> = {};
  if (method === "COD") {
    const collected = number(request.data?.collectedMinor, -1);
    const due = number(value.amountDueMinor);
    if (paymentStatus !== "PENDING" || collected < due) {
      throw new HttpsError("failed-precondition", "Collected amount is below COD due", {reason: "COD_AMOUNT_MISMATCH"});
    }
    paymentStatus = "COLLECTED";
    Object.assign(update, {collectedMinor: collected, changeReturnedMinor: collected - due,
      collectedAtEpochMillis: Date.now(), collectedByUserId: context.uid});
  } else if (paymentStatus !== "PAID") invalidTransition();
  transaction.delete(deliveryOtpRef(context.shopId, orderId));
  return {...update, orderStatus: "DELIVERED", paymentStatus, deliveredAtEpochMillis: Date.now(),
    deliveredAt: FieldValue.serverTimestamp(), deliveredByUserId: context.uid,
    trackingLifecycle: "STOPPED"};
}, "ORDER_DELIVERED");

export const adminReportCodMismatch = mutate(async (value, context, request) => {
  requireState(value, "OUT_FOR_DELIVERY");
  if (value.adminDeliveringPersonally !== true || value.paymentMethod !== "COD") forbidden();
  const actual = number(request.data?.actualMinor, -1);
  if (actual < 0 || actual === number(value.amountDueMinor)) throw new HttpsError("invalid-argument", "Invalid amount");
  return {codMismatchReported: true, codMismatchActualMinor: actual, codMismatchReportedAtEpochMillis: Date.now(),
    codMismatchReportedBy: context.uid};
}, "COD_AMOUNT_MISMATCH_REPORTED");

function transition(target: string, validate: Mutation) {
  return mutate(async (value, context) => ({...validate(value, context), orderStatus: target}), `ORDER_${target}`);
}

function mutate(build: (value: FirebaseFirestore.DocumentData, context: AdminContext,
  request: CallableRequest, transaction: FirebaseFirestore.Transaction) => Promise<Record<string, unknown>>, action: string,
permission = "UPDATE_ORDER_STATUS") {
  return onCall(options, async (request) => {
    const context = await requireOperator(request, permission);
    const orderId = text(request.data?.orderId);
    const expectedRevision = number(request.data?.expectedRevision, -1);
    if (!orderId || expectedRevision < 0) throw new HttpsError("invalid-argument", "Order and revision required");
    const reference = orders(context.shopId).doc(orderId);
    const result = await getFirestore().runTransaction(async (transaction) => {
      const snapshot = await transaction.get(reference);
      if (!snapshot.exists) throw new HttpsError("not-found", "Order not found");
      const value = snapshot.data()!;
      if (number(value.revision) !== expectedRevision) invalidTransition();
      const update = await build(value, context, request, transaction);
      const revision = expectedRevision + 1;
      transaction.update(reference, {...update, revision, updatedAt: FieldValue.serverTimestamp(), updatedBy: context.uid});
      transaction.create(getFirestore().collection("shops").doc(context.shopId).collection("auditLogs").doc(),
        {action, actorUid: context.uid, targetOrderId: orderId, createdAt: FieldValue.serverTimestamp()});
      return orderResponse(orderId, {...value, ...update, revision});
    });
    return {order: result};
  });
}

async function requireAdmin(request: CallableRequest): Promise<AdminContext> {
  if (!request.auth) throw new HttpsError("unauthenticated", "Authentication required");
  const reference = getFirestore().collection("users").doc(request.auth.uid);
  const profile = await reference.get();
  if (!profile.exists || profile.get("active") !== true || profile.get("role") !== "ADMIN") forbidden();
  const shopId = text(profile.get("shopId"), defaultShopId);
  if (!profile.get("shopId")) await reference.update({shopId, updatedAt: FieldValue.serverTimestamp()});
  return {uid: request.auth.uid, shopId};
}

async function requireOperator(request: CallableRequest, permission: string): Promise<AdminContext> {
  if (!request.auth) throw new HttpsError("unauthenticated", "Authentication required");
  const reference = getFirestore().collection("users").doc(request.auth.uid); const profile = await reference.get();
  const role = profile.get("role"); const permissions = Array.isArray(profile.get("permissions")) ? profile.get("permissions") : [];
  if (!profile.exists || profile.get("active") !== true ||
      (role !== "ADMIN" && !(role === "STAFF" && permissions.includes(permission)))) forbidden();
  return {uid: request.auth.uid, shopId: text(profile.get("shopId"), defaultShopId)};
}

function orders(shopId: string) { return getFirestore().collection("shops").doc(shopId).collection("orders"); }
function rawItems(value: FirebaseFirestore.DocumentData): FirebaseFirestore.DocumentData[] {
  return Array.isArray(value.items) ? value.items.filter((item): item is FirebaseFirestore.DocumentData =>
    item != null && typeof item === "object") : [];
}
function requireState(value: FirebaseFirestore.DocumentData, expected: string) {
  if (value.orderStatus !== expected) invalidTransition();
}
function requireReadyUnassigned(value: FirebaseFirestore.DocumentData) {
  requireState(value, "PREPARING");
  if (number(value.readyForDeliveryAtEpochMillis) <= 0 || value.adminDeliveringPersonally === true || value.assignedDeliveryUserId) {
    throw new HttpsError("failed-precondition", "Order already assigned or not ready", {reason: "ALREADY_ASSIGNED"});
  }
}

export function orderResponse(id: string, value: FirebaseFirestore.DocumentData) {
  const address = value.deliveryAddressSnapshot ?? {};
  return {id, displayNumber: text(value.displayNumber ?? value.orderNumber, id),
    customerName: text(value.customerName, "Customer"), customerMobile: text(value.customerMobile ?? value.customerPhone),
    addressSummary: text(value.addressSummary ?? address.formattedAddress ?? address.address),
    addressLabel: text(value.addressLabel ?? address.type, "Home"), amountDueMinor: number(value.amountDueMinor ?? value.totalAmount ?? value.totalMinor),
    items: Array.isArray(value.items) ? value.items : [], subtotalMinor: number(value.subtotalMinor ?? value.itemTotal),
    discountMinor: number(value.discountMinor ?? value.discountAmount), deliveryFeeMinor: number(value.deliveryFeeMinor ?? value.deliveryCharge),
    taxMinor: number(value.taxMinor ?? value.taxAmount), totalMinor: number(value.totalMinor ?? value.totalAmount),
    paymentMethod: text(value.paymentMethod, "COD"), paymentStatus: text(value.paymentStatus, "PENDING"),
    orderStatus: text(value.orderStatus, "PENDING"), instructions: text(value.instructions ?? value.customerNote),
    currencyCode: text(value.currencyCode, "INR"), createdAtEpochMillis: epoch(value.createdAtEpochMillis ?? value.createdAt),
    deliverySlotDateLabel: value.deliverySlotDateLabel ?? value.deliverySlotSnapshot?.dateLabel ?? null,
    deliverySlotTimeLabel: value.deliverySlotTimeLabel ?? value.deliverySlotSnapshot?.timeLabel ?? null,
    deliveryDateIso: value.deliveryDate ?? value.deliverySlotSnapshot?.date ?? null,
    deliverySlotStartMinutes: value.deliverySlotSnapshot?.startMinutes ?? null,
    deliverySlotEndMinutes: value.deliverySlotSnapshot?.endMinutes ?? null,
    revision: number(value.revision), customerCancellationAllowed: value.customerCancellationAllowed === true,
    confirmedAtEpochMillis: epoch(value.confirmedAtEpochMillis), confirmedByAdminId: value.confirmedByAdminId ?? null,
    preparingAtEpochMillis: epoch(value.preparingAtEpochMillis), preparingByAdminId: value.preparingByAdminId ?? null,
    readyForDeliveryAtEpochMillis: epoch(value.readyForDeliveryAtEpochMillis), readyForDeliveryByAdminId: value.readyForDeliveryByAdminId ?? null,
    assignedDeliveryUserId: value.assignedDeliveryUserId ?? null, assignedDeliveryUserName: value.assignedDeliveryUserName ?? null,
    assignedDeliveryRole: value.assignedDeliveryRole ?? null, assignedAtEpochMillis: epoch(value.assignedAtEpochMillis),
    assignedByAdminId: value.assignedByAdminId ?? null, deliveryStartedAtEpochMillis: epoch(value.deliveryStartedAtEpochMillis),
    deliveryStartedByUserId: value.deliveryStartedByUserId ?? null, adminDeliveringPersonally: value.adminDeliveringPersonally === true,
    deliveredAtEpochMillis: epoch(value.deliveredAtEpochMillis ?? value.deliveredAt), deliveredByUserId: value.deliveredByUserId ?? null,
    cancelledAtEpochMillis: epoch(value.cancelledAtEpochMillis ?? value.cancelledAt), cancelledByUserId: value.cancelledByUserId ?? null,
    cancelledByRole: value.cancelledByRole ?? null, cancelReason: value.cancelReason ?? null,
    codMismatchReported: value.codMismatchReported === true, trackingLifecycle: text(value.trackingLifecycle, "DISABLED"),
    trackingSessionId: value.trackingSessionId ?? null,
    deliveryDestinationLatitude: value.deliveryDestinationLatitude ?? address.latitude ?? null,
    deliveryDestinationLongitude: value.deliveryDestinationLongitude ?? address.longitude ?? null};
}

async function trackingPreparation(shopId: string, transaction: FirebaseFirestore.Transaction) {
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

function epoch(value: unknown): number | null { return value instanceof Timestamp ? value.toMillis() : number(value) || null; }
function text(value: unknown, fallback = ""): string { return typeof value === "string" ? value : fallback; }
function number(value: unknown, fallback = 0): number { const parsed = Number(value); return Number.isFinite(parsed) ? parsed : fallback; }
function invalidTransition(): never { throw new HttpsError("aborted", "Order changed", {reason: "INVALID_TRANSITION"}); }
function forbidden(): never { throw new HttpsError("permission-denied", "Active Admin access required"); }
