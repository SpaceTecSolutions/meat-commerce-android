import {getAuth} from "firebase-admin/auth";
import {FieldValue} from "firebase-admin/firestore";
import {getFirestore} from "./database.js";
import {defineSecret} from "firebase-functions/params";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {validPassword} from "./auth-helpers.js";
import {catalogOptions, requireAdmin, shop, text} from "./catalog-common.js";
import {verifyFirebasePassword} from "./identity-password.js";

const webApiKey = defineSecret("IDENTITY_TOOLKIT_API_KEY");
const settingsOptions = {...catalogOptions, invoker: "public" as const};

export const adminGetShopSettings = onCall(settingsOptions, async (request) => {
  const context = await requireAdmin(request);
  const [profile, shopSnapshot] = await Promise.all([
    getFirestore().collection("users").doc(context.uid).get(), shop(context).get(),
  ]);
  return {settings: settingsResponse(context.shopId, profile.data() ?? {}, shopSnapshot.data() ?? {})};
});

export const adminUpdateProfile = onCall(settingsOptions, async (request) => {
  const context = await requireAdmin(request);
  const firstName = boundedName(request.data?.firstName, "First name");
  const lastName = boundedName(request.data?.lastName, "Last name", true);
  await getFirestore().runTransaction(async (transaction) => {
    const reference = getFirestore().collection("users").doc(context.uid);
    const current = await transaction.get(reference);
    if (!current.exists || current.get("active") !== true || current.get("role") !== "ADMIN") denied();
    transaction.update(reference, {
      firstName, lastName, displayName: [firstName, lastName].filter(Boolean).join(" "),
      updatedAt: FieldValue.serverTimestamp(),
    });
    transaction.create(shop(context).collection("auditLogs").doc(), {
      action: "ADMIN_PROFILE_UPDATED", actorUid: context.uid, createdAt: FieldValue.serverTimestamp(),
    });
  });
  return {success: true};
});

export const adminSaveShopSettings = onCall(settingsOptions, async (request) => {
  const context = await requireAdmin(request);
  const reference = shop(context); const expected = numberValue(request.data?.expectedRevision, -1);
  if (expected < 0) invalid("Settings revision required");
  const section = text(request.data?.section, "SHOP").toUpperCase();
  const result = await getFirestore().runTransaction(async (transaction) => {
    const [current, profile] = await Promise.all([
      transaction.get(reference), transaction.get(getFirestore().collection("users").doc(context.uid)),
    ]);
    const revision = numberValue(current.get("revision"));
    if (revision !== expected) throw new HttpsError("aborted", "Settings changed");
    const bannerAllowed = current.get("bannerEditingAllowed") === true;
    const sectionUpdate = settingsSectionInput(section, request.data, bannerAllowed);
    const update = {...sectionUpdate, revision: revision + 1,
      updatedAt: FieldValue.serverTimestamp(), updatedBy: context.uid};
    transaction.set(reference, update, {merge: true});
    return settingsResponse(context.shopId, profile.data() ?? {},
      {...current.data(), ...update, bannerEditingAllowed: bannerAllowed});
  });
  return {settings: result};
});

export const adminChangePassword = onCall({...settingsOptions, secrets: [webApiKey]}, async (request) => {
  const context = await requireAdmin(request);
  const currentPassword = request.data?.currentPassword; const newPassword = request.data?.newPassword;
  if (!validPassword(currentPassword) || !strongPassword(newPassword)) invalid("Check password requirements");
  const authUser = await getAuth().getUser(context.uid);
  if (!authUser.email) throw new HttpsError("failed-precondition", "Account credentials are unavailable");
  const verification = await verifyFirebasePassword(webApiKey.value(), authUser.email, currentPassword);
  if (verification.status !== "valid" || verification.uid !== context.uid) {
    throw new HttpsError("unauthenticated", "Current password is incorrect");
  }
  await getAuth().updateUser(context.uid, {password: newPassword});
  await shop(context).collection("auditLogs").add({
    action: "ADMIN_PASSWORD_CHANGED", actorUid: context.uid, createdAt: FieldValue.serverTimestamp(),
  });
  return {success: true};
});

function settingsResponse(shopId: string, profile: FirebaseFirestore.DocumentData,
  value: FirebaseFirestore.DocumentData) {
  return {shopId, shopName: text(value.shopName, text(value.displayName)), address: text(value.address),
    contactPhone: text(value.contactPhone), contactEmail: nullable(value.contactEmail),
    supportPhone: nullable(value.supportPhone), supportWhatsApp: nullable(value.supportWhatsApp),
    supportEmail: nullable(value.supportEmail),
    bannerTitle: nullable(value.bannerTitle), bannerMessage: nullable(value.bannerMessage),
    bannerEnabled: value.bannerEnabled === true, bannerEditingAllowed: value.bannerEditingAllowed === true,
    profileName: text(profile.displayName), profileMobile: text(profile.mobileNumber),
    revision: numberValue(value.revision)};
}

function shopInput(data: any) {
  const shopName = required(data?.shopName, "Shop name", 100); const address = required(data?.address, "Address", 300);
  const contactPhone = required(data?.contactPhone, "Contact phone", 20);
  return {shopName, displayName: shopName, address, contactPhone,
    contactEmail: optionalEmail(data?.contactEmail)};
}
function settingsSectionInput(section: string, data: any, bannerAllowed: boolean) {
  switch (section) {
  case "SHOP": return shopInput(data);
  case "APP":
    if (!bannerAllowed) throw new HttpsError("failed-precondition", "Banner editing is not authorized");
    return {bannerTitle: optional(data?.bannerTitle, 80), bannerMessage: optional(data?.bannerMessage, 240),
      bannerEnabled: data?.bannerEnabled === true};
  case "SUPPORT": {
    const supportPhone = optionalPhone(data?.supportPhone, "call number");
    const supportWhatsApp = optionalPhone(data?.supportWhatsApp, "WhatsApp number");
    const supportEmail = optionalEmail(data?.supportEmail);
    if (!supportPhone && !supportWhatsApp && !supportEmail) invalid("Provide at least one support method");
    return {supportPhone, supportWhatsApp, supportEmail};
  }
  default: return invalid("Invalid settings section");
  }
}
function boundedName(value: unknown, label: string, optionalValue = false) {
  const result = text(value).trim().replace(/\s+/g, " ");
  if ((!optionalValue && result.length < 2) || result.length > 50) invalid(`Invalid ${label}`);
  return result;
}
function required(value: unknown, label: string, max: number) {
  const result = text(value).trim(); if (!result || result.length > max) invalid(`Invalid ${label}`); return result;
}
function optional(value: unknown, max: number) { const result = text(value).trim(); return result ? result.slice(0, max) : null; }
function optionalPhone(value: unknown, label: string) {
  const result = optional(value, 20); const digits = result?.replace(/\D/g, "") ?? "";
  if (result && (digits.length < 10 || digits.length > 15)) invalid(`Invalid ${label}`);
  return result;
}
function optionalEmail(value: unknown) { const result = optional(value, 120); if (result && !/^\S+@\S+\.\S+$/.test(result)) invalid("Invalid email"); return result; }
function nullable(value: unknown) { return typeof value === "string" && value.trim() ? value.trim() : null; }
function numberValue(value: unknown, fallback = 0) { const parsed = Number(value); return Number.isInteger(parsed) ? parsed : fallback; }
function strongPassword(value: unknown): value is string { return validPassword(value) && /[A-Z]/.test(value) && /[a-z]/.test(value) && /\d/.test(value); }
function invalid(message: string): never { throw new HttpsError("invalid-argument", message); }
function denied(): never { throw new HttpsError("permission-denied", "Active Admin access required"); }
