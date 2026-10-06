import {FieldValue} from "firebase-admin/firestore";
import {getFirestore} from "./database.js";
import {CallableRequest, HttpsError, onCall} from "firebase-functions/v2/https";

const options = {region: "asia-south1", enforceAppCheck: true, timeoutSeconds: 30, memory: "256MiB" as const};
const configRef = () => getFirestore().collection("appConfig").doc("features");

const defaults = {
  deliveryStaffManagementAllowed: false,
  staffManagementAllowed: false,
  subcategoriesAllowed: false,
  realtimeTrackingAllowed: false,
  codAllowed: true,
  razorpayAllowed: false,
  upiAllowed: false,
  offersAllowed: false,
  couponsAllowed: false,
  scheduledDeliveryAllowed: true,
  inAppNotificationsEnabled: true,
  maxProducts: 100,
  revision: 0,
};

export const getAllowedFeatureConfig = onCall(options, async (request) => {
  // The feature availability bit is public; updates remain Super Admin-only.
  const snapshot = await configRef().get();
  return {config: {...defaults, ...snapshot.data(), codAllowed: true, scheduledDeliveryAllowed: true}};
});

export const superAdminUpdateFeatureConfig = onCall(options, async (request) => {
  await requireSuperAdmin(request);
  const next = validatedConfig(request.data);
  const firestore = getFirestore();
  const updated = await firestore.runTransaction(async (transaction) => {
    const snapshot = await transaction.get(configRef());
    const currentRevision = Number(snapshot.get("revision") ?? 0);
    if (Number(request.data?.expectedRevision ?? 0) !== currentRevision) {
      throw new HttpsError("aborted", "Feature configuration changed elsewhere");
    }
    const config = {...next, revision: currentRevision + 1};
    transaction.set(configRef(), {...config, updatedAt: FieldValue.serverTimestamp(), updatedBy: request.auth!.uid});
    transaction.set(firestore.collection("appConfig").doc("publicCustomerFeatures"), {
      inAppNotificationsEnabled: next.inAppNotificationsEnabled,
      revision: config.revision,
    });
    transaction.create(firestore.collection("auditLogs").doc(), {
      action: "FEATURE_CONFIG_CHANGED", actorUid: request.auth!.uid,
      targetUid: null, createdAt: FieldValue.serverTimestamp(),
    });
    return config;
  });
  return {config: updated};
});

async function requireActiveUser(request: CallableRequest): Promise<void> {
  if (!request.auth) throw new HttpsError("unauthenticated", "Authentication required");
  const profile = await getFirestore().collection("users").doc(request.auth.uid).get();
  if (!profile.exists || profile.get("active") !== true) {
    throw new HttpsError("permission-denied", "Active account required");
  }
}

async function requireSuperAdmin(request: CallableRequest): Promise<void> {
  await requireActiveUser(request);
  const profile = await getFirestore().collection("users").doc(request.auth!.uid).get();
  if (profile.get("role") !== "SUPER_ADMIN") {
    throw new HttpsError("permission-denied", "Super Admin access required");
  }
}

function validatedConfig(data: Record<string, unknown> | undefined) {
  if (!data) throw new HttpsError("invalid-argument", "Feature configuration required");
  const booleanKeys = [
    "deliveryStaffManagementAllowed", "realtimeTrackingAllowed", "codAllowed", "razorpayAllowed",
    "upiAllowed", "offersAllowed", "couponsAllowed", "scheduledDeliveryAllowed",
    "inAppNotificationsEnabled", "staffManagementAllowed", "subcategoriesAllowed",
  ] as const;
  const result: Record<string, boolean | number> = {};
  for (const key of booleanKeys) {
    if (typeof data[key] !== "boolean") throw new HttpsError("invalid-argument", `Invalid ${key}`);
    result[key] = data[key] as boolean;
  }
  const maximum = Number(data.maxProducts);
  if (!Number.isInteger(maximum) || maximum < 0 || maximum > 100000) {
    throw new HttpsError("invalid-argument", "Invalid maximum products");
  }
  result.codAllowed = true;
  result.scheduledDeliveryAllowed = true;
  result.maxProducts = maximum;
  return result;
}
