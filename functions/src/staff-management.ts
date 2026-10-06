import {getAuth} from "firebase-admin/auth";
import {FieldValue, Timestamp} from "firebase-admin/firestore";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {defineSecret} from "firebase-functions/params";
import {catalogOptions, requireAdmin, text} from "./catalog-common.js";
import {getFirestore} from "./database.js";
import {mobileLookupId} from "./auth-helpers.js";

const allowed = new Set(["VIEW_ORDERS", "UPDATE_ORDER_STATUS", "ADJUST_FINAL_BILL", "GENERATE_INVOICE",
  "MANAGE_PRODUCTS", "MANAGE_STOCK", "MANAGE_FAQ"]);
const hmacKey = defineSecret("AUTH_LOOKUP_HMAC_KEY");
const options = {...catalogOptions, secrets: [hmacKey]};
async function requireEnabled() {
  if ((await getFirestore().collection("appConfig").doc("features").get()).get("staffManagementAllowed") !== true)
    throw new HttpsError("failed-precondition", "Staff Management is disabled", {reason: "FEATURE_DISABLED"});
}
function permissions(data: any): string[] {
  if (!Array.isArray(data)) return [];
  const value = [...new Set(data.filter((item): item is string => typeof item === "string" && allowed.has(item)))];
  if (value.length !== data.length) throw new HttpsError("invalid-argument", "Invalid staff permission");
  return value;
}
function millis(value: unknown) { return value instanceof Timestamp ? value.toMillis() : 0; }
function response(id: string, value: FirebaseFirestore.DocumentData) {
  return {id, role: "STAFF", displayName: text(value.displayName), mobileNumber: text(value.mobileNumber),
    email: text(value.email) || null, shopId: text(value.shopId, "default"), active: value.active === true,
    permissions: permissions(value.permissions), createdAtEpochMillis: millis(value.createdAt),
    updatedAtEpochMillis: millis(value.updatedAt)};
}
function input(data: any) {
  const displayName = text(data?.displayName).trim(); const digits = text(data?.mobileNumber).replace(/\D/g, "");
  const mobileNumber = digits.length === 10 ? `+91${digits}` : `+${digits}`; const email = text(data?.email).trim().toLowerCase();
  if (displayName.length < 2 || displayName.length > 80 || !/^\+91[6-9]\d{9}$/.test(mobileNumber) ||
      (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email))) throw new HttpsError("invalid-argument", "Invalid staff details");
  return {displayName, mobileNumber, email: email || null, active: data?.active !== false, permissions: permissions(data?.permissions)};
}
export const adminListStaff = onCall(options, async (request) => {
  const context = await requireAdmin(request); await requireEnabled();
  const snapshot = await getFirestore().collection("users").where("shopId", "==", context.shopId)
    .where("role", "==", "STAFF").limit(200).get();
  return {staff: snapshot.docs.map((doc) => response(doc.id, doc.data()))};
});
export const adminCreateStaff = onCall(options, async (request) => {
  const context = await requireAdmin(request); await requireEnabled(); const value = input(request.data);
  let existing = null; try { existing = await getAuth().getUserByPhoneNumber(value.mobileNumber); } catch (error: any) {
    if (error?.code !== "auth/user-not-found") throw error;
  }
  if (existing) throw new HttpsError("already-exists", "Mobile number is already registered");
  const auth = await getAuth().createUser({phoneNumber: value.mobileNumber, displayName: value.displayName,
    email: value.email ?? undefined, disabled: !value.active});
  const profile = {...value, role: "STAFF", shopId: context.shopId, createdBy: context.uid,
    createdAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp()};
  try {
    const firestore = getFirestore(); const lookup = firestore.collection("authCredentials")
      .doc(mobileLookupId(value.mobileNumber, hmacKey.value()));
    await firestore.runTransaction(async (transaction) => {
      if ((await transaction.get(lookup)).exists) throw new HttpsError("already-exists", "Mobile number is already registered");
      const now = FieldValue.serverTimestamp();
      transaction.create(firestore.collection("users").doc(auth.uid), profile);
      transaction.create(lookup, {uid: auth.uid, active: value.active, createdAt: now, updatedAt: now});
    });
    await getAuth().setCustomUserClaims(auth.uid, {role: "STAFF", shopId: context.shopId, permissions: value.permissions});
  } catch (error) { await getAuth().deleteUser(auth.uid).catch(() => undefined); throw error; }
  return {staff: response(auth.uid, profile)};
});
export const adminUpdateStaff = onCall(options, async (request) => {
  const context = await requireAdmin(request); await requireEnabled(); const value = input(request.data);
  const userId = text(request.data?.userId); if (!userId) throw new HttpsError("invalid-argument", "Staff required");
  const reference = getFirestore().collection("users").doc(userId); const previous = await reference.get();
  if (!previous.exists || previous.get("role") !== "STAFF" || text(previous.get("shopId"), "default") !== context.shopId)
    throw new HttpsError("not-found", "Staff not found");
  const oldMobile = text(previous.get("mobileNumber")); const firestore = getFirestore();
  const oldLookup = firestore.collection("authCredentials").doc(mobileLookupId(oldMobile, hmacKey.value()));
  const newLookup = firestore.collection("authCredentials").doc(mobileLookupId(value.mobileNumber, hmacKey.value()));
  const conflicting = await newLookup.get();
  if (conflicting.exists && text(conflicting.get("uid")) !== userId)
    throw new HttpsError("already-exists", "Mobile number is already registered");
  await getAuth().updateUser(userId, {phoneNumber: value.mobileNumber, displayName: value.displayName,
    email: value.email ?? undefined, disabled: !value.active});
  try {
    await firestore.runTransaction(async (transaction) => {
      const now = FieldValue.serverTimestamp();
      transaction.update(reference, {...value, updatedAt: now, updatedBy: context.uid});
      if (oldLookup.path !== newLookup.path) transaction.delete(oldLookup);
      transaction.set(newLookup, {uid: userId, active: value.active,
        createdAt: conflicting.get("createdAt") ?? now, updatedAt: now}, {merge: true});
    });
  } catch (error) {
    await getAuth().updateUser(userId, {phoneNumber: oldMobile}).catch(() => undefined); throw error;
  }
  await getAuth().setCustomUserClaims(userId, {role: "STAFF", shopId: context.shopId, permissions: value.permissions});
  return {staff: response(userId, {...previous.data(), ...value})};
});
