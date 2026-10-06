import {randomUUID} from "node:crypto";
import {FieldValue, Timestamp} from "firebase-admin/firestore";
import {getFirestore} from "./database.js";
import {getStorage} from "firebase-admin/storage";
import {CallableRequest, HttpsError, onCall} from "firebase-functions/v2/https";

export const catalogOptions = {
  region: "asia-south1", enforceAppCheck: true, timeoutSeconds: 45, memory: "256MiB" as const,
};
export type AdminContext = {uid: string; shopId: string};
export type CustomerContext = {uid: string; shopId: string};

export async function requireAdmin(request: CallableRequest): Promise<AdminContext> {
  if (!request.auth) throw new HttpsError("unauthenticated", "Authentication required");
  const reference = getFirestore().collection("users").doc(request.auth.uid);
  const profile = await reference.get();
  if (!profile.exists || profile.get("active") !== true || profile.get("role") !== "ADMIN") {
    throw new HttpsError("permission-denied", "Active Admin access required");
  }
  const shopId = text(profile.get("shopId"), "default");
  if (!profile.get("shopId")) await reference.update({shopId, updatedAt: FieldValue.serverTimestamp()});
  return {uid: request.auth.uid, shopId};
}

export async function requireAdminOrStaff(request: CallableRequest, permission: string): Promise<AdminContext> {
  if (!request.auth) throw new HttpsError("unauthenticated", "Authentication required");
  const profile = await getFirestore().collection("users").doc(request.auth.uid).get();
  const role = profile.get("role");
  const permissions = Array.isArray(profile.get("permissions")) ? profile.get("permissions") as string[] : [];
  if (!profile.exists || profile.get("active") !== true ||
      (role !== "ADMIN" && !(role === "STAFF" && permissions.includes(permission)))) {
    throw new HttpsError("permission-denied", "Permission required");
  }
  return {uid: request.auth.uid, shopId: text(profile.get("shopId"), "default")};
}

export async function requireAdminOrStaffAny(
  request: CallableRequest,
  requiredPermissions: string[],
): Promise<AdminContext> {
  if (!request.auth) throw new HttpsError("unauthenticated", "Authentication required");
  const profile = await getFirestore().collection("users").doc(request.auth.uid).get();
  const role = profile.get("role");
  const permissions = Array.isArray(profile.get("permissions")) ? profile.get("permissions") as string[] : [];
  const hasPermission = requiredPermissions.some((permission) => permissions.includes(permission));
  if (!profile.exists || profile.get("active") !== true ||
      (role !== "ADMIN" && !(role === "STAFF" && hasPermission))) {
    throw new HttpsError("permission-denied", "Permission required");
  }
  return {uid: request.auth.uid, shopId: text(profile.get("shopId"), "default")};
}

export async function requireCustomer(request: CallableRequest): Promise<CustomerContext> {
  if (!request.auth) throw new HttpsError("unauthenticated", "Authentication required");
  const profile = await getFirestore().collection("users").doc(request.auth.uid).get();
  if (!profile.exists || profile.get("active") !== true || profile.get("role") !== "CUSTOMER" ||
      profile.get("deletionPending") === true) {
    throw new HttpsError("permission-denied", "Active Customer access required");
  }
  return {uid: request.auth.uid, shopId: text(profile.get("shopId"), "default")};
}

export async function publicCustomerShop(request: CallableRequest): Promise<string> {
  if (request.auth) return (await requireCustomer(request)).shopId;
  const shopId = "default";
  const snapshot = await getFirestore().collection("shops").doc(shopId).get();
  // Existing shops predate the optional active flag. Only an explicit false
  // disables public browsing; requiring true locks every legacy shop out.
  if (!snapshot.exists || snapshot.get("active") === false) {
    throw new HttpsError("failed-precondition", "Shop is not available");
  }
  return shopId;
}

export function beginCatalogUpload(kind: "categories" | "subcategories" | "products" | "banners", staffPermission?: string) {
  return onCall(catalogOptions, async (request) => {
    const context = staffPermission ? await requireAdminOrStaff(request, staffPermission) : await requireAdmin(request);
    const uploadToken = randomUUID();
    const storagePath = `shops/${context.shopId}/${kind}/uploads/${uploadToken}.jpg`;
    await session(context.shopId, uploadToken).set({
      uid: context.uid, kind, storagePath, createdAt: FieldValue.serverTimestamp(),
      expiresAt: Timestamp.fromMillis(Date.now() + 30 * 60_000),
    });
    return {uploadToken, storagePath};
  });
}

export async function finalizeUpload(
  context: AdminContext, token: string | undefined, kind: "categories" | "subcategories" | "products" | "banners", targetId: string,
): Promise<{url: string; path: string; cleanup: () => Promise<void>} | null> {
  if (!token) return null;
  const reference = session(context.shopId, token);
  const snapshot = await reference.get();
  if (!snapshot.exists || snapshot.get("uid") !== context.uid || snapshot.get("kind") !== kind ||
      snapshot.get("expiresAt")?.toMillis?.() < Date.now()) {
    throw new HttpsError("failed-precondition", "Image upload expired", {reason: "UPLOAD_EXPIRED"});
  }
  const sourcePath = text(snapshot.get("storagePath"));
  const bucket = getStorage().bucket();
  const source = bucket.file(sourcePath);
  if (!(await source.exists())[0]) throw new HttpsError("failed-precondition", "Image upload missing");
  const downloadToken = randomUUID();
  const targetPath = `shops/${context.shopId}/${kind}/${targetId}/${randomUUID()}.jpg`;
  await source.copy(bucket.file(targetPath), {
    contentType: "image/jpeg", cacheControl: "public,max-age=31536000,immutable",
    metadata: {firebaseStorageDownloadTokens: String(downloadToken)},
  });
  const encoded = encodeURIComponent(targetPath);
  const url = `https://firebasestorage.googleapis.com/v0/b/${bucket.name}/o/${encoded}?alt=media&token=${downloadToken}`;
  return {url, path: targetPath, cleanup: async () => { await Promise.allSettled([source.delete(), reference.delete()]); }};
}

/** Remove finalized images only. Historical order/report values remain in Firestore snapshots. */
export async function deleteCatalogImageUrls(context: AdminContext, urls: unknown[]): Promise<void> {
  const prefix = `shops/${context.shopId}/`;
  const bucket = getStorage().bucket();
  const paths = urls.map(storagePathFromUrl).filter((path): path is string =>
    path != null && path.startsWith(prefix) && !path.includes("/uploads/"));
  await Promise.allSettled([...new Set(paths)].map((path) => bucket.file(path).delete({ignoreNotFound: true})));
}

function storagePathFromUrl(value: unknown): string | null {
  if (typeof value !== "string") return null;
  try {
    const url = new URL(value); const marker = "/o/"; const index = url.pathname.indexOf(marker);
    return index < 0 ? null : decodeURIComponent(url.pathname.slice(index + marker.length));
  } catch { return null; }
}

export function categoryResponse(id: string, value: FirebaseFirestore.DocumentData) {
  return {id, name: text(value.name), description: text(value.description), imageUrl: value.imageUrl ?? null,
    active: value.active === true, sortOrder: integer(value.sortOrder), productCount: integer(value.productCount),
    activeProductCount: integer(value.activeProductCount), revision: integer(value.revision)};
}

export function productResponse(id: string, value: FirebaseFirestore.DocumentData) {
  const ratingAverage = number(value.ratingAverage ?? value.averageRating);
  return {id, categoryId: text(value.categoryId), categoryName: text(value.categoryName),
    subcategoryId: text(value.subcategoryId) || null, subcategoryName: text(value.subcategoryName) || null,
    name: text(value.name),
    description: text(value.description), unit: text(value.unit, "PIECE"), priceMinor: integer(value.priceMinor),
    offerPriceMinor: value.offerPriceMinor ?? null, stockQuantity: number(value.stockQuantity),
    lowStockThreshold: value.lowStockThreshold ?? null, imageUrls: Array.isArray(value.imageUrls) ? value.imageUrls : [],
    attributes: value.attributes ?? {}, active: value.active === true, archived: value.archived === true,
    revision: integer(value.revision), ratingAverage: ratingAverage > 0 ? ratingAverage : null,
    ratingCount: Math.max(0, integer(value.ratingCount ?? value.reviewCount))};
}

export function text(value: unknown, fallback = ""): string {
  return typeof value === "string" ? value : fallback;
}
export function integer(value: unknown, fallback = 0): number {
  const parsed = Number(value); return Number.isInteger(parsed) ? parsed : fallback;
}
export function number(value: unknown, fallback = 0): number {
  const parsed = Number(value); return Number.isFinite(parsed) ? parsed : fallback;
}
export function normalize(value: string): string { return value.trim().replace(/\s+/g, " ").toLocaleLowerCase("en"); }
export function shop(context: AdminContext) { return getFirestore().collection("shops").doc(context.shopId); }
function session(shopId: string, token: string) { return getFirestore().collection("shops").doc(shopId).collection("uploadSessions").doc(token); }
