import {randomBytes} from "node:crypto";
import {getAuth} from "firebase-admin/auth";
import {FieldValue} from "firebase-admin/firestore";
import {getFirestore} from "./database.js";
import {defineSecret} from "firebase-functions/params";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {canonicalMobile, mobileLookupId, validPassword} from "./auth-helpers.js";

const hmacKey = defineSecret("AUTH_LOOKUP_HMAC_KEY");
const options = {region: "asia-south1", enforceAppCheck: true, invoker: "public" as const,
  secrets: [hmacKey], timeoutSeconds: 30, memory: "256MiB" as const, maxInstances: 20};
const TOKEN_LIFETIME_MS = 10 * 60_000;

type PhoneProof = {uid: string; mobile: string};
type FlowToken = {purpose?: string; mobile?: string; uid?: string; expiresAtMillis?: number; consumed?: boolean};

export const authVerifyCustomerRegistration = onCall(options, async (request) => {
  const proof = await verifiedPhone(request.data?.phoneVerificationIdToken);
  const firestore = getFirestore(); const lookupId = mobileLookupId(proof.mobile, hmacKey.value());
  const mapping = await firestore.collection("authCredentials").doc(lookupId).get();
  if (mapping.exists) {
    const uid = string(mapping.get("uid"));
    await releasePhoneProof(proof, uid);
    return {verificationToken: "", accountExists: true};
  }
  await releasePhoneProof(proof);
  return {verificationToken: await issueToken("REGISTER", proof.mobile), accountExists: false};
});

// Phone possession is sufficient for sign-in. The client never chooses a role: the
// trusted active profile is the only source used for the session's role and shop claim.
export const authSignInCustomerWithPhone = onCall(options, async (request) => {
  const proof = await verifiedPhone(request.data?.phoneVerificationIdToken);
  const firestore = getFirestore();
  const lookupId = mobileLookupId(proof.mobile, hmacKey.value());
  const mappingRef = firestore.collection("authCredentials").doc(lookupId);
  const mapping = await mappingRef.get();
  if (mapping.exists) {
    const uid = string(mapping.get("uid"));
    if (!uid) {
      await releasePhoneProof(proof);
      disabledAccount();
    }
    const [profile, authUser] = await Promise.all([
      firestore.collection("users").doc(uid).get(),
      getAuth().getUser(uid).catch(() => null),
    ]);
    const role = string(profile.get("role"));
    if (!uid || mapping.get("active") !== true || !profile.exists || profile.get("active") !== true ||
        !authUser || authUser.disabled || !supportsOtpSignInRole(role)) {
      await releasePhoneProof(proof, uid);
      disabledAccount();
    }
    await releasePhoneProof(proof, uid);
    await firestore.collection("users").doc(uid).update({lastLoginAt: FieldValue.serverTimestamp()});
    const shopId = string(profile.get("shopId"));
    return {customToken: await getAuth().createCustomToken(uid, {
      role,
      ...(shopId ? {shopId} : role === "CUSTOMER" ? {shopId: "default"} : {}),
    })};
  }
  const existingProofProfile = await firestore.collection("users").doc(proof.uid).get();
  if (existingProofProfile.exists) {
    // Staff created through the trusted Admin callable already owns this Firebase Auth phone UID,
    // but older records may predate the private mobile lookup document. Repair that linkage only
    // when the verified phone, Auth UID and trusted profile all identify the same active account.
    const role = string(existingProofProfile.get("role"));
    const profileMobile = canonicalMobile(existingProofProfile.get("mobileNumber"));
    const authUser = await getAuth().getUser(proof.uid).catch(() => null);
    if (!authUser || authUser.disabled || existingProofProfile.get("active") !== true ||
        profileMobile !== proof.mobile || !supportsOtpSignInRole(role)) {
      throw new HttpsError("permission-denied", "This phone cannot create a Customer account");
    }
    const shopId = string(existingProofProfile.get("shopId"));
    const permissions = Array.isArray(existingProofProfile.get("permissions")) ?
      existingProofProfile.get("permissions") as string[] : [];
    await firestore.runTransaction(async (transaction) => {
      const current = await transaction.get(mappingRef);
      if (!current.exists) {
        const now = FieldValue.serverTimestamp();
        transaction.create(mappingRef, {uid: proof.uid, active: true, createdAt: now, updatedAt: now});
      } else if (string(current.get("uid")) !== proof.uid) duplicateAccount();
      transaction.update(existingProofProfile.ref, {lastLoginAt: FieldValue.serverTimestamp()});
    });
    await getAuth().setCustomUserClaims(proof.uid, {role, ...(shopId ? {shopId} : {}), permissions});
    return {customToken: await getAuth().createCustomToken(proof.uid, {
      role, ...(shopId ? {shopId} : {}), permissions,
    })};
  }
  await getAuth().setCustomUserClaims(proof.uid, {role: "CUSTOMER", shopId: "default"});
  try {
    await firestore.runTransaction(async (transaction) => {
      const current = await transaction.get(mappingRef);
      if (current.exists) duplicateAccount();
      const now = FieldValue.serverTimestamp();
      transaction.create(mappingRef, {uid: proof.uid, active: true, createdAt: now, updatedAt: now});
      transaction.create(firestore.collection("users").doc(proof.uid), {
        firstName: "", lastName: "", displayName: "Customer", mobileNumber: proof.mobile,
        role: "CUSTOMER", shopId: "default", active: true, revision: 0,
        createdAt: now, updatedAt: now, lastLoginAt: now,
      });
    });
  } catch (error) {
    if (error instanceof HttpsError) throw error;
    throw new HttpsError("internal", "Customer sign-in failed");
  }
  return {customToken: await getAuth().createCustomToken(proof.uid, {role: "CUSTOMER", shopId: "default"})};
});

export const authRegisterCustomer = onCall(options, async (request) => {
  const firstName = validName(request.data?.firstName); const lastName = validName(request.data?.lastName);
  const suppliedMobile = canonicalMobile(request.data?.mobileNumber); const password = request.data?.password;
  const rawToken = string(request.data?.verificationToken);
  if (!strongPassword(password) || !rawToken) invalidRegistration();
  const tokenReference = tokenDocument(rawToken); const token = await tokenReference.get();
  const mobile = canonicalMobile(token.get("mobile"));
  if (!mobile || (suppliedMobile && suppliedMobile !== mobile)) invalidRegistration();
  requireToken(token.data(), "REGISTER", mobile);
  const firestore = getFirestore(); const lookupId = mobileLookupId(mobile, hmacKey.value());
  const mappingReference = firestore.collection("authCredentials").doc(lookupId);
  if ((await mappingReference.get()).exists) duplicateAccount();
  const internalEmail = `customer-${lookupId.slice(0, 32)}@auth.meatbush.invalid`;
  let createdUid: string | undefined;
  let profileCommitted = false;
  try {
    const displayName = `${firstName} ${lastName}`.trim();
    const user = await getAuth().createUser({email: internalEmail, password, phoneNumber: mobile,
      displayName, disabled: false});
    createdUid = user.uid;
    await getAuth().setCustomUserClaims(user.uid, {role: "CUSTOMER", shopId: "default"});
    await firestore.runTransaction(async (transaction) => {
      const [freshToken, freshMapping] = await Promise.all([
        transaction.get(tokenReference), transaction.get(mappingReference),
      ]);
      requireToken(freshToken.data(), "REGISTER", mobile);
      if (freshMapping.exists) duplicateAccount();
      const now = FieldValue.serverTimestamp();
      transaction.update(tokenReference, {consumed: true, consumedAt: now});
      transaction.create(mappingReference, {uid: user.uid, internalEmail, active: true, createdAt: now, updatedAt: now});
      transaction.create(firestore.collection("users").doc(user.uid), {
        firstName, lastName, displayName, mobileNumber: mobile, role: "CUSTOMER", shopId: "default",
        active: true, revision: 0, createdAt: now, updatedAt: now, lastLoginAt: now,
      });
    });
    profileCommitted = true;
    return {customToken: await getAuth().createCustomToken(user.uid, {role: "CUSTOMER", shopId: "default"})};
  } catch (error) {
    if (createdUid && !profileCommitted) await getAuth().deleteUser(createdUid).catch(() => undefined);
    if (error instanceof HttpsError) throw error;
    const code = (error as {code?: string}).code;
    if (code === "auth/email-already-exists" || code === "auth/phone-number-already-exists") duplicateAccount();
    throw new HttpsError("internal", "Customer registration failed");
  }
});

export const authVerifyPasswordResetCode = onCall(options, async (request) => {
  const proof = await verifiedPhone(request.data?.phoneVerificationIdToken);
  const mapping = await getFirestore().collection("authCredentials")
    .doc(mobileLookupId(proof.mobile, hmacKey.value())).get();
  const uid = string(mapping.get("uid"));
  if (!mapping.exists || !uid) {
    await releasePhoneProof(proof);
    throw new HttpsError("not-found", "Account not found", {reason: "ACCOUNT_NOT_FOUND"});
  }
  if (mapping.get("active") !== true) disabledAccount();
  await releasePhoneProof(proof, uid);
  return {resetToken: await issueToken("RESET", proof.mobile, uid)};
});

export const authResetPassword = onCall(options, async (request) => {
  const rawToken = string(request.data?.resetToken); const password = request.data?.newPassword;
  if (!rawToken || !strongPassword(password)) invalidPassword();
  const reference = tokenDocument(rawToken); const firestore = getFirestore();
  const token = await firestore.runTransaction(async (transaction) => {
    const snapshot = await transaction.get(reference); const value = snapshot.data() as FlowToken | undefined;
    requireToken(value, "RESET");
    transaction.update(reference, {consumed: true, consumedAt: FieldValue.serverTimestamp()});
    return value!;
  });
  const uid = string(token.uid); if (!uid) throw invalidResetToken();
  const profile = await firestore.collection("users").doc(uid).get();
  if (!profile.exists || profile.get("active") !== true) disabledAccount();
  await getAuth().updateUser(uid, {password});
  await getAuth().revokeRefreshTokens(uid);
  return {};
});

async function verifiedPhone(value: unknown): Promise<PhoneProof> {
  const token = string(value); if (!token) throw invalidCode();
  try {
    const decoded = await getAuth().verifyIdToken(token, true);
    const mobile = canonicalMobile(decoded.phone_number);
    if (!mobile || decoded.firebase?.sign_in_provider !== "phone") throw invalidCode();
    return {uid: decoded.uid, mobile};
  } catch (error) {
    if (error instanceof HttpsError) throw error;
    throw invalidCode();
  }
}

async function releasePhoneProof(proof: PhoneProof, targetUid?: string) {
  if (targetUid === proof.uid) return;
  const auth = getAuth();
  await auth.deleteUser(proof.uid).catch((error) => {
    if ((error as {code?: string}).code !== "auth/user-not-found") throw error;
  });
  if (targetUid) await auth.updateUser(targetUid, {phoneNumber: proof.mobile});
}

async function issueToken(purpose: "REGISTER" | "RESET", mobile: string, uid?: string) {
  const raw = randomBytes(32).toString("base64url");
  await tokenDocument(raw).set({purpose, mobile, ...(uid ? {uid} : {}), consumed: false,
    expiresAtMillis: Date.now() + TOKEN_LIFETIME_MS, createdAt: FieldValue.serverTimestamp()});
  return raw;
}

function tokenDocument(raw: string) {
  return getFirestore().collection("authFlowTokens").doc(mobileLookupId(raw, hmacKey.value()));
}
function requireToken(value: FlowToken | undefined, purpose: string, mobile?: string) {
  if (!value || value.purpose !== purpose || value.consumed === true ||
      (value.expiresAtMillis ?? 0) < Date.now() || (mobile && value.mobile !== mobile)) throw invalidResetToken();
}
export function strongPassword(value: unknown): value is string {
  return validPassword(value) && /[A-Z]/.test(value) && /[a-z]/.test(value) && /\d/.test(value);
}
function validName(value: unknown): string {
  const result = string(value).trim(); if (result.length < 2 || result.length > 50) invalidRegistration(); return result;
}
function string(value: unknown) { return typeof value === "string" ? value : ""; }
const OTP_SIGN_IN_ROLES = new Set(["CUSTOMER", "ADMIN", "SUPER_ADMIN", "STAFF"]);
export function supportsOtpSignInRole(value: unknown): boolean {
  return typeof value === "string" && OTP_SIGN_IN_ROLES.has(value);
}
function invalidCode() { return new HttpsError("invalid-argument", "Invalid or expired verification code",
  {reason: "INVALID_VERIFICATION_CODE"}); }
function invalidRegistration(): never { throw new HttpsError("invalid-argument", "Check registration details",
  {reason: "INVALID_REGISTRATION"}); }
function invalidPassword(): never { throw new HttpsError("invalid-argument", "Password does not meet security requirements",
  {reason: "INVALID_PASSWORD"}); }
function invalidResetToken() { return new HttpsError("failed-precondition", "Verification expired", {reason: "RESET_EXPIRED"}); }
function duplicateAccount(): never { throw new HttpsError("already-exists", "Account already exists", {reason: "DUPLICATE_ACCOUNT"}); }
function disabledAccount(): never { throw new HttpsError("permission-denied", "Account disabled", {reason: "USER_DISABLED"}); }
