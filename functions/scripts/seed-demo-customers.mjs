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
const customers = [
  customer("demo-9876543210", "Rahul", "Sharma", "+919876543210", 36),
  customer("demo-9123456780", "Priya", "Mehta", "+919123456780", 24),
  customer("demo-9789012345", "Amit", "Verma", "+919789012345", 12),
];

const writes = customers.map(({id, ...data}) => ({update: {
  name: `projects/${projectId}/databases/default/documents/users/${id}`,
  fields: fields(data),
}}));
const endpoint = `https://firestore.googleapis.com/v1/projects/${projectId}/databases/default/documents:commit`;
const response = await fetch(endpoint, {
  method: "POST",
  headers: {Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json"},
  body: JSON.stringify({writes}),
});
if (!response.ok) throw new Error(`Customer seed failed (${response.status}): ${await response.text()}`);
console.log(`Seeded ${customers.length} non-login demo customers into shop '${shopId}'.`);

function customer(id, firstName, lastName, mobileNumber, ageHours) {
  return {id, firstName, lastName, displayName: `${firstName} ${lastName}`, mobileNumber,
    role: "CUSTOMER", active: true, shopId, revision: 0, isDemo: true,
    seedMarker: "ADMIN_CUSTOMER_UI_DEMO", createdAt: new Date(now - ageHours * 60 * 60_000),
    updatedAt: new Date(now)};
}
function fields(value) {
  return Object.fromEntries(Object.entries(value).map(([key, item]) => [key, encode(item)]));
}
function encode(value) {
  if (value instanceof Date) return {timestampValue: value.toISOString()};
  if (value === null) return {nullValue: null};
  if (typeof value === "string") return {stringValue: value};
  if (typeof value === "boolean") return {booleanValue: value};
  if (typeof value === "number") return {integerValue: String(value)};
  throw new Error(`Unsupported Firestore seed value: ${typeof value}`);
}
