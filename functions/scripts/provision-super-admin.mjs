import {createHmac} from "node:crypto";
import {applicationDefault, initializeApp} from "firebase-admin/app";
import {getAuth} from "firebase-admin/auth";
import {FieldValue, getFirestore} from "firebase-admin/firestore";

const required = (name) => {
  const value = process.env[name]?.trim();
  if (!value) throw new Error(`Missing required environment variable: ${name}`);
  return value;
};

const mobile = required("SUPER_ADMIN_MOBILE").replace(/[\s()-]/g, "");
const password = required("SUPER_ADMIN_PASSWORD");
const hmacKey = required("AUTH_LOOKUP_HMAC_KEY");
const firstName = process.env.SUPER_ADMIN_FIRST_NAME?.trim() || "Super";
const lastName = process.env.SUPER_ADMIN_LAST_NAME?.trim() || "Admin";
const existingUid = process.env.SUPER_ADMIN_UID?.trim();
if (!/^\+[1-9]\d{7,14}$/.test(mobile)) throw new Error("SUPER_ADMIN_MOBILE must be E.164");
if (password.length < 12) throw new Error("SUPER_ADMIN_PASSWORD must contain at least 12 characters");

initializeApp({credential: applicationDefault()});
const auth = getAuth();
const firestore = getFirestore("default");
const lookupId = createHmac("sha256", hmacKey).update(mobile, "utf8").digest("hex");
const internalEmail = `sa-${lookupId.slice(0, 32)}@auth.meatbush.invalid`;

let user;
if (existingUid) {
  await auth.getUser(existingUid);
  user = await auth.updateUser(existingUid, {email: internalEmail, password, disabled: false,
    displayName: `${firstName} ${lastName}`});
} else {
  try {
    user = await auth.getUserByEmail(internalEmail);
    user = await auth.updateUser(user.uid, {password, disabled: false, displayName: `${firstName} ${lastName}`});
  } catch (error) {
    if (error?.code !== "auth/user-not-found") throw error;
    user = await auth.createUser({email: internalEmail, password, disabled: false,
      displayName: `${firstName} ${lastName}`});
  }
}
await auth.setCustomUserClaims(user.uid, {role: "SUPER_ADMIN"});

const userReference = firestore.collection("users").doc(user.uid);
const mappingReference = firestore.collection("authCredentials").doc(lookupId);
await firestore.runTransaction(async (transaction) => {
  const existing = await transaction.get(userReference);
  transaction.set(userReference, {
    displayName: `${firstName} ${lastName}`,
    firstName,
    lastName,
    mobileNumber: mobile,
    role: "SUPER_ADMIN",
    active: true,
    shopId: null,
    createdAt: existing.exists ? existing.get("createdAt") ?? FieldValue.serverTimestamp() : FieldValue.serverTimestamp(),
    updatedAt: FieldValue.serverTimestamp(),
    lastLoginAt: null,
  });
  transaction.set(mappingReference, {
    uid: user.uid,
    internalEmail,
    active: true,
    createdAt: FieldValue.serverTimestamp(),
    updatedAt: FieldValue.serverTimestamp(),
  }, {merge: true});
});

console.log(`Super Admin provisioned successfully. UID: ${user.uid}`);
