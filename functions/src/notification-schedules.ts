import {Timestamp} from "firebase-admin/firestore";
import {onSchedule} from "firebase-functions/v2/scheduler";
import {getFirestore} from "./database.js";
import {money, notify, orderNumber, text, usersForRole, usersForShop} from "./notification-core.js";

const scheduled = {region: "asia-south1", timeZone: "Asia/Kolkata", memory: "256MiB" as const};
const activeStatuses = ["PENDING", "CONFIRMED", "PREPARING", "OUT_FOR_DELIVERY"];

export const sendDelayedOrderNotifications = onSchedule({...scheduled, schedule: "every 15 minutes"}, async () => {
  const snapshot = await getFirestore().collectionGroup("orders").where("orderStatus", "in", activeStatuses).limit(500).get();
  const now = Date.now();
  await Promise.all(snapshot.docs.map(async (doc) => {
    const value = doc.data(); const end = deliveryEnd(value); if (!end || end > now) return;
    const shopId = doc.ref.parent.parent?.id; if (!shopId) return;
    const number = orderNumber(value, doc.id); const date = new Date(end).toISOString().slice(0, 10);
    await notify({recipientUid: text(value.customerId), recipientRole: "CUSTOMER", event: "DELIVERY_DELAYED",
      category: "DELIVERY", priority: "HIGH", title: "Delivery Delayed",
      body: `Your delivery for order ${number} is taking longer than expected.`,
      eventKey: `${doc.id}:delayed:${date}:customer`, shopId, orderId: doc.id, deepLinkRoute: "customer/orders"});
    const admins = await usersForShop(shopId, "ADMIN");
    await Promise.all(admins.map((uid) => notify({recipientUid: uid, recipientRole: "ADMIN", event: "DELIVERY_ISSUE",
      category: "DELIVERY", priority: "HIGH", title: "Delivery Issue", body: `${number} has passed its delivery slot.`,
      eventKey: `${doc.id}:delayed:${date}:admin:${uid}`, shopId, orderId: doc.id, deepLinkRoute: "admin/orders"})));
  }));
});

export const sendDeliveryStartReminders = onSchedule({...scheduled, schedule: "every 15 minutes"}, async () => {
  const snapshot = await getFirestore().collectionGroup("orders").where("orderStatus", "in", ["CONFIRMED", "PREPARING"])
    .limit(500).get(); const now = Date.now();
  await Promise.all(snapshot.docs.map(async (doc) => {
    const value = doc.data(); const start = deliveryStart(value);
    if (!start || start < now || start - now > 45 * 60_000) return;
    const uid = text(value.assignedDeliveryUserId) || (value.adminDeliveringPersonally === true ? text(value.assignedByAdminId) : "");
    if (!uid) return; const role = value.adminDeliveringPersonally === true ? "ADMIN" : "DELIVERY";
    await notify({recipientUid: uid, recipientRole: role, event: "DELIVERY_START_REMINDER", category: "DELIVERY",
      priority: "HIGH", title: "Delivery Reminder",
      body: `${orderNumber(value, doc.id)} is due for delivery soon.`,
      eventKey: `${doc.id}:start-reminder:${new Date(start).toISOString()}`, shopId: doc.ref.parent.parent?.id,
      orderId: doc.id, deepLinkRoute: role === "ADMIN" ? "admin/orders" : "delivery/orders"});
  }));
});

export const sendWeeklySuperAdminReport = onSchedule({...scheduled, schedule: "30 9 * * 1"}, async () => {
  const end = startOfTodayIst(); const start = end - 7 * 86_400_000;
  await sendReport("WEEKLY_REPORT", "Weekly Business Summary", start, end);
});

export const sendMonthlySuperAdminReport = onSchedule({...scheduled, schedule: "0 9 1 * *"}, async () => {
  const local = new Date(Date.now() + 330 * 60_000);
  const end = Date.UTC(local.getUTCFullYear(), local.getUTCMonth(), 1) - 330 * 60_000;
  const start = Date.UTC(local.getUTCFullYear(), local.getUTCMonth() - 1, 1) - 330 * 60_000;
  await sendReport("MONTHLY_REPORT", "Monthly Business Summary", start, end);
});

async function sendReport(event: "WEEKLY_REPORT" | "MONTHLY_REPORT", title: string, start: number, end: number) {
  const [orders, customers, superAdmins] = await Promise.all([
    getFirestore().collectionGroup("orders").where("createdAt", ">=", Timestamp.fromMillis(start))
      .where("createdAt", "<", Timestamp.fromMillis(end)).get(),
    getFirestore().collection("users").where("role", "==", "CUSTOMER").where("createdAt", ">=", Timestamp.fromMillis(start))
      .where("createdAt", "<", Timestamp.fromMillis(end)).get().catch(() => null),
    usersForRole("SUPER_ADMIN"),
  ]);
  const all = orders.docs.map((doc) => doc.data());
  const delivered = all.filter((order) => order.orderStatus === "DELIVERED");
  const finalized = delivered.filter((order) => order.paymentStatus === "PAID" || order.paymentStatus === "COLLECTED");
  const revenue = finalized.reduce((sum, order) => sum + Number(order.totalMinor ?? 0), 0);
  const cancelled = all.filter((order) => order.orderStatus === "CANCELLED").length;
  const pending = all.length - delivered.length - cancelled;
  const products = new Map<string, {name: string; quantity: number}>();
  finalized.flatMap((order) => Array.isArray(order.items) ? order.items : []).forEach((item) => {
    const id = text(item.productId, text(item.name)); const current = products.get(id) ?? {name: text(item.name, "Product"), quantity: 0};
    current.quantity += Number(item.quantity ?? 0); products.set(id, current);
  });
  const top = [...products.values()].sort((a, b) => b.quantity - a.quantity)[0]?.name ?? "—";
  const period = `${new Date(start).toISOString().slice(0, 10)}_${new Date(end - 1).toISOString().slice(0, 10)}`;
  const details = {revenueMinor: String(revenue), orders: String(all.length), delivered: String(delivered.length),
    cancelled: String(cancelled), pending: String(pending), customers: String(customers?.size ?? 0),
    averageOrderValueMinor: String(finalized.length ? Math.round(revenue / finalized.length) : 0), topProduct: top};
  await Promise.all(superAdmins.map((uid) => notify({recipientUid: uid, recipientRole: "SUPER_ADMIN", event,
    category: "REPORT", title, body: `${money(revenue)} • ${all.length} orders. Tap to view full report.`,
    eventKey: `${event}:${period}:${uid}`, reportPeriod: period, deepLinkRoute: "super_admin/reports", metadata: details})));
}

function deliveryStart(value: FirebaseFirestore.DocumentData) { return deliveryMoment(value, "startMinutes"); }
function deliveryEnd(value: FirebaseFirestore.DocumentData) { return deliveryMoment(value, "endMinutes"); }
export function deliveryMoment(value: FirebaseFirestore.DocumentData, field: string) {
  const date = text(value.deliveryDate ?? value.deliverySlotSnapshot?.date);
  const minutes = Number(value.deliverySlotSnapshot?.[field] ?? value[`deliverySlot${field[0].toUpperCase()}${field.slice(1)}`]);
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date) || !Number.isFinite(minutes)) return 0;
  const [year, month, day] = date.split("-").map(Number);
  return Date.UTC(year, month - 1, day, 0, minutes) - 330 * 60_000;
}
function startOfTodayIst() {
  const local = new Date(Date.now() + 330 * 60_000);
  return Date.UTC(local.getUTCFullYear(), local.getUTCMonth(), local.getUTCDate()) - 330 * 60_000;
}
