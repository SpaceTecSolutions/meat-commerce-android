import {getAuth} from "firebase-admin/auth";
import {FieldValue, Timestamp} from "firebase-admin/firestore";
import {getFirestore} from "./database.js";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {catalogOptions, integer, requireAdmin, shop, text} from "./catalog-common.js";

const customerOptions = {...catalogOptions, invoker: "public" as const};

export const adminGetCustomers = onCall(customerOptions, async (request) => {
  const context = await requireAdmin(request);
  const query = text(request.data?.query).trim().toLocaleLowerCase("en");
  const limit = Math.min(100, Math.max(1, integer(request.data?.limit, 50)));
  const [users, ordersSnapshot, shopSnapshot] = await Promise.all([
    getFirestore().collection("users").where("role", "==", "CUSTOMER").limit(500).get(),
    shop(context).collection("orders").limit(500).get(), shop(context).get(),
  ]);
  const stats = customerStats(ordersSnapshot.docs);
  const managementAllowed = shopSnapshot.get("customerStatusManagementAllowed") === true;
  const customers = users.docs
    .filter((doc) => text(doc.get("shopId"), "default") === context.shopId)
    .map((doc) => summary(doc.id, doc.data(), stats.get(doc.id), managementAllowed))
    .filter((value) => !query || `${value.displayName} ${value.mobileNumber}`.toLocaleLowerCase("en").includes(query))
    .sort((a, b) => a.displayName.localeCompare(b.displayName)).slice(0, limit);
  return {customers};
});

export const adminGetCustomerDetails = onCall(customerOptions, async (request) => {
  const context = await requireAdmin(request); const customerId = text(request.data?.customerId);
  if (!customerId) invalid("Customer is required");
  const [profile, ordersSnapshot, shopSnapshot] = await Promise.all([
    getFirestore().collection("users").doc(customerId).get(),
    shop(context).collection("orders").where("customerId", "==", customerId).get(), shop(context).get(),
  ]);
  requireCustomer(profile, context.shopId);
  const orders = ordersSnapshot.docs.filter((doc) => text(doc.get("customerId")) === customerId)
    .sort((a, b) => epoch(b.get("createdAtEpochMillis") ?? b.get("createdAt")) -
      epoch(a.get("createdAtEpochMillis") ?? a.get("createdAt")));
  const stats = customerStats(orders);
  const managementAllowed = shopSnapshot.get("customerStatusManagementAllowed") === true;
  return {details: {
    customer: summary(profile.id, profile.data()!, stats.get(customerId), managementAllowed),
    joinedAtEpochMillis: epoch(profile.get("createdAt")),
    highestOrderMinor: orders.reduce((highest, doc) => Math.max(highest, integer(doc.get("totalMinor") ?? doc.get("totalAmount"))), 0),
    orders: orders.slice(0, 3).map((doc) => orderResponse(doc.id, doc.data())),
  }};
});

export const adminSetCustomerActive = onCall(customerOptions, async (request) => {
  const context = await requireAdmin(request); const customerId = text(request.data?.customerId);
  const expected = integer(request.data?.expectedRevision, -1); const active = request.data?.active === true;
  if (!customerId || expected < 0) invalid("Customer revision is required");
  const [shopSnapshot, mappingQuery] = await Promise.all([
    shop(context).get(), getFirestore().collection("authCredentials").where("uid", "==", customerId).limit(1).get(),
  ]);
  if (shopSnapshot.get("customerStatusManagementAllowed") !== true) {
    throw new HttpsError("failed-precondition", "Customer status management is not enabled");
  }
  const reference = getFirestore().collection("users").doc(customerId);
  const customer = await getFirestore().runTransaction(async (transaction) => {
    const current = await transaction.get(reference); requireCustomer(current, context.shopId);
    if (integer(current.get("revision")) !== expected) throw new HttpsError("aborted", "Customer changed");
    const revision = expected + 1;
    transaction.update(reference, {active, revision, updatedAt: FieldValue.serverTimestamp(), updatedBy: context.uid});
    mappingQuery.docs.forEach((mapping) => transaction.update(mapping.ref, {active, updatedAt: FieldValue.serverTimestamp()}));
    transaction.create(shop(context).collection("auditLogs").doc(), {
      action: active ? "CUSTOMER_ACTIVATED" : "CUSTOMER_DEACTIVATED", actorUid: context.uid,
      targetCustomerId: customerId, createdAt: FieldValue.serverTimestamp(),
    });
    return {...current.data(), active, revision};
  });
  await getAuth().updateUser(customerId, {disabled: !active});
  const orders = await shop(context).collection("orders").limit(500).get();
  return {customer: summary(customerId, customer, customerStats(orders.docs).get(customerId), true)};
});

type CustomerStats = {orders: number; spending: number};
function customerStats(docs: FirebaseFirestore.QueryDocumentSnapshot[]) {
  const result = new Map<string, CustomerStats>();
  docs.forEach((doc) => {
    const customerId = text(doc.get("customerId")); if (!customerId) return;
    const current = result.get(customerId) ?? {orders: 0, spending: 0}; current.orders += 1;
    if (doc.get("orderStatus") === "DELIVERED" &&
        (doc.get("paymentStatus") === "PAID" || doc.get("paymentStatus") === "COLLECTED")) {
      current.spending += integer(doc.get("totalMinor") ?? doc.get("totalAmount"));
    }
    result.set(customerId, current);
  });
  return result;
}
function summary(id: string, value: FirebaseFirestore.DocumentData, stats: CustomerStats | undefined,
  managementAllowed: boolean) {
  return {id, displayName: text(value.displayName, "Customer"), mobileNumber: text(value.mobileNumber),
    active: value.active === true, totalOrders: stats?.orders ?? 0, totalSpendingMinor: stats?.spending ?? 0,
    currencyCode: "INR", statusManagementAllowed: managementAllowed, revision: integer(value.revision)};
}
function orderResponse(id: string, value: FirebaseFirestore.DocumentData) {
  const items = Array.isArray(value.items) ? value.items : [];
  return {id, displayNumber: text(value.displayNumber ?? value.orderNumber, id),
    createdAtEpochMillis: epoch(value.createdAtEpochMillis ?? value.createdAt),
    productSummary: items.map((item: any) => text(item?.name)).filter(Boolean).join(", ") || "Products unavailable",
    totalMinor: integer(value.totalMinor ?? value.totalAmount), paymentMethod: text(value.paymentMethod, "COD"),
    paymentStatus: text(value.paymentStatus, "PENDING"), orderStatus: text(value.orderStatus, "PENDING")};
}
function requireCustomer(snapshot: FirebaseFirestore.DocumentSnapshot, shopId: string) {
  if (!snapshot.exists || snapshot.get("role") !== "CUSTOMER" || text(snapshot.get("shopId"), "default") !== shopId) {
    throw new HttpsError("not-found", "Customer not found");
  }
}
function epoch(value: unknown): number {
  return value instanceof Timestamp ? value.toMillis() : Math.max(0, Number(value) || 0);
}
function invalid(message: string): never { throw new HttpsError("invalid-argument", message); }
