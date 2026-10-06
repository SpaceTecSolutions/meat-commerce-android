import {DocumentSnapshot} from "firebase-admin/firestore";
import {getFirestore} from "./database.js";
import {CallableRequest, HttpsError, onCall} from "firebase-functions/v2/https";

const options = {region: "asia-south1", enforceAppCheck: true, timeoutSeconds: 30, memory: "256MiB" as const};
const allowedActions = new Set([
  "ADMIN_CREATED", "ADMIN_ACTIVATED", "ADMIN_DISABLED", "FEATURE_CHANGED",
  "PRODUCT_LIMIT_CHANGED", "PAYMENT_FEATURE_CHANGED",
]);

export const superAdminGetAuditLog = onCall(options, async (request) => {
  await requireSuperAdmin(request);
  const requestedAction = typeof request.data?.action === "string" ? request.data.action : null;
  if (requestedAction && !allowedActions.has(requestedAction)) {
    throw new HttpsError("invalid-argument", "Invalid audit action");
  }
  const limit = Math.min(100, Math.max(1, Number(request.data?.limit) || 30));
  const cursor = typeof request.data?.cursor === "string" ? request.data.cursor : null;
  let cursorDocument: DocumentSnapshot | null = cursor ?
    await getFirestore().collection("auditLogs").doc(cursor).get() : null;
  if (cursor && !cursorDocument?.exists) throw new HttpsError("invalid-argument", "Invalid audit cursor");

  const collected: DocumentSnapshot[] = [];
  let exhausted = false;
  while (collected.length < limit && !exhausted) {
    let query = getFirestore().collection("auditLogs").orderBy("createdAt", "desc").limit(100);
    if (cursorDocument) query = query.startAfter(cursorDocument);
    const page = await query.get();
    exhausted = page.size < 100;
    if (page.empty) break;
    for (const document of page.docs) {
      cursorDocument = document;
      if (!requestedAction || normalizeAction(document.get("action")) === requestedAction) collected.push(document);
      if (collected.length === limit) break;
    }
  }

  const actorIds = [...new Set(collected.map((document) => string(document.get("actorUid"))).filter(Boolean))];
  const actors = new Map<string, string>();
  await Promise.all(actorIds.map(async (id) => {
    const profile = await getFirestore().collection("users").doc(id).get();
    actors.set(id, string(profile.get("displayName"), "Super Admin"));
  }));
  return {
    entries: collected.map((document) => auditResponse(document, actors)),
    nextCursor: !exhausted && cursorDocument ? cursorDocument.id : null,
  };
});

async function requireSuperAdmin(request: CallableRequest) {
  if (!request.auth) throw new HttpsError("unauthenticated", "Authentication required");
  const profile = await getFirestore().collection("users").doc(request.auth.uid).get();
  if (!profile.exists || profile.get("active") !== true || profile.get("role") !== "SUPER_ADMIN") {
    throw new HttpsError("permission-denied", "Super Admin access required");
  }
}

function auditResponse(document: DocumentSnapshot, actors: Map<string, string>) {
  const actor = string(document.get("actorUid"));
  const action = normalizeAction(document.get("action"));
  const target = string(document.get("targetUid")) || string(document.get("targetId")) || null;
  return {
    id: document.id, action, actorUserId: actor, actorDisplayName: actors.get(actor) ?? "Super Admin",
    targetId: target, summary: summary(action, target),
    occurredAtEpochMillis: document.get("createdAt")?.toMillis?.() ?? 0,
    correlationId: string(document.get("correlationId"), document.id),
  };
}

function normalizeAction(value: unknown) {
  const action = string(value, "FEATURE_CHANGED");
  return action === "FEATURE_CONFIG_CHANGED" ? "FEATURE_CHANGED" : action;
}

function summary(action: string, target: string | null) {
  const label = action.toLocaleLowerCase("en").replaceAll("_", " ");
  return `${label.charAt(0).toUpperCase()}${label.slice(1)}${target ? ` · ${target}` : ""}`;
}

function string(value: unknown, fallback = "") { return typeof value === "string" ? value : fallback; }
