import {AggregateField, Timestamp} from "firebase-admin/firestore";
import {CallableRequest, HttpsError, onCall} from "firebase-functions/v2/https";
import {getFirestore} from "./database.js";

const options = {region: "asia-south1", enforceAppCheck: true, invoker: "public" as const,
  timeoutSeconds: 60, memory: "512MiB" as const};
const DAY = 86_400_000;

export const superAdminGetDashboard = onCall(options, async (request) => {
  await requireSuperAdmin(request);
  const firestore = getFirestore();
  const [shopsSnapshot, customersSnapshot, features, products, archived] = await Promise.all([
    firestore.collection("shops").select().get(),
    firestore.collection("users").where("role", "==", "CUSTOMER").get(),
    firestore.collection("appConfig").doc("features").get(),
    firestore.collectionGroup("products").count().get(),
    firestore.collectionGroup("products").where("archived", "==", true).count().get(),
  ]);
  const now = new Date();
  const monthStart = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), 1));
  const weekStart = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate() - 6));
  const nextMonthStart = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth() + 1, 1));
  const monthRange = {start: Timestamp.fromDate(monthStart), end: Timestamp.fromDate(nextMonthStart)};
  const weekRange = {start: Timestamp.fromDate(weekStart), end: Timestamp.fromDate(new Date(now.getTime() + 1))};
  const orderCollections = shopsSnapshot.docs.map((doc) => doc.ref.collection("orders"));
  const [totalOrders, pendingOrders, allRevenue, monthOrders, weekOrders] = await Promise.all([
    sumCounts(orderCollections.map((orders) => orders.count().get())),
    sumCounts(orderCollections.map((orders) => orders.where("orderStatus", "==", "PENDING").count().get())),
    sumAmounts(orderCollections.map((orders) => orders.where("orderStatus", "==", "DELIVERED")
      .aggregate({revenue: AggregateField.sum("totalMinor")}).get())),
    queryRange(orderCollections, "deliveredAt", monthRange.start, monthRange.end),
    queryRange(orderCollections, "deliveredAt", weekRange.start, weekRange.end),
  ]);
  const finalizedMonth = monthOrders.filter(finalized);
  const finalizedWeek = weekOrders.filter(finalized);
  const monthRevenue = finalizedMonth.reduce((sum, order) => sum + number(order.totalMinor ?? order.totalAmount), 0);
  const points = Array.from({length: 7}, (_, index) => {
    const dayStart = weekStart.getTime() + index * DAY;
    const revenue = finalizedWeek.filter((order) => {
      const date = dateMillis(order.deliveredAt ?? order.deliveredAtEpochMillis);
      return date >= dayStart && date < dayStart + DAY;
    }).reduce((sum, order) => sum + number(order.totalMinor ?? order.totalAmount), 0);
    return revenue;
  });
  const labels = Array.from({length: 7}, (_, index) => new Date(weekStart.getTime() + index * DAY)
    .toLocaleDateString("en-US", {weekday: "short", timeZone: "UTC"}));
  const activeCustomers = customersSnapshot.docs.filter((doc) => doc.get("active") !== false).length;
  const newCustomers = customersSnapshot.docs.filter((doc) => {
    const created = doc.get("createdAt");
    return created instanceof Timestamp && created.toDate() >= monthStart;
  }).length;
  const paymentTotals = new Map<string, number>();
  finalizedMonth.forEach((order) => {
    const method = String(order.paymentMethod ?? "COD") === "COD" ? "COD" : "ONLINE";
    paymentTotals.set(method, (paymentTotals.get(method) ?? 0) + number(order.totalMinor ?? order.totalAmount));
  });
  const paymentTotal = [...paymentTotals.values()].reduce((sum, value) => sum + value, 0);
  const paymentMethods = [...paymentTotals].map(([label, value]) => ({
    label, percentage: paymentTotal ? Math.round(value * 100 / paymentTotal) : 0,
  }));
  return {dashboard: {
    currencyCode: finalizedMonth[0]?.currencyCode ?? finalizedWeek[0]?.currencyCode ?? "INR",
    totalRevenueMinor: allRevenue,
    monthlyRevenueMinor: monthRevenue,
    totalOrders,
    pendingOrders,
    totalCustomers: customersSnapshot.size,
    activeCustomers,
    newCustomers,
    totalProducts: Math.max(0, products.data().count - archived.data().count),
    currentProductLimit: number(features.get("maxProducts"), 100),
    revenuePointsMinor: points,
    revenueLabels: labels,
    paymentMethods,
  }};
});

async function requireSuperAdmin(request: CallableRequest) {
  if (!request.auth) throw new HttpsError("unauthenticated", "Authentication required");
  const profile = await getFirestore().collection("users").doc(request.auth.uid).get();
  if (!profile.exists || profile.get("active") !== true || profile.get("role") !== "SUPER_ADMIN") {
    throw new HttpsError("permission-denied", "Super Admin access required");
  }
}
async function sumCounts(values: Promise<FirebaseFirestore.AggregateQuerySnapshot<FirebaseFirestore.AggregateSpec>>[]) {
  const snapshots = await Promise.all(values);
  return snapshots.reduce((sum, snapshot) => sum + number(snapshot.data().count), 0);
}
async function sumAmounts(values: Promise<FirebaseFirestore.AggregateQuerySnapshot<{revenue: FirebaseFirestore.AggregateField<number>}>>[]) {
  const snapshots = await Promise.all(values);
  return snapshots.reduce((sum, snapshot) => sum + number(snapshot.data().revenue), 0);
}
async function queryRange(collections: FirebaseFirestore.CollectionReference[], field: string,
  start: Timestamp, end: Timestamp) {
  const snapshots = await Promise.all(collections.map((collection) => collection
    .where(field, ">=", start).where(field, "<", end).get()));
  return snapshots.flatMap((snapshot) => snapshot.docs.map((doc) => doc.data()));
}
function finalized(order: FirebaseFirestore.DocumentData) {
  return order.orderStatus === "DELIVERED" && ["PAID", "COLLECTED"].includes(String(order.paymentStatus));
}
function dateMillis(value: unknown) {
  if (value instanceof Timestamp) return value.toMillis();
  const parsed = Number(value); return Number.isFinite(parsed) ? parsed : 0;
}
function number(value: unknown, fallback = 0) { const parsed = Number(value); return Number.isFinite(parsed) ? parsed : fallback; }
