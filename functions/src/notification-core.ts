import {createHash} from "node:crypto";
import {FieldValue, Timestamp} from "firebase-admin/firestore";
import {getMessaging} from "firebase-admin/messaging";
import {getFirestore} from "./database.js";

export type NotificationCategory = "ORDER" | "PAYMENT" | "DELIVERY" | "STOCK" |
  "PROMOTION" | "SYSTEM" | "REPORT";
export type NotificationPriority = "NORMAL" | "HIGH";
export type NotificationInput = {
  recipientUid: string;
  recipientRole: string;
  event: string;
  category: NotificationCategory;
  priority?: NotificationPriority;
  title: string;
  body: string;
  eventKey: string;
  shopId?: string;
  orderId?: string;
  productId?: string;
  reportPeriod?: string;
  deepLinkRoute?: string;
  metadata?: Record<string, string>;
};

export async function notify(input: NotificationInput): Promise<boolean> {
  if (!input.recipientUid) return false;
  const firestore = getFirestore();
  const id = createHash("sha256").update(input.eventKey).digest("hex");
  const features = await firestore.collection("appConfig").doc("features").get();
  if (features.get("inAppNotificationsEnabled") === false) {
    await push(input).catch((error) => console.error("Notification push failed", input.event, id, error));
    return true;
  }
  const reference = firestore.collection("users").doc(input.recipientUid).collection("notifications").doc(id);
  const created = await firestore.runTransaction(async (transaction) => {
    if ((await transaction.get(reference)).exists) return false;
    transaction.create(reference, {
      ...input,
      priority: input.priority ?? "NORMAL",
      readAt: null,
      createdAt: FieldValue.serverTimestamp(),
      expiresAt: Timestamp.fromMillis(Date.now() + 3 * 24 * 60 * 60 * 1000),
    });
    return true;
  });
  if (!created) return false;
  await push(input, id).catch((error) => console.error("Notification push failed", input.event, id, error));
  return true;
}

async function push(input: NotificationInput, notificationId?: string) {
  const devices = await getFirestore().collection("users").doc(input.recipientUid)
    .collection("devices").where("isActive", "==", true).limit(20).get();
  const tokens = [...new Set(devices.docs.map((doc) => String(doc.get("fcmToken") ?? "")).filter(Boolean))];
  if (!tokens.length) return;
  const channelId = channel(input.category);
  const result = await getMessaging().sendEachForMulticast({
    tokens,
    notification: {title: input.title, body: input.body},
    data: compact({
      notificationId,
      title: input.title,
      body: input.body,
      event: input.event,
      category: input.category,
      deepLinkRoute: input.deepLinkRoute,
      orderId: input.orderId,
      productId: input.productId,
      reportPeriod: input.reportPeriod,
    }),
    android: {
      priority: input.priority === "HIGH" ? "high" : "normal",
      notification: {channelId, sound: "default"},
    },
  });
  const invalid = new Set(["messaging/registration-token-not-registered", "messaging/invalid-registration-token"]);
  const batch = getFirestore().batch();
  result.responses.forEach((response, index) => {
    if (!response.success && invalid.has(response.error?.code ?? "")) {
      devices.docs.filter((doc) => doc.get("fcmToken") === tokens[index]).forEach((doc) =>
        batch.update(doc.ref, {isActive: false, invalidatedAt: FieldValue.serverTimestamp()}));
    }
  });
  await batch.commit();
}

export async function usersForShop(shopId: string, role: string) {
  const snapshot = await getFirestore().collection("users")
    .where("shopId", "==", shopId).where("role", "==", role).where("active", "==", true).get();
  return snapshot.docs.map((doc) => doc.id);
}

export async function usersForRole(role: string) {
  const snapshot = await getFirestore().collection("users")
    .where("role", "==", role).where("active", "==", true).get();
  return snapshot.docs.map((doc) => doc.id);
}

export function orderNumber(value: FirebaseFirestore.DocumentData, fallback: string) {
  return String(value.displayNumber ?? value.orderNumber ?? fallback);
}

export function money(minor: unknown, currency = "INR") {
  const value = Number(minor ?? 0) / 100;
  return new Intl.NumberFormat("en-IN", {style: "currency", currency, maximumFractionDigits: 0}).format(value);
}

export function epoch(value: unknown): number {
  if (value instanceof Timestamp) return value.toMillis();
  const parsed = Number(value); return Number.isFinite(parsed) ? parsed : 0;
}

export function text(value: unknown, fallback = "") {
  return typeof value === "string" ? value : fallback;
}

export function tokenId(token: string) {
  return createHash("sha256").update(token).digest("hex");
}

function compact(value: Record<string, string | undefined>) {
  return Object.fromEntries(Object.entries(value).filter((entry): entry is [string, string] => Boolean(entry[1])));
}

function channel(category: NotificationCategory) {
  if (category === "ORDER") return "orders";
  if (category === "PAYMENT") return "payments";
  if (category === "DELIVERY") return "delivery";
  if (category === "PROMOTION") return "promotions";
  return "system_reports";
}
