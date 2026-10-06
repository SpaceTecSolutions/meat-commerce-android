import {readFileSync} from "node:fs";
import {cert, deleteApp, initializeApp} from "firebase-admin/app";
import {getAuth} from "firebase-admin/auth";
import {getFirestore} from "firebase-admin/firestore";
import {createHmac} from "node:crypto";

const credentialsPath = process.env.GOOGLE_APPLICATION_CREDENTIALS;
const hmacKey = process.env.AUTH_LOOKUP_HMAC_KEY;
if (!credentialsPath || !hmacKey) throw new Error("Credentials and AUTH_LOOKUP_HMAC_KEY are required");
const credentials = JSON.parse(readFileSync(credentialsPath, "utf8"));
const app = initializeApp({credential: cert(credentials)}, "registration-inspection");
const firestore = getFirestore(app, "default");

try {
  const tokens = await firestore.collection("authFlowTokens").where("purpose", "==", "REGISTER").get();
  const recent = tokens.docs.map((document) => document.data())
    .filter((value) => value.consumed !== true)
    .sort((left, right) => (right.expiresAtMillis ?? 0) - (left.expiresAtMillis ?? 0))[0];
  if (!recent?.mobile) {
    console.log(JSON.stringify({unconsumedRegistrationToken: false}, null, 2));
  } else {
    const lookupId = createHmac("sha256", hmacKey).update(recent.mobile, "utf8").digest("hex");
    const mapping = await firestore.collection("authCredentials").doc(lookupId).get();
    let authUserExists = false;
    try {
      await getAuth(app).getUserByPhoneNumber(recent.mobile);
      authUserExists = true;
    } catch (error) {
      if (error.code !== "auth/user-not-found") throw error;
    }
    console.log(JSON.stringify({
      unconsumedRegistrationToken: true,
      tokenExpired: recent.expiresAtMillis < Date.now(),
      credentialMappingExists: mapping.exists,
      authUserExistsForPhone: authUserExists,
      mobileSuffix: `***${recent.mobile.slice(-2)}`,
    }, null, 2));
  }
} finally {
  await deleteApp(app);
}
