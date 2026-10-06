import {getAuth} from "firebase-admin/auth";
import {FieldValue} from "firebase-admin/firestore";
import {getFirestore} from "./database.js";
import {defineSecret} from "firebase-functions/params";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {canonicalMobile, mobileLookupId, validPassword} from "./auth-helpers.js";
import {verifyFirebasePassword} from "./identity-password.js";
import {clearSignInAttempts, consumeSignInAttempt} from "./rate-limit.js";

const hmacKey = defineSecret("AUTH_LOOKUP_HMAC_KEY");
const webApiKey = defineSecret("IDENTITY_TOOLKIT_API_KEY");

type CredentialMapping = {
  uid?: string;
  internalEmail?: string;
  active?: boolean;
};

type UserProfile = {
  active?: boolean;
  role?: string;
  shopId?: string | null;
};

export const authSignInWithMobilePassword = onCall(
  {
    region: "asia-south1",
    enforceAppCheck: true,
    secrets: [hmacKey, webApiKey],
    timeoutSeconds: 30,
    memory: "256MiB",
    maxInstances: 20,
  },
  async (request): Promise<{customToken: string}> => {
    const mobile = canonicalMobile(request.data?.mobileNumber);
    const password = request.data?.password;
    if (!mobile || !validPassword(password)) throw invalidCredentials();

    const lookupId = mobileLookupId(mobile, hmacKey.value());
    await consumeSignInAttempt(lookupId);

    const firestore = getFirestore();
    const mappingSnapshot = await firestore.collection("authCredentials").doc(lookupId).get();
    const mapping = mappingSnapshot.data() as CredentialMapping | undefined;
    if (!mapping?.uid || !mapping.internalEmail || mapping.active !== true) throw invalidCredentials();

    let verification;
    try {
      verification = await verifyFirebasePassword(webApiKey.value(), mapping.internalEmail, password);
    } catch {
      throw new HttpsError("unavailable", "Authentication service is temporarily unavailable");
    }
    if (verification.status === "disabled") throw disabledAccount();
    if (verification.status !== "valid" || verification.uid !== mapping.uid) throw invalidCredentials();

    const [authUser, profileSnapshot] = await Promise.all([
      getAuth().getUser(mapping.uid),
      firestore.collection("users").doc(mapping.uid).get(),
    ]);
    const profile = profileSnapshot.data() as UserProfile | undefined;
    if (authUser.disabled || profile?.active !== true) throw disabledAccount();
    if (!profileSnapshot.exists || !profile?.role) throw invalidCredentials();

    const customToken = await getAuth().createCustomToken(mapping.uid, {
      role: profile.role,
      ...(profile.shopId ? {shopId: profile.shopId} : {}),
    });
    await Promise.all([
      clearSignInAttempts(lookupId),
      profileSnapshot.ref.update({lastLoginAt: FieldValue.serverTimestamp()}),
    ]);
    return {customToken};
  },
);

function invalidCredentials(): HttpsError {
  return new HttpsError("unauthenticated", "Invalid mobile number or password");
}

function disabledAccount(): HttpsError {
  return new HttpsError("permission-denied", "This account is disabled", {reason: "USER_DISABLED"});
}
