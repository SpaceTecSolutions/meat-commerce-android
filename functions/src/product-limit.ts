import {FieldValue} from "firebase-admin/firestore";
import {getFirestore} from "./database.js";
import {CallableRequest, HttpsError, onCall} from "firebase-functions/v2/https";

const options = {region: "asia-south1", enforceAppCheck: true, timeoutSeconds: 30, memory: "256MiB" as const};
const configRef = () => getFirestore().collection("appConfig").doc("features");

export const superAdminGetProductLimitStatus = onCall(options, async (request) => {
  await requireSuperAdmin(request);
  const [config, countedProducts] = await Promise.all([configRef().get(), countNonArchivedProducts()]);
  return {status: {
    countedProducts,
    maxProducts: numberField(config.get("maxProducts"), 100),
    revision: numberField(config.get("revision"), 0),
  }};
});

export const superAdminUpdateProductLimit = onCall(options, async (request) => {
  await requireSuperAdmin(request);
  const maxProducts = Number(request.data?.maxProducts);
  const expectedRevision = Number(request.data?.expectedRevision);
  if (!Number.isInteger(maxProducts) || maxProducts < 0 || maxProducts > 100000) {
    throw new HttpsError("invalid-argument", "Invalid maximum products");
  }
  const countedProducts = await countNonArchivedProducts();
  if (maxProducts < countedProducts) {
    throw new HttpsError("failed-precondition", "Limit is below the current product count", {
      reason: "LIMIT_BELOW_CURRENT_COUNT",
    });
  }
  const firestore = getFirestore();
  const revision = await firestore.runTransaction(async (transaction) => {
    const snapshot = await transaction.get(configRef());
    const currentRevision = numberField(snapshot.get("revision"), 0);
    if (expectedRevision !== currentRevision) {
      throw new HttpsError("aborted", "Product configuration changed elsewhere");
    }
    const nextRevision = currentRevision + 1;
    transaction.set(configRef(), {
      maxProducts, revision: nextRevision, updatedAt: FieldValue.serverTimestamp(), updatedBy: request.auth!.uid,
    }, {merge: true});
    transaction.create(firestore.collection("auditLogs").doc(), {
      action: "PRODUCT_LIMIT_CHANGED", actorUid: request.auth!.uid, targetUid: null,
      maxProducts, createdAt: FieldValue.serverTimestamp(),
    });
    return nextRevision;
  });
  return {status: {countedProducts, maxProducts, revision}};
});

async function countNonArchivedProducts(): Promise<number> {
  const products = getFirestore().collectionGroup("products");
  const [all, archived] = await Promise.all([
    products.count().get(),
    products.where("archived", "==", true).count().get(),
  ]);
  return Math.max(0, all.data().count - archived.data().count);
}

async function requireSuperAdmin(request: CallableRequest): Promise<void> {
  if (!request.auth) throw new HttpsError("unauthenticated", "Authentication required");
  const profile = await getFirestore().collection("users").doc(request.auth.uid).get();
  if (!profile.exists || profile.get("active") !== true || profile.get("role") !== "SUPER_ADMIN") {
    throw new HttpsError("permission-denied", "Super Admin access required");
  }
}

function numberField(value: unknown, fallback: number): number {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}
