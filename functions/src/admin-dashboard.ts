import {Timestamp} from "firebase-admin/firestore";
import {getFirestore} from "./database.js";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {catalogOptions, requireAdmin, shop, text} from "./catalog-common.js";

const options = {...catalogOptions, invoker: "public" as const};
const DAY = 86_400_000;
const periods = new Set(["TODAY", "THIS_WEEK", "THIS_MONTH"]);
type Range = {start: Date; end: Date; label: string; period: string};
export type DashboardOrder = FirebaseFirestore.DocumentData & {id: string};

export const adminGetDashboard = onCall(options, async (request) => {
  const context = await requireAdmin(request);
  const period = text(request.data?.period, "THIS_WEEK").toUpperCase();
  if (!periods.has(period)) throw new HttpsError("invalid-argument", "Invalid dashboard period");
  const [profile, shopSnapshot] = await Promise.all([
    getFirestore().collection("users").doc(context.uid).get(), shop(context).get(),
  ]);
  const notificationsEnabled = (await getFirestore().collection("appConfig").doc("features").get())
    .get("inAppNotificationsEnabled") !== false;
  const offset = boundedOffset(shopSnapshot.get("timezoneOffsetMinutes"));
  const today = dashboardRange("TODAY", new Date(), offset);
  const selected = dashboardRange(period, new Date(), offset);
  const orderCollection = shop(context).collection("orders");
  const todayCreatedPromise = rangeQuery(orderCollection, "createdAt", today);
  const todayDeliveredPromise = rangeQuery(orderCollection, "deliveredAt", today);
  const selectedDeliveredPromise = period === "TODAY" ? todayDeliveredPromise :
    rangeQuery(orderCollection, "deliveredAt", selected);
  const [todayCreated, todayDelivered, selectedDelivered, pending, unread] = await Promise.all([
    todayCreatedPromise, todayDeliveredPromise, selectedDeliveredPromise,
    orderCollection.where("orderStatus", "==", "PENDING").count().get(),
    notificationsEnabled ? unreadCount(context.uid) : 0,
  ]);
  const summary = summarizeDashboard(todayCreated, todayDelivered, selectedDelivered, selected);
  return {dashboard: {
    adminName: text(profile.get("firstName"), text(profile.get("displayName"), "Admin")).trim(),
    currencyCode: summary.currencyCode,
    todayRevenueMinor: summary.todayRevenueMinor,
    todayOrders: todayCreated.length,
    deliveredOrders: todayDelivered.filter((order) => order.orderStatus === "DELIVERED").length,
    pendingOrders: pending.data().count,
    unreadNotificationCount: unread,
    period,
    periodLabel: selected.label,
    revenueSeries: summary.revenueSeries,
    topSellingProduct: summary.topSellingProduct,
    topSellingProducts: summary.topSellingProducts,
  }};
});

async function rangeQuery(
  collection: FirebaseFirestore.CollectionReference,
  field: string,
  range: Range,
): Promise<DashboardOrder[]> {
  const snapshot = await collection.where(field, ">=", Timestamp.fromDate(range.start))
    .where(field, "<", Timestamp.fromDate(range.end)).get();
  return snapshot.docs.map((document) => ({id: document.id, ...document.data()}));
}

async function unreadCount(uid: string) {
  return getFirestore().collection("users").doc(uid).collection("notifications")
    .where("readAt", "==", null).count().get().then((result) => result.data().count).catch(() => 0);
}

export function summarizeDashboard(
  todayCreated: DashboardOrder[],
  todayDelivered: DashboardOrder[],
  selectedDelivered: DashboardOrder[],
  range: Range,
) {
  const finalizedToday = todayDelivered.filter(finalizedRevenue);
  const finalizedSelected = selectedDelivered.filter(finalizedRevenue);
  return {
    currencyCode: text(finalizedToday[0]?.currencyCode,
      text(todayCreated[0]?.currencyCode, text(finalizedSelected[0]?.currencyCode, "INR"))),
    todayRevenueMinor: finalizedToday.reduce((sum, order) => sum + number(order.totalMinor ?? order.totalAmount), 0),
    revenueSeries: series(finalizedSelected, range),
    topSellingProduct: topProducts(finalizedSelected, 3)[0] ?? null,
    topSellingProducts: topProducts(finalizedSelected, 3),
  };
}

function series(orders: DashboardOrder[], range: Range) {
  const labels = bucketLabels(range);
  const values = new Map(labels.map((label) => [label, 0]));
  orders.forEach((order) => {
    const date = dateValue(order.deliveredAt ?? order.deliveredAtEpochMillis);
    if (!date) return;
    const label = bucketLabel(date, range);
    if (values.has(label)) values.set(label, (values.get(label) ?? 0) + number(order.totalMinor ?? order.totalAmount));
  });
  return [...values].map(([label, revenueMinor]) => ({label, revenueMinor}));
}

function topProducts(orders: DashboardOrder[], limit: number) {
  const products = new Map<string, {productId: string; name: string; imageUrl: string | null;
    unit: string; quantitySold: number; revenueMinor: number}>();
  orders.forEach((order) => items(order).forEach((item) => {
    const productId = text(item.productId, text(item.name, "unknown"));
    const unit = text(item.unit, "piece");
    const key = `${productId}|${unit}`;
    const current = products.get(key) ?? {productId, name: text(item.name, "Product"),
      imageUrl: typeof item.imageUrl === "string" ? item.imageUrl : null,
      unit, quantitySold: 0, revenueMinor: 0};
    current.quantitySold += number(item.quantity);
    current.revenueMinor += number(item.lineTotalMinor ?? item.lineTotal);
    products.set(key, current);
  }));
  return [...products.values()].sort((a, b) => b.revenueMinor - a.revenueMinor ||
    b.quantitySold - a.quantitySold).slice(0, limit);
}

function dashboardRange(period: string, now: Date, offsetMinutes: number): Range {
  const local = new Date(now.getTime() + offsetMinutes * 60_000);
  const startLocal = new Date(Date.UTC(local.getUTCFullYear(), local.getUTCMonth(), local.getUTCDate()));
  if (period === "THIS_WEEK") startLocal.setUTCDate(startLocal.getUTCDate() - (startLocal.getUTCDay() + 6) % 7);
  if (period === "THIS_MONTH") startLocal.setUTCDate(1);
  const start = new Date(startLocal.getTime() - offsetMinutes * 60_000);
  const endLocal = new Date(startLocal);
  if (period === "TODAY") endLocal.setUTCDate(endLocal.getUTCDate() + 1);
  if (period === "THIS_WEEK") endLocal.setUTCDate(endLocal.getUTCDate() + 7);
  if (period === "THIS_MONTH") endLocal.setUTCMonth(endLocal.getUTCMonth() + 1, 1);
  const end = new Date(endLocal.getTime() - offsetMinutes * 60_000);
  const label = period === "TODAY" ? "Today" : period === "THIS_WEEK" ? "This Week" : "This Month";
  return {start, end, label, period};
}

function bucketLabels(range: Range) {
  if (range.period === "TODAY") return ["12am", "4am", "8am", "12pm", "4pm", "8pm"];
  if (range.period === "THIS_WEEK") return ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
  const days = Math.ceil((range.end.getTime() - range.start.getTime()) / DAY);
  return Array.from({length: Math.ceil(days / 7)}, (_, index) => {
    const start = index * 7 + 1; return `${start}-${Math.min(start + 6, days)}`;
  });
}

function bucketLabel(date: Date, range: Range) {
  const local = new Date(date.getTime() - range.start.getTime());
  const day = Math.floor(local.getTime() / DAY);
  if (range.period === "TODAY") return bucketLabels(range)[Math.min(5, Math.floor(local.getUTCHours() / 4))];
  if (range.period === "THIS_WEEK") return bucketLabels(range)[Math.min(6, day)];
  return bucketLabels(range)[Math.min(bucketLabels(range).length - 1, Math.floor(day / 7))];
}

function finalizedRevenue(order: DashboardOrder) {
  return order.orderStatus === "DELIVERED" && (order.paymentStatus === "PAID" || order.paymentStatus === "COLLECTED");
}
function items(order: DashboardOrder): FirebaseFirestore.DocumentData[] {
  return Array.isArray(order.items) ? order.items.filter((item) => item && typeof item === "object") : [];
}
function dateValue(value: unknown): Date | null {
  if (value instanceof Timestamp) return value.toDate();
  const milliseconds = number(value); return milliseconds > 0 ? new Date(milliseconds) : null;
}
function boundedOffset(value: unknown) { const parsed = number(value); return parsed >= -720 && parsed <= 840 ? parsed : 330; }
function number(value: unknown) { const parsed = Number(value); return Number.isFinite(parsed) ? parsed : 0; }
