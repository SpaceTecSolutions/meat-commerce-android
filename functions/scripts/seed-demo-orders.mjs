import {createRequire} from "node:module";
import {join} from "node:path";

const projectId = process.env.FIREBASE_PROJECT_ID?.trim();
const shopId = process.env.SHOP_ID?.trim() || "default";
if (!projectId) throw new Error("FIREBASE_PROJECT_ID is required");

const require = createRequire(import.meta.url);
const firebaseTools = join(process.env.APPDATA || "", "npm", "node_modules", "firebase-tools", "lib");
const auth = require(join(firebaseTools, "auth.js"));
const api = require(join(firebaseTools, "apiv2.js"));
const account = auth.getGlobalDefaultAccount();
if (!account) throw new Error("Firebase CLI login required. Run: firebase login");
auth.setActiveAccount({}, account);
const accessToken = await api.getAccessToken();

const now = Date.now();
const meatImage = "https://images.unsplash.com/photo-1607623814075-e51df1bdc82f?w=240&q=75";
const chickenImage = "https://images.unsplash.com/photo-1604503468506-a8da13d82791?w=240&q=75";
const items = [
  item("mutton-curry-cut", "Mutton Curry Cut", meatImage, "KILOGRAM", 1, 75000, {Weight: "1 kg"}),
  item("chicken-skinless", "Chicken Skinless", chickenImage, "KILOGRAM", 1, 22000, {Weight: "1 kg"}),
  item("eggs-six", "Eggs (Pack of 6)", null, "PACK", 1, 6000, {Pack: "6 pieces"}),
];

const orders = [
  order("demo-order-pending-1", "ORD-1025", "Rahul Sharma", "9876543210", "PENDING", "COD", "PENDING", 0,
    {instructions: "Please call before delivery.", deliverySlotDateLabel: "Tomorrow", deliverySlotTimeLabel: "11:00 AM - 1:00 PM"}),
  order("demo-order-pending-2", "ORD-1022", "Sneha Iyer", "9012345678", "PENDING", "UPI", "PAID", 18),
  order("demo-order-confirmed", "ORD-1020", "Arjun Nair", "9988776655", "CONFIRMED", "RAZORPAY", "PAID", 32,
    {confirmedAtEpochMillis: now - 25 * 60_000, confirmedByAdminId: "demo-admin"}),
  order("demo-order-preparing", "ORD-1024", "Priya Mehta", "9123456780", "PREPARING", "COD", "PENDING", 46,
    {confirmedAtEpochMillis: now - 60 * 60_000, preparingAtEpochMillis: now - 30 * 60_000}),
  order("demo-order-ready", "ORD-1019", "Kavya Rao", "9345678901", "PREPARING", "COD", "PENDING", 61,
    {confirmedAtEpochMillis: now - 90 * 60_000, preparingAtEpochMillis: now - 65 * 60_000,
      readyForDeliveryAtEpochMillis: now - 10 * 60_000}),
  order("demo-order-out", "ORD-1018", "Mohammed Ali", "9567890123", "OUT_FOR_DELIVERY", "UPI", "PAID", 78,
    {assignedDeliveryUserId: "demo-delivery", assignedDeliveryUserName: "Ravi Delivery", assignedDeliveryRole: "DELIVERY",
      assignedAtEpochMillis: now - 45 * 60_000, deliveryStartedAtEpochMillis: now - 20 * 60_000,
      deliveryStartedByUserId: "demo-delivery", outForDeliveryAtEpochMillis: now - 20 * 60_000}),
  order("demo-order-delivered", "ORD-1023", "Amit Verma", "9789012345", "DELIVERED", "COD", "COLLECTED", 94,
    {deliveredAtEpochMillis: now - 70 * 60_000, deliveredByUserId: "demo-delivery"}),
  order("demo-order-cancelled", "ORD-1017", "Vikram Singh", "9890123456", "CANCELLED", "UPI", "REFUNDED", 112,
    {cancelReason: "Customer requested", cancelledAtEpochMillis: now - 100 * 60_000,
      cancelledByUserId: "demo-admin", cancelledByRole: "ADMIN"}),
];

const writes = orders.map(({id, ...data}) => ({
  update: {
    name: `projects/${projectId}/databases/default/documents/shops/${shopId}/orders/${id}`,
    fields: fields(data),
  },
}));
const endpoint = `https://firestore.googleapis.com/v1/projects/${projectId}/databases/default/documents:commit`;
const response = await fetch(endpoint, {
  method: "POST",
  headers: {Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json"},
  body: JSON.stringify({writes}),
});
if (!response.ok) throw new Error(`Firestore seed failed (${response.status}): ${await response.text()}`);
console.log(`Seeded ${orders.length} demo orders into shops/${shopId}/orders.`);

function item(productId, name, imageUrl, unit, quantity, unitPriceMinor, attributes) {
  return {productId, name, imageUrl, unit, quantity, unitPriceMinor, regularPriceMinor: unitPriceMinor,
    lineTotalMinor: unitPriceMinor * quantity, attributes};
}

function order(id, displayNumber, customerName, customerMobile, orderStatus, paymentMethod, paymentStatus,
  ageMinutes, overrides = {}) {
  const subtotalMinor = items.reduce((sum, value) => sum + value.lineTotalMinor, 0);
  const discountMinor = displayNumber === "ORD-1025" ? 5000 : 0;
  const deliveryFeeMinor = subtotalMinor >= 100000 ? 0 : 3000;
  const totalMinor = subtotalMinor - discountMinor + deliveryFeeMinor;
  return {id, displayNumber, orderNumber: displayNumber, customerId: `demo-${customerMobile}`,
    customerName, customerMobile, customerPhone: customerMobile, addressLabel: "Home",
    addressSummary: "23, 3rd Cross, Koramangala, Bengaluru - 560034",
    deliveryAddressSnapshot: {type: "Home", formattedAddress: "23, 3rd Cross, Koramangala, Bengaluru - 560034"},
    items, subtotalMinor, itemTotal: subtotalMinor, discountMinor, discountAmount: discountMinor,
    deliveryFeeMinor, deliveryCharge: deliveryFeeMinor, taxMinor: 0, taxAmount: 0,
    totalMinor, totalAmount: totalMinor, amountDueMinor: totalMinor, currencyCode: "INR",
    orderStatus, paymentMethod, paymentStatus, customerCancellationAllowed: orderStatus === "PENDING",
    createdAtEpochMillis: now - ageMinutes * 60_000, updatedAtEpochMillis: now - ageMinutes * 60_000,
    revision: 0, isDemo: true, seedMarker: "ADMIN_UI_DEMO", ...overrides};
}

function fields(value) {
  return Object.fromEntries(Object.entries(value).filter(([, item]) => item !== undefined).map(([key, item]) => [key, encode(item)]));
}

function encode(value) {
  if (value === null) return {nullValue: null};
  if (typeof value === "string") return {stringValue: value};
  if (typeof value === "boolean") return {booleanValue: value};
  if (typeof value === "number") return Number.isInteger(value) ? {integerValue: String(value)} : {doubleValue: value};
  if (Array.isArray(value)) return {arrayValue: {values: value.map(encode)}};
  if (typeof value === "object") return {mapValue: {fields: fields(value)}};
  throw new Error(`Unsupported Firestore value: ${typeof value}`);
}
