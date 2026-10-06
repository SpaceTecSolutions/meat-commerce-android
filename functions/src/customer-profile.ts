import {FieldValue} from "firebase-admin/firestore";
import {onCall, HttpsError} from "firebase-functions/v2/https";
import {catalogOptions, requireCustomer, text} from "./catalog-common.js";
import {getFirestore} from "./database.js";

export const customerUpdateProfile = onCall(catalogOptions, async (request) => {
  const context = await requireCustomer(request);
  const firstName = text(request.data?.firstName).trim();
  const lastName = text(request.data?.lastName).trim();
  if (firstName.length < 2 || firstName.length > 50 || lastName.length > 50) {
    throw new HttpsError("invalid-argument", "Enter a valid name");
  }
  await getFirestore().collection("users").doc(context.uid).update({
    firstName, lastName, displayName: [firstName, lastName].filter(Boolean).join(" "),
    updatedAt: FieldValue.serverTimestamp(),
  });
  return {success: true};
});
