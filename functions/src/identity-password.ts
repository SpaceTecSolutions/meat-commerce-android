type IdentityResponse = {
  localId?: string;
  error?: {message?: string};
};

export type PasswordVerification =
  | {status: "valid"; uid: string}
  | {status: "invalid"}
  | {status: "disabled"};

export async function verifyFirebasePassword(
  apiKey: string,
  internalEmail: string,
  password: string,
): Promise<PasswordVerification> {
  const response = await fetch(
    `https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${encodeURIComponent(apiKey)}`,
    {
      method: "POST",
      headers: {"content-type": "application/json"},
      body: JSON.stringify({email: internalEmail, password, returnSecureToken: true}),
    },
  );
  const body = await response.json() as IdentityResponse;
  if (response.ok && body.localId) return {status: "valid", uid: body.localId};

  const reason = body.error?.message?.split(" : ")[0] ?? "";
  if (reason === "USER_DISABLED") return {status: "disabled"};
  if (["EMAIL_NOT_FOUND", "INVALID_PASSWORD", "INVALID_LOGIN_CREDENTIALS"].includes(reason)) {
    return {status: "invalid"};
  }
  throw new Error(`Identity Toolkit password verification failed: ${response.status}`);
}
