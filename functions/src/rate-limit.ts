import {FieldValue} from "firebase-admin/firestore";
import {getFirestore} from "./database.js";
import {HttpsError} from "firebase-functions/v2/https";

const WINDOW_MILLIS = 15 * 60 * 1000;
const BLOCK_MILLIS = 30 * 60 * 1000;
const MAX_ATTEMPTS = 8;

type RateLimitData = {
  attempts?: number;
  windowStartedAtMillis?: number;
  blockedUntilMillis?: number;
};

export async function consumeSignInAttempt(lookupId: string): Promise<void> {
  const firestore = getFirestore();
  const reference = firestore.collection("authRateLimits").doc(lookupId);
  const now = Date.now();

  const blocked = await firestore.runTransaction(async (transaction) => {
    const snapshot = await transaction.get(reference);
    const stored = (snapshot.data() ?? {}) as RateLimitData;
    if ((stored.blockedUntilMillis ?? 0) > now) {
      return true;
    }

    const insideWindow = now - (stored.windowStartedAtMillis ?? 0) < WINDOW_MILLIS;
    const attempts = insideWindow ? (stored.attempts ?? 0) + 1 : 1;
    const windowStartedAtMillis = insideWindow ? stored.windowStartedAtMillis ?? now : now;
    const blockedUntilMillis = attempts > MAX_ATTEMPTS ? now + BLOCK_MILLIS : 0;
    transaction.set(reference, {
      attempts,
      windowStartedAtMillis,
      blockedUntilMillis,
      updatedAt: FieldValue.serverTimestamp(),
      expireAt: new Date(now + 24 * 60 * 60 * 1000),
    });
    return blockedUntilMillis > 0;
  });
  if (blocked) throw new HttpsError("resource-exhausted", "Please wait before trying again");
}

export async function clearSignInAttempts(lookupId: string): Promise<void> {
  await getFirestore().collection("authRateLimits").doc(lookupId).delete().catch(() => undefined);
}
