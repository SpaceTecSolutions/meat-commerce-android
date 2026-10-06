# Payment configuration backend contract

Phase 20 uses generic Admin payment-configuration callables. Android never receives or submits a
Razorpay secret key.

## `adminGetPaymentConfig`

- Require Firebase Auth, valid App Check, active profile, matching client, and `ADMIN` role.
- Read the server-side Super Admin feature policy and Admin payment configuration.
- Return only non-secret state: `codEnabled`, `razorpayEnabled`, `razorpayConfigured`, `upiEnabled`,
  optional public display metadata, and `revision`.
- Derive `razorpayConfigured` on the backend from securely provisioned server configuration. Do not
  infer it from a client-writable Firestore field.

## `adminUpdatePaymentConfig`

- Apply the same checks in a Firestore transaction.
- Intersect requested enablement with the current Super Admin `codAllowed`, `razorpayAllowed`, and
  `upiAllowed` values. Reject attempts to enable a disallowed method.
- Reject Razorpay enablement unless secure backend configuration is complete.
- Compare `expectedRevision`, increment it atomically, and return authoritative non-secret state.
- Audit actor UID, client ID, previous/new values, and server timestamp.

Suggested document: `clients/{clientId}/config/payments`. Deny direct client writes. Never store
Razorpay `key_secret`, service-account credentials, or other backend secrets in this document or in
Android resources. Secrets belong in the Cloud Functions secret manager.

## Runtime and billing guard

Checkout must resolve effective methods from both Super Admin permission and Admin enablement. When
Razorpay is disabled, not configured, or not allowed, Android must not call create-order/verify
functions or launch Razorpay Checkout. The backend create-order function must independently reject
the request before contacting Razorpay unless all three conditions are true.
