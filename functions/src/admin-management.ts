import {getAuth, UserRecord} from "firebase-admin/auth";
import {FieldValue, Timestamp} from "firebase-admin/firestore";
import {getFirestore} from "./database.js";
import {defineSecret} from "firebase-functions/params";
import {CallableRequest, HttpsError, onCall} from "firebase-functions/v2/https";
import {canonicalMobile, mobileLookupId, validPassword} from "./auth-helpers.js";

const hmacKey = defineSecret("AUTH_LOOKUP_HMAC_KEY");
const options = {region: "asia-south1", enforceAppCheck: true, timeoutSeconds: 30, memory: "256MiB" as const};

type AdminProfile = {
  displayName?: string;
  mobileNumber?: string;
  role?: string;
  shopId?: string | null;
  active?: boolean;
  createdAt?: Timestamp;
  updatedAt?: Timestamp;
};

export const superAdminListAdmins = onCall(options, async (request) => {
  await requireSuperAdmin(request);
  const snapshot = await getFirestore().collection("users").where("role", "==", "ADMIN").get();
  return {admins: snapshot.docs.map((document) => adminResponse(document.id, document.data()))};
});

export const superAdminCreateAdmin = onCall(
  {...options, secrets: [hmacKey]},
  async (request) => {
    await requireSuperAdmin(request);
    const displayName = requiredName(request.data?.displayName);
    const mobile = canonicalMobile(request.data?.mobileNumber);
    const password = request.data?.password;
    if (!mobile || !validPassword(password)) throw invalidAdmin();

    const firestore = getFirestore();
    const lookupId = mobileLookupId(mobile, hmacKey.value());
    const mappingReference = firestore.collection("authCredentials").doc(lookupId);
    if ((await mappingReference.get()).exists) throw duplicateMobile();

    const internalEmail = `admin-${lookupId.slice(0, 32)}@auth.meatbush.invalid`;
    let authUser: UserRecord | undefined;
    try {
      authUser = await getAuth().createUser({email: internalEmail, password, displayName, disabled: false});
      const userId = authUser.uid;
      await getAuth().setCustomUserClaims(userId, {role: "ADMIN"});
      const now = FieldValue.serverTimestamp();
      await firestore.runTransaction(async (transaction) => {
        if ((await transaction.get(mappingReference)).exists) throw duplicateMobile();
        transaction.create(mappingReference, {uid: userId, internalEmail, active: true, createdAt: now, updatedAt: now});
        transaction.create(firestore.collection("users").doc(userId), profile(displayName, mobile, now));
        transaction.create(firestore.collection("auditLogs").doc(), audit(request, "ADMIN_CREATED", userId));
      });
    } catch (error) {
      if (authUser) await getAuth().deleteUser(authUser.uid).catch(() => undefined);
      if (error instanceof HttpsError) throw error;
      if ((error as {code?: string}).code === "auth/email-already-exists") throw duplicateMobile();
      throw error;
    }
    if (!authUser) throw new HttpsError("internal", "Admin creation failed");
    return {admin: adminResponse(authUser.uid, {...profile(displayName, mobile), createdAt: Timestamp.now(), updatedAt: Timestamp.now()})};
  },
);

export const superAdminUpdateAdmin = onCall(
  {...options, secrets: [hmacKey]},
  async (request) => {
    await requireSuperAdmin(request);
    const userId = requiredUserId(request.data?.userId);
    const displayName = requiredName(request.data?.displayName);
    const mobile = canonicalMobile(request.data?.mobileNumber);
    if (!mobile) throw invalidAdmin();

    const firestore = getFirestore();
    const userReference = firestore.collection("users").doc(userId);
    const currentSnapshot = await userReference.get();
    const current = currentSnapshot.data() as AdminProfile | undefined;
    if (!currentSnapshot.exists || current?.role !== "ADMIN" || !current.mobileNumber) throw adminNotFound();

    const oldLookupId = mobileLookupId(current.mobileNumber, hmacKey.value());
    const newLookupId = mobileLookupId(mobile, hmacKey.value());
    const newInternalEmail = `admin-${newLookupId.slice(0, 32)}@auth.meatbush.invalid`;
    if (newLookupId !== oldLookupId && (await firestore.collection("authCredentials").doc(newLookupId).get()).exists) {
      throw duplicateMobile();
    }

    await getAuth().updateUser(userId, {displayName, email: newInternalEmail});
    const batch = firestore.batch();
    batch.update(userReference, {displayName, ...splitName(displayName), mobileNumber: mobile, updatedAt: FieldValue.serverTimestamp()});
    if (newLookupId !== oldLookupId) batch.delete(firestore.collection("authCredentials").doc(oldLookupId));
    batch.set(firestore.collection("authCredentials").doc(newLookupId), {
      uid: userId, internalEmail: newInternalEmail, active: current.active === true,
      updatedAt: FieldValue.serverTimestamp(), createdAt: FieldValue.serverTimestamp(),
    }, {merge: true});
    await batch.commit();
    return {admin: adminResponse(userId, {...current, displayName, mobileNumber: mobile, updatedAt: Timestamp.now()})};
  },
);

export const superAdminSetAdminActive = onCall(
  {...options, secrets: [hmacKey]},
  async (request) => {
    await requireSuperAdmin(request);
    const userId = requiredUserId(request.data?.userId);
    const active = request.data?.active;
    if (typeof active !== "boolean") throw invalidAdmin();
    const firestore = getFirestore();
    const userReference = firestore.collection("users").doc(userId);
    const snapshot = await userReference.get();
    const current = snapshot.data() as AdminProfile | undefined;
    if (!snapshot.exists || current?.role !== "ADMIN" || !current.mobileNumber) throw adminNotFound();

    await getAuth().updateUser(userId, {disabled: !active});
    const lookupId = mobileLookupId(current.mobileNumber, hmacKey.value());
    const batch = firestore.batch();
    batch.update(userReference, {active, updatedAt: FieldValue.serverTimestamp()});
    batch.set(firestore.collection("authCredentials").doc(lookupId), {active, updatedAt: FieldValue.serverTimestamp()}, {merge: true});
    batch.create(firestore.collection("auditLogs").doc(), audit(request, active ? "ADMIN_ACTIVATED" : "ADMIN_DISABLED", userId));
    await batch.commit();
    return {admin: adminResponse(userId, {...current, active, updatedAt: Timestamp.now()})};
  },
);

async function requireSuperAdmin(request: CallableRequest): Promise<void> {
  if (!request.auth) throw new HttpsError("unauthenticated", "Authentication required");
  const profile = await getFirestore().collection("users").doc(request.auth.uid).get();
  if (!profile.exists || profile.get("active") !== true || profile.get("role") !== "SUPER_ADMIN") {
    throw new HttpsError("permission-denied", "Super Admin access required");
  }
}

function requiredName(value: unknown): string {
  if (typeof value !== "string" || value.trim().length < 2 || value.trim().length > 80) throw invalidAdmin();
  return value.trim();
}

function requiredUserId(value: unknown): string {
  if (typeof value !== "string" || !value.trim()) throw invalidAdmin();
  return value.trim();
}

function profile(displayName: string, mobileNumber: string, timestamp: unknown = FieldValue.serverTimestamp()) {
  return {displayName, ...splitName(displayName), mobileNumber, role: "ADMIN", shopId: "default", active: true,
    createdAt: timestamp, updatedAt: timestamp, lastLoginAt: null};
}

function splitName(name: string) {
  const [firstName, ...remaining] = name.split(/\s+/);
  return {firstName, lastName: remaining.join(" ")};
}

function adminResponse(id: string, value: FirebaseFirestore.DocumentData) {
  return {id, displayName: value.displayName ?? "Admin", mobileNumber: value.mobileNumber ?? "", role: "ADMIN",
    shopId: value.shopId ?? null, active: value.active === true,
    createdAtEpochMillis: value.createdAt?.toMillis?.() ?? 0,
    updatedAtEpochMillis: value.updatedAt?.toMillis?.() ?? 0};
}

function audit(request: CallableRequest, action: string, targetUid: string) {
  return {action, actorUid: request.auth!.uid, targetUid, createdAt: FieldValue.serverTimestamp()};
}

function invalidAdmin() { return new HttpsError("invalid-argument", "Check the entered Admin details"); }
function duplicateMobile() { return new HttpsError("already-exists", "Mobile number already registered", {reason: "DUPLICATE_MOBILE"}); }
function adminNotFound() { return new HttpsError("not-found", "Admin account not found"); }
