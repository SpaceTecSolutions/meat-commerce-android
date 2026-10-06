import {FieldValue, Timestamp} from "firebase-admin/firestore";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {catalogOptions, integer, publicCustomerShop, requireAdminOrStaff, shop, text} from "./catalog-common.js";

function faq(id: string, value: FirebaseFirestore.DocumentData) {
  return {id, question: text(value.question), answer: text(value.answer), active: value.active === true,
    sortOrder: integer(value.sortOrder), revision: integer(value.revision),
    createdAtEpochMillis: value.createdAt instanceof Timestamp ? value.createdAt.toMillis() : 0,
    updatedAtEpochMillis: value.updatedAt instanceof Timestamp ? value.updatedAt.toMillis() : 0,
    createdBy: text(value.createdBy) || null};
}
export const customerGetFaqs = onCall(catalogOptions, async (request) => {
  const id = await publicCustomerShop(request); const snap = await shop({shopId: id} as any).collection("faqs").limit(100).get();
  return {faqs: snap.docs.filter((doc) => doc.get("active") === true).map((doc) => faq(doc.id, doc.data()))
    .sort((a, b) => a.sortOrder - b.sortOrder)};
});
export const adminGetFaqs = onCall(catalogOptions, async (request) => {
  const context = await requireAdminOrStaff(request, "MANAGE_FAQ"); const snap = await shop(context).collection("faqs").limit(100).get();
  return {faqs: snap.docs.map((doc) => faq(doc.id, doc.data())).sort((a, b) => a.sortOrder - b.sortOrder)};
});
export const adminSaveFaq = onCall(catalogOptions, async (request) => {
  const context = await requireAdminOrStaff(request, "MANAGE_FAQ"); const question = text(request.data?.question).trim();
  const answer = text(request.data?.answer).trim(); const id = text(request.data?.faqId);
  if (question.length < 2 || question.length > 160 || answer.length < 2 || answer.length > 2000)
    throw new HttpsError("invalid-argument", "Invalid FAQ");
  const ref = id ? shop(context).collection("faqs").doc(id) : shop(context).collection("faqs").doc();
  const previous = await ref.get(); const expected = integer(request.data?.expectedRevision, -1);
  if (previous.exists && integer(previous.get("revision")) !== expected) throw new HttpsError("aborted", "FAQ changed");
  const value = {question, answer, active: request.data?.active !== false, sortOrder: integer(request.data?.sortOrder),
    revision: previous.exists ? expected + 1 : 0, updatedAt: FieldValue.serverTimestamp(), updatedBy: context.uid,
    ...(!previous.exists ? {createdAt: FieldValue.serverTimestamp(), createdBy: context.uid} : {})};
  await ref.set(value, {merge: previous.exists}); return {faq: faq(ref.id, value)};
});
export const adminDeleteFaq = onCall(catalogOptions, async (request) => {
  const context = await requireAdminOrStaff(request, "MANAGE_FAQ"); await shop(context).collection("faqs").doc(text(request.data?.faqId)).delete();
  return {deleted: true};
});
