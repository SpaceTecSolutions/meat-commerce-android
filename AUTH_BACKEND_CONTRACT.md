# Mobile-number/password authentication backend contract

Firebase Authentication does not provide a native mobile-number-and-password credential provider. The
Android application therefore uses Firebase custom authentication through App Check-protected callable Cloud
Functions. A server-only HMAC mapping resolves the visible mobile number to an internal Firebase Email/Password
identity. The internal email never reaches feature code, password material is stored only by Firebase
Authentication, and no server credential ships in the APK.

## Required callable functions

All functions must use HTTPS callable functions, enforce App Check in production, validate input again on the
server, return generic public error messages, and apply per-IP, per-device, per-mobile, and per-challenge rate
limits.

### `authSignInWithMobilePassword`

Input:

```text
mobileNumber    canonical E.164 string
password        plaintext transported only over TLS and never logged
```

The backend looks up a server-only credential record by a keyed, non-reversible HMAC of the canonical mobile,
verifies the internal email/password through Firebase Authentication's Identity Toolkit API, checks that the
resolved UID, Firebase Auth user, mapping, and Firestore profile are active, then creates a short-lived Firebase
custom token for that UID. Return only:

```json
{ "customToken": "..." }
```

Invalid account and invalid password responses must be indistinguishable. Disabled accounts return a stable
machine reason such as `USER_DISABLED` without exposing private account data.

### `authRegisterCustomer`

Input:

```text
firstName
lastName
mobileNumber
password
verificationToken   short-lived, single-use token issued only after OTP verification
```

The server must ignore any client-supplied privileged role and always create `CUSTOMER`. In one idempotent,
compensating operation it must verify uniqueness, create the Firebase Auth identity, create `users/{uid}` with
`role = CUSTOMER` and `active = true`, create the server-only password credential, and return a custom token.
Never expose this endpoint for ADMIN, DELIVERY, or SUPER_ADMIN creation.

### `authRequestCustomerRegistration`

Input: canonical `mobileNumber`.

Create a cryptographically random six-digit OTP challenge with a short expiry, resend cooldown and strict
attempt limits. Send the OTP through the configured SMS provider. Store only a keyed OTP hash in a
server-only collection. The response contains only `challengeId` and `maskedDestination`.

### `authVerifyCustomerRegistration`

Input: `challengeId`, `verificationCode`.

Atomically validate and consume the challenge. Only after successful phone possession proof may this endpoint
return whether the number already has an account. For a new number, return a short-lived, single-use
`verificationToken`; for an existing number return `accountExists = true` and no private account details.
The registration endpoint must reject missing, expired, previously consumed, or mobile-mismatched tokens.

### `authRequestPasswordReset`

Input: canonical `mobileNumber`.

Create a cryptographically random, single-use challenge with a short expiry and attempt limit. Deliver the OTP
through an approved SMS provider. Store only a keyed hash of the OTP. Return a challenge ID and masked
destination. Return a syntactically identical response for unknown accounts to prevent user enumeration.

### `authVerifyPasswordResetCode`

Input: `challengeId`, `verificationCode`.

Atomically validate expiry, attempts, and OTP hash, then consume the challenge and return a short-lived,
single-use opaque `resetToken`. Never return the OTP or password credential record.

### `authResetPassword`

Input: `resetToken`, `newPassword`.

Atomically consume the reset token, validate the password policy, replace the server-side password hash,
revoke existing Firebase refresh tokens, and invalidate outstanding challenges. Return an empty success map.

## Client session flow

```text
callable verifies mobile/password
  → callable returns Firebase custom token
  → Android calls FirebaseAuth.signInWithCustomToken
  → Firebase restores the session on subsequent launches
  → Android reads users/{uid}
  → active status and role are resolved from the trusted profile
  → navigate to the matching role root
```

The login UI never asks for a role. UI routing is not authorization: Security Rules, custom claims where used,
and Cloud Functions must enforce role and shop boundaries. When an account is disabled, the backend must also
revoke refresh tokens and prevent issuance of new custom tokens.

## Error contract

Use callable status codes plus optional stable `details.reason` values:

```text
UNAUTHENTICATED                 invalid mobile or password
ALREADY_EXISTS                 duplicate customer account
RESOURCE_EXHAUSTED             rate limit reached
UNAVAILABLE                    temporary network/service failure
FAILED_PRECONDITION + USER_DISABLED
INVALID_ARGUMENT + INVALID_VERIFICATION_CODE
```

Do not put stack traces, password details, credential hashes, internal document paths, or account-existence
information in client-visible errors.

## Deployment requirements

- Store password credentials outside client-readable Firestore paths, preferably in a dedicated trusted
  backend datastore or a collection denied to all client SDK access.
- Use Secret Manager for SMS/API secrets and password pepper values.
- Enforce App Check on callable endpoints after registering debug and Play Integrity providers.
- Configure Firebase Auth authorized domains, SMS abuse controls, logging redaction, monitoring, and alerts.
- Add Emulator Suite tests for registration role enforcement, disabled users, duplicate accounts, brute-force
  limits, OTP expiry/replay, token revocation, and cross-shop access.
- Deploy separate functions, secrets, Auth configuration, and rules for every white-label Firebase project.
