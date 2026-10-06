# Phase 41 security review

## Privileged identity boundary

Ordinary Android code has no API that accepts a requested role. Customer registration no longer sends `CUSTOMER`, and Admin creation no longer sends `ADMIN`; the callable endpoint defines the only role it can create. Delivery creation follows its dedicated Admin-authorized endpoint. No generic `setRole`, custom-claims, or user-document write operation exists in the client.

Firestore denies all client writes to `users`, including `role`, `shopId`, and `active`. It also denies all direct access to audit records. Cloud Functions must verify Firebase Auth, App Check, the caller's active server-side profile, and the exact required role on every request. Client navigation and hidden controls are defense in depth only.

Role changes and custom claims are Admin SDK-only. A callable must never accept a role, tenant, shop ID, or privilege set from the request. The server must derive them from the endpoint and verified actor. After any claim change, revoke refresh tokens and require a fresh ID token.

## Audit requirements

The following successful transactions append an immutable audit record in the same transaction or through a reliable transactional outbox: `ADMIN_CREATED`, `ADMIN_DISABLED`, `FEATURE_CHANGED`, `PRODUCT_LIMIT_CHANGED`, and `PAYMENT_FEATURE_CHANGED`. Feature updates that change COD, Razorpay, or UPI write both the general feature event and the payment-specific event.

Each record contains a server-generated ID, server timestamp, verified actor UID and display snapshot, action, target identifier, safe before/after summary, correlation/idempotency ID, and tenant/application context. Never include passwords, tokens, payment secrets, full personal addresses, or raw credentials.

`superAdminGetAuditLog` requires App Check and an active `SUPER_ADMIN`, supports an allow-listed action filter and opaque cursor, caps page size at 100, and returns newest entries first. Audit records cannot be edited or deleted by Android users, including Super Admins. Apply backend retention/export policy separately.

## Privileged mutation checks

- `superAdminCreateAdmin` always creates `ADMIN`; reject any unexpected role field.
- `superAdminSetAdminActive` cannot target a Super Admin and audits disabling.
- `superAdminUpdateFeatureConfig` uses revision checks, validates all values, and audits only committed differences.
- `superAdminUpdateProductLimit` enforces the catalog policy transactionally and audits committed changes.
- Payment capability changes are enforced again by checkout/payment Cloud Functions; configuration UI state is not authorization.
