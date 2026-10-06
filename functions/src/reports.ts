import {Timestamp} from "firebase-admin/firestore";
import {getFirestore} from "./database.js";
import {CallableRequest, HttpsError, onCall} from "firebase-functions/v2/https";
import {catalogOptions, requireAdmin, shop, text} from "./catalog-common.js";

const options = {region: "asia-south1", enforceAppCheck: true, timeoutSeconds: 30, memory: "256MiB" as const};
const adminReportOptions = {...catalogOptions, invoker: "public" as const};
const DAY = 86_400_000;
const allowedPeriods = new Set(["WEEKLY", "CURRENT_MONTH_BY_WEEK", "MONTHLY", "YEARLY", "CUSTOM"]);

type Range = {start: Date; end: Date; label: string; bucket: "day" | "week" | "month"; offsetMinutes: number};
type Order = FirebaseFirestore.DocumentData & {id: string};

export const superAdminGetReport = onCall(options, async (request) => {
  await requireSuperAdmin(request);
  const period = String(request.data?.period ?? "WEEKLY");
  if (!allowedPeriods.has(period)) throw new HttpsError("invalid-argument", "Invalid report period");
  const range = reportRange(period, request.data?.startDate, request.data?.endDate);
  const firestore = getFirestore();
  const shops = await firestore.collection("shops").select().get();
  const shopIds = shops.docs.map((shop) => shop.id);
  const [createdOrders, deliveredOrders, cancelledOrders, customers, deletedCustomerFacts] = await Promise.all([
    queryOrders(shopIds, "createdAt", range),
    queryOrders(shopIds, "deliveredAt", range),
    queryOrders(shopIds, "cancelledAt", range),
    firestore.collection("users").where("role", "==", "CUSTOMER").get(),
    firestore.collection("customerAccountFacts").where("joinedAt", ">=", Timestamp.fromDate(range.start))
      .where("joinedAt", "<", Timestamp.fromDate(range.end)).count().get(),
  ]);
  const finalized = deliveredOrders.filter(isFinalizedRevenue);
  const revenueMinor = finalized.reduce((total, order) => total + number(order.totalMinor), 0);
  const activeCustomers = new Set(createdOrders.filter((order) => order.orderStatus !== "CANCELLED")
    .map((order) => order.customerId || order.anonymousCustomerKey).filter(Boolean)).size;
  const newCustomers = customers.docs.filter((doc) => inRange(doc.get("createdAt"), range)).length +
    deletedCustomerFacts.data().count;
  const currencyCode = String(finalized[0]?.currencyCode ?? createdOrders[0]?.currencyCode ?? "INR");
  return {report: {
    periodLabel: range.label,
    currencyCode,
    revenueMinor,
    orders: createdOrders.length,
    delivered: deliveredOrders.length,
    cancelled: cancelledOrders.length,
    averageOrderValueMinor: finalized.length ? Math.round(revenueMinor / finalized.length) : 0,
    customers: customers.size,
    newCustomers,
    activeCustomers,
    revenueSeries: revenueSeries(finalized, range),
    bestSellingProducts: bestSelling(finalized),
  }};
});

export const adminGetReport = onCall(adminReportOptions, async (request) => {
  const context = await requireAdmin(request);
  const period = String(request.data?.period ?? "WEEKLY");
  if (!allowedPeriods.has(period)) throw new HttpsError("invalid-argument", "Invalid report period");
  const shopSnapshot = await shop(context).get();
  const offset = boundedOffset(shopSnapshot.get("timezoneOffsetMinutes"));
  const range = reportRange(period, request.data?.startDate, request.data?.endDate,
    request.data?.anchorDate, offset);
  const previous = previousRange(range);
  const productId = text(request.data?.productId).trim() || null;
  const [created, delivered, previousCreated, previousDelivered, product] = await Promise.all([
    queryShopOrders(context.shopId, "createdAt", range),
    queryShopOrders(context.shopId, "deliveredAt", range),
    queryShopOrders(context.shopId, "createdAt", previous),
    queryShopOrders(context.shopId, "deliveredAt", previous),
    productId ? shop(context).collection("products").doc(productId).get() : Promise.resolve(null),
  ]);
  if (productId && !product?.exists) throw new HttpsError("not-found", "Product not found");
  const current = adminSummary(created, delivered, productId);
  const prior = adminSummary(previousCreated, previousDelivered, productId);
  const selectedUnit = selectedProductUnit(current.topProducts);
  return {report: {
    periodLabel: range.label,
    currencyCode: String(delivered[0]?.currencyCode ?? created[0]?.currencyCode ?? "INR"),
    revenueMinor: current.revenueMinor,
    orders: current.orders,
    customers: current.customers,
    products: current.topProducts.length,
    activeProducts: current.topProducts.length,
    completedOrders: current.completedOrders,
    cancelledOrders: current.cancelledOrders,
    quantitySold: current.quantitySold,
    quantityUnit: selectedUnit,
    averageOrderValueMinor: current.completedOrders ? Math.round(current.revenueMinor / current.completedOrders) : 0,
    averageSellingPriceMinor: current.quantitySold ? Math.round(current.revenueMinor / current.quantitySold) : 0,
    revenueChangePercent: percentChange(current.revenueMinor, prior.revenueMinor),
    ordersChangePercent: percentChange(current.orders, prior.orders),
    revenueSeries: adminRevenueSeries(current.finalized, range, productId),
    topProducts: current.topProducts.slice(0, 5),
  }};
});

async function queryShopOrders(shopId: string, field: string, range: Range): Promise<Order[]> {
  const snapshot = await getFirestore().collection("shops").doc(shopId).collection("orders")
    .where(field, ">=", Timestamp.fromDate(range.start))
    .where(field, "<", Timestamp.fromDate(range.end)).get();
  return snapshot.docs.map((doc) => ({id: doc.id, ...doc.data()}));
}

export function adminSummary(created: Order[], delivered: Order[], productId: string | null) {
  const matchingCreated = created.filter((order) => !productId || orderItems(order)
    .some((item) => text(item.productId) === productId));
  const finalized = delivered.filter(isFinalizedRevenue).filter((order) => !productId || orderItems(order)
    .some((item) => text(item.productId) === productId));
  const topProducts = topProductReports(finalized, productId);
  const revenueMinor = productId ? topProducts.reduce((sum, item) => sum + item.revenueMinor, 0) :
    finalized.reduce((sum, order) => sum + number(order.totalMinor), 0);
  return {
    revenueMinor, orders: matchingCreated.length, finalized,
    completedOrders: finalized.length,
    cancelledOrders: matchingCreated.filter((order) => order.orderStatus === "CANCELLED").length,
    customers: new Set(matchingCreated.map((order) =>
      text(order.customerId, text(order.anonymousCustomerKey))).filter(Boolean)).size,
    quantitySold: topProducts.reduce((sum, item) => sum + item.quantitySold, 0), topProducts,
  };
}

function topProductReports(orders: Order[], selectedProductId: string | null) {
  const products = new Map<string, {productId: string; productName: string; imageUrl: string | null;
    unit: string; quantitySold: number; revenueMinor: number; orders: Set<string>}>();
  orders.forEach((order) => orderItems(order).forEach((item) => {
    const productId = text(item.productId, text(item.name, "unknown"));
    if (selectedProductId && productId !== selectedProductId) return;
    const unit = text(item.unit, "PIECE");
    const key = `${productId}|${unit}`;
    const current = products.get(key) ?? {productId, productName: text(item.name, "Product"),
      imageUrl: typeof item.imageUrl === "string" ? item.imageUrl : null, unit,
      quantitySold: 0, revenueMinor: 0, orders: new Set<string>()};
    current.quantitySold += number(item.quantity);
    current.revenueMinor += number(item.lineTotalMinor);
    current.orders.add(order.id);
    products.set(key, current);
  }));
  return [...products.values()].map((value) => ({productId: value.productId, productName: value.productName,
    imageUrl: value.imageUrl, unit: value.unit, quantitySold: value.quantitySold,
    revenueMinor: value.revenueMinor, orderCount: value.orders.size}))
    .sort((a, b) => b.revenueMinor - a.revenueMinor || b.quantitySold - a.quantitySold);
}

function adminRevenueSeries(orders: Order[], range: Range, productId: string | null) {
  const buckets = new Map<string, number>();
  for (let cursor = new Date(range.start); cursor < range.end;
    cursor = nextBucket(cursor, range.bucket, range.offsetMinutes)) {
    buckets.set(bucketKey(cursor, range.bucket, range.offsetMinutes), 0);
  }
  orders.forEach((order) => {
    const date = timestampDate(order.deliveredAt); if (!date) return;
    const revenue = productId ? orderItems(order).filter((item) => text(item.productId) === productId)
      .reduce((sum, item) => sum + number(item.lineTotalMinor), 0) : number(order.totalMinor);
    const key = bucketKey(date, range.bucket, range.offsetMinutes);
    buckets.set(key, (buckets.get(key) ?? 0) + revenue);
  });
  return [...buckets].map(([label, revenueMinor]) => ({label, revenueMinor}));
}

function orderItems(order: Order): FirebaseFirestore.DocumentData[] {
  return Array.isArray(order.items) ? order.items.filter((item): item is FirebaseFirestore.DocumentData =>
    item != null && typeof item === "object") : [];
}

function previousRange(range: Range): Range {
  const duration = range.end.getTime() - range.start.getTime();
  const end = new Date(range.start); const start = new Date(end.getTime() - duration);
  return {start, end, label: "Previous period", bucket: range.bucket, offsetMinutes: range.offsetMinutes};
}
function percentChange(current: number, previous: number) {
  return previous > 0 ? Math.round(((current - previous) / previous) * 1000) / 10 : null;
}
function selectedProductUnit(items: Array<{unit: string}>) {
  const units = new Set(items.map((item) => item.unit)); return units.size === 1 ? [...units][0] : null;
}
function boundedOffset(value: unknown) { const parsed = number(value); return parsed >= -720 && parsed <= 840 ? parsed : 330; }

async function queryOrders(shopIds: string[], field: string, range: Range): Promise<Order[]> {
  const firestore = getFirestore();
  const snapshots = await Promise.all(shopIds.map((shopId) => firestore.collection("shops").doc(shopId)
    .collection("orders")
    .where(field, ">=", Timestamp.fromDate(range.start))
    .where(field, "<", Timestamp.fromDate(range.end))
    .get()));
  return snapshots.flatMap((snapshot) => snapshot.docs.map((doc) => ({id: doc.id, ...doc.data()})));
}

async function requireSuperAdmin(request: CallableRequest): Promise<void> {
  if (!request.auth) throw new HttpsError("unauthenticated", "Authentication required");
  const profile = await getFirestore().collection("users").doc(request.auth.uid).get();
  if (!profile.exists || profile.get("active") !== true || profile.get("role") !== "SUPER_ADMIN") {
    throw new HttpsError("permission-denied", "Super Admin access required");
  }
}

function reportRange(period: string, startValue: unknown, endValue: unknown,
  anchorValue?: unknown, offsetMinutes = 0): Range {
  const now = parseDate(anchorValue) ?? new Date();
  const year = now.getUTCFullYear();
  const month = now.getUTCMonth();
  if (period === "WEEKLY") {
    const local = new Date(now.getTime() + offsetMinutes * 60_000);
    const startLocal = utcDay(local); const mondayOffset = (startLocal.getUTCDay() + 6) % 7;
    startLocal.setUTCDate(startLocal.getUTCDate() - mondayOffset);
    const start = new Date(startLocal.getTime() - offsetMinutes * 60_000);
    return {start, end: new Date(start.getTime() + 7 * DAY), label: weekLabel(startLocal),
      bucket: "day", offsetMinutes};
  }
  if (period === "YEARLY") {
    return {start: new Date(Date.UTC(year, 0, 1) - offsetMinutes * 60_000),
      end: new Date(Date.UTC(year + 1, 0, 1) - offsetMinutes * 60_000),
      label: String(year), bucket: "month", offsetMinutes};
  }
  if (period === "CUSTOM") {
    const displayStart = parseDate(startValue); const displayEnd = parseDate(endValue);
    const start = displayStart && new Date(displayStart.getTime() - offsetMinutes * 60_000);
    const inclusiveEnd = displayEnd && new Date(displayEnd.getTime() - offsetMinutes * 60_000);
    if (!start || !inclusiveEnd || inclusiveEnd < start || inclusiveEnd.getTime() - start.getTime() > 365 * DAY) {
      throw new HttpsError("invalid-argument", "Select a valid report range");
    }
    const days = Math.round((inclusiveEnd.getTime() - start.getTime()) / DAY) + 1;
    return {start, end: new Date(inclusiveEnd.getTime() + DAY),
      label: `${iso(displayStart!)} – ${iso(displayEnd!)}`,
      bucket: days > 60 ? "month" : days > 7 ? "week" : "day", offsetMinutes};
  }
  const start = new Date(Date.UTC(year, month, 1) - offsetMinutes * 60_000);
  const end = new Date(Date.UTC(year, month + 1, 1));
  end.setTime(end.getTime() - offsetMinutes * 60_000);
  return {start, end, label: monthLabel(new Date(start.getTime() + offsetMinutes * 60_000)),
    bucket: period === "CURRENT_MONTH_BY_WEEK" || period === "MONTHLY" ? "week" : "day", offsetMinutes};
}

function revenueSeries(orders: Order[], range: Range) {
  const buckets = new Map<string, number>();
  for (let cursor = new Date(range.start); cursor < range.end;
    cursor = nextBucket(cursor, range.bucket, range.offsetMinutes)) {
    buckets.set(bucketKey(cursor, range.bucket, range.offsetMinutes), 0);
  }
  for (const order of orders) {
    const date = timestampDate(order.deliveredAt);
    if (date) {
      const key = bucketKey(date, range.bucket, range.offsetMinutes);
      buckets.set(key, (buckets.get(key) ?? 0) + number(order.totalMinor));
    }
  }
  return [...buckets].map(([label, revenueMinor]) => ({label, revenueMinor}));
}

function bestSelling(orders: Order[]) {
  const products = new Map<string, {productId: string; name: string; quantitySold: number; revenueMinor: number}>();
  orders.flatMap((order) => Array.isArray(order.items) ? order.items : []).forEach((item) => {
    const id = String(item.productId ?? item.name ?? "unknown");
    const current = products.get(id) ?? {productId: id, name: String(item.name ?? "Product"), quantitySold: 0, revenueMinor: 0};
    current.quantitySold += number(item.quantity);
    current.revenueMinor += number(item.lineTotalMinor);
    products.set(id, current);
  });
  return [...products.values()].sort((a, b) => b.quantitySold - a.quantitySold || b.revenueMinor - a.revenueMinor).slice(0, 10);
}

export function isFinalizedRevenue(order: Order) {
  return order.orderStatus === "DELIVERED" && (order.paymentStatus === "PAID" || order.paymentStatus === "COLLECTED");
}
function inRange(value: unknown, range: Range) {
  const date = timestampDate(value);
  return date != null && date >= range.start && date < range.end;
}
function timestampDate(value: unknown): Date | null {
  if (value instanceof Timestamp) return value.toDate();
  const millis = number(value);
  return millis > 0 ? new Date(millis) : null;
}
function nextBucket(date: Date, bucket: Range["bucket"], offsetMinutes = 0) {
  const local = new Date(date.getTime() + offsetMinutes * 60_000);
  if (bucket === "month") local.setUTCMonth(local.getUTCMonth() + 1, 1);
  else local.setUTCDate(local.getUTCDate() + (bucket === "week" ? 7 : 1));
  return new Date(local.getTime() - offsetMinutes * 60_000);
}
function bucketKey(date: Date, bucket: Range["bucket"], offsetMinutes = 0) {
  const local = new Date(date.getTime() + offsetMinutes * 60_000);
  if (bucket === "month") return local.toLocaleString("en", {month: "short", timeZone: "UTC"});
  if (bucket === "week") {
    const start = 1 + Math.floor((local.getUTCDate() - 1) / 7) * 7;
    return `${start}-${Math.min(start + 6, daysInMonth(local))}`;
  }
  return String(local.getUTCDate()).padStart(2, "0");
}
function daysInMonth(date: Date) {
  return new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth() + 1, 0)).getUTCDate();
}
function parseDate(value: unknown) {
  if (typeof value !== "string" || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return null;
  const parsed = new Date(`${value}T00:00:00.000Z`);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}
function utcDay(date: Date) { return new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate())); }
function iso(date: Date) { return date.toISOString().slice(0, 10); }
function monthLabel(date: Date) { return date.toLocaleString("en", {month: "long", year: "numeric", timeZone: "UTC"}); }
function weekLabel(date: Date) { return `Week of ${date.toLocaleString("en", {day: "numeric", month: "short", year: "numeric", timeZone: "UTC"})}`; }
function number(value: unknown) { const parsed = Number(value); return Number.isFinite(parsed) ? parsed : 0; }
