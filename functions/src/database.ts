import {getFirestore as getAdminFirestore} from "firebase-admin/firestore";

export const FIRESTORE_DATABASE_ID = "default";

export function getFirestore() {
  return getAdminFirestore(FIRESTORE_DATABASE_ID);
}
