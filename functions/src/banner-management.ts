import {FieldValue} from "firebase-admin/firestore";
import {getStorage} from "firebase-admin/storage";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {beginCatalogUpload, catalogOptions, finalizeUpload, integer, requireAdmin, shop, text} from "./catalog-common.js";

const alignments = new Set([
  "TOP_START", "TOP_CENTER", "TOP_END", "CENTER_START", "CENTER", "CENTER_END",
  "BOTTOM_START", "BOTTOM_CENTER", "BOTTOM_END",
]);

export const adminBeginBannerImageUpload = beginCatalogUpload("banners");

export const adminGetBanners = onCall(catalogOptions, async (request) => {
  const context = await requireAdmin(request);
  const snapshot = await shop(context).collection("banners").limit(5).get();
  return {banners: snapshot.docs.map((doc) => response(doc.id, doc.data()))
    .sort((a, b) => a.sortOrder - b.sortOrder)};
});

export const adminCreateBanner = onCall(catalogOptions, async (request) => {
  const context = await requireAdmin(request); const input = validate(request.data);
  const collection = shop(context).collection("banners");
  const current = await collection.get();
  if (current.size >= 5) throw new HttpsError("failed-precondition", "Maximum 5 banners allowed");
  if (current.empty && !input.active) throw new HttpsError("failed-precondition", "The first banner must be visible");
  const reference = collection.doc();
  const image = await finalizeUpload(context, input.imageUploadToken, "banners", reference.id);
  if (!image) throw new HttpsError("invalid-argument", "Banner image is required");
  const value = {...input, imageUrl: image.url, imagePath: image.path, revision: 0,
    createdAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp(), updatedBy: context.uid};
  delete value.imageUploadToken;
  await reference.create(value); await image.cleanup();
  return {banner: response(reference.id, value)};
});

export const adminUpdateBanner = onCall(catalogOptions, async (request) => {
  const context = await requireAdmin(request); const input = validate(request.data);
  const id = text(request.data?.bannerId); const expected = integer(request.data?.expectedRevision, -1);
  if (!id || expected < 0) throw new HttpsError("invalid-argument", "Banner revision required");
  const reference = shop(context).collection("banners").doc(id);
  const image = await finalizeUpload(context, input.imageUploadToken, "banners", id);
  let oldImagePath: string | null = null;
  const result = await reference.firestore.runTransaction(async (transaction) => {
    const [current, activeBanners] = await Promise.all([
      transaction.get(reference), transaction.get(shop(context).collection("banners").where("active", "==", true)),
    ]);
    if (!current.exists) throw new HttpsError("not-found", "Banner not found");
    if (integer(current.get("revision")) !== expected) changed();
    if (current.get("active") === true && !input.active && activeBanners.size <= 1) minimum();
    oldImagePath = text(current.get("imagePath")) || null;
    const update: Record<string, unknown> = {...input, revision: expected + 1,
      updatedAt: FieldValue.serverTimestamp(), updatedBy: context.uid};
    delete update.imageUploadToken;
    if (image) { update.imageUrl = image.url; update.imagePath = image.path; }
    transaction.update(reference, update);
    return response(id, {...current.data(), ...update});
  });
  await image?.cleanup();
  if (image && oldImagePath) await getStorage().bucket().file(oldImagePath).delete().catch(() => undefined);
  return {banner: result};
});

export const adminSetBannerActive = onCall(catalogOptions, async (request) => {
  const context = await requireAdmin(request); const id = text(request.data?.bannerId);
  const expected = integer(request.data?.expectedRevision, -1); const active = request.data?.active === true;
  const collection = shop(context).collection("banners"); const reference = collection.doc(id);
  const result = await reference.firestore.runTransaction(async (transaction) => {
    const [current, activeBanners] = await Promise.all([
      transaction.get(reference), transaction.get(collection.where("active", "==", true)),
    ]);
    if (!current.exists) throw new HttpsError("not-found", "Banner not found");
    if (integer(current.get("revision")) !== expected) changed();
    if (!active && current.get("active") === true && activeBanners.size <= 1) minimum();
    const update = {active, revision: expected + 1, updatedAt: FieldValue.serverTimestamp(), updatedBy: context.uid};
    transaction.update(reference, update); return response(id, {...current.data(), ...update});
  });
  return {banner: result};
});

export const adminDeleteBanner = onCall(catalogOptions, async (request) => {
  const context = await requireAdmin(request); const id = text(request.data?.bannerId);
  const expected = integer(request.data?.expectedRevision, -1); const collection = shop(context).collection("banners");
  const reference = collection.doc(id); let imagePath: string | null = null;
  await reference.firestore.runTransaction(async (transaction) => {
    const [current, active] = await Promise.all([
      transaction.get(reference), transaction.get(collection.where("active", "==", true)),
    ]);
    if (!current.exists) throw new HttpsError("not-found", "Banner not found");
    if (integer(current.get("revision")) !== expected) changed();
    if (current.get("active") === true && active.size <= 1) minimum();
    imagePath = text(current.get("imagePath")) || null; transaction.delete(reference);
  });
  if (imagePath) await getStorage().bucket().file(imagePath).delete().catch(() => undefined);
  return {deleted: true};
});

function validate(data: any) {
  const title = text(data?.title).trim(); const subtitle = text(data?.subtitle).trim();
  const buttonEnabled = data?.buttonEnabled === true; const buttonText = text(data?.buttonText, "Shop Now").trim();
  const actionRoute = text(data?.actionRoute).trim() || null;
  const contentAlignment = text(data?.contentAlignment, "BOTTOM_START");
  const buttonColor = text(data?.buttonColor, "RED");
  const buttonTextColor = text(data?.buttonTextColor, "WHITE");
  const buttonShape = text(data?.buttonShape, "ROUNDED");
  if (!title || title.length > 80 || subtitle.length > 180 || !alignments.has(contentAlignment) ||
      integer(data?.sortOrder) < 0 || (buttonEnabled && (!buttonText || !actionRoute)) ||
      !["RED", "WHITE"].includes(buttonColor) || !["WHITE", "RED", "BLACK"].includes(buttonTextColor) ||
      !["ROUNDED", "CAPSULE"].includes(buttonShape)) {
    throw new HttpsError("invalid-argument", "Check banner text, alignment and button action");
  }
  return {title, subtitle, buttonEnabled, buttonText, actionRoute, contentAlignment,
    textPlacement: placement(data?.textPlacement), buttonPlacement: placement(data?.buttonPlacement),
    subtitlePlacement: placement(data?.subtitlePlacement), buttonColor, buttonTextColor, buttonShape,
    buttonArrow: data?.buttonArrow === true,
    active: data?.active !== false, sortOrder: integer(data?.sortOrder),
    imageUploadToken: text(data?.imageUploadToken) || undefined};
}
function response(id: string, value: FirebaseFirestore.DocumentData) {
  return {id, title: text(value.title), subtitle: text(value.subtitle), imageUrl: value.imageUrl ?? null,
    actionRoute: value.actionRoute ?? null, active: value.active === true, sortOrder: integer(value.sortOrder),
    contentAlignment: text(value.contentAlignment, "BOTTOM_START"), buttonEnabled: value.buttonEnabled === true,
    textPlacement: value.textPlacement ?? null, buttonPlacement: value.buttonPlacement ?? null,
    subtitlePlacement: value.subtitlePlacement ?? null,
    buttonColor: text(value.buttonColor, "RED"), buttonTextColor: text(value.buttonTextColor, "WHITE"),
    buttonShape: text(value.buttonShape, "ROUNDED"),
    buttonArrow: value.buttonArrow === true,
    buttonText: text(value.buttonText, "Shop Now"), revision: integer(value.revision)};
}
function changed(): never { throw new HttpsError("aborted", "Banner changed"); }
function placement(value: any) {
  if (value == null) return null;
  if (![value.x, value.y].every((it) => typeof it === "number" && Number.isFinite(it) && it >= 0 && it <= 1) ||
      !["LEFT", "CENTER", "RIGHT"].includes(value.alignment)) {
    throw new HttpsError("invalid-argument", "Invalid banner placement");
  }
  return {x: value.x, y: value.y, alignment: value.alignment};
}
function minimum(): never { throw new HttpsError("failed-precondition", "At least one banner must remain visible"); }
