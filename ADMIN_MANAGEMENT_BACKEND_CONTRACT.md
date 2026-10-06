# Super Admin Admin-management backend contract

Admin identity management is privileged. Android calls the endpoints below but never creates Firebase Auth
users, writes roles, changes active state, or stores passwords directly. Deploy every endpoint separately to
each client flavor's Firebase project.

## Common authorization

Every callable must enforce App Check and require Firebase Authentication. On every request, load the caller's
trusted `users/{uid}` profile and require `role == SUPER_ADMIN` and `active == true`. Do not rely solely on a
custom claim or client payload. Scope targets to the caller's shop/tenant and reject cross-tenant IDs with
`PERMISSION_DENIED`. Apply rate limits, redact mobile/password data from logs, and write an immutable audit log.

## `superAdminListAdmins`

Input: empty map. Query only bounded `ADMIN` profiles belonging to the caller's tenant. Return:

```json
{ "admins": [{ "id": "uid", "displayName": "Name", "mobileNumber": "+91...", "role": "ADMIN", "shopId": "shop", "active": true, "createdAtEpochMillis": 0, "updatedAtEpochMillis": 0 }] }
```

Never return password credentials, hashes, salts, reset tokens, Auth metadata, or users from another tenant.
Production implementations should add cursor pagination when the configured Admin count can exceed one page.

## `superAdminCreateAdmin`

Input: `displayName`, canonical E.164 `mobileNumber`, `password`, and an advisory `role = ADMIN`. Ignore the
role value and hardcode `ADMIN`. Validate all input again, reserve the normalized mobile lookup atomically,
create the Firebase Auth identity, create `users/{uid}` with the caller's tenant and `active = true`, and store
the memory-hard password credential only in a server-only datastore. Compensate partial failures. Return the
created public Admin profile. Duplicate mobile returns `ALREADY_EXISTS` with reason `DUPLICATE_MOBILE`.

## `superAdminUpdateAdmin`

Input: `userId`, `displayName`, `mobileNumber`. Confirm the target is an `ADMIN` in the caller's tenant. If the
mobile changes, atomically replace the normalized lookup without exposing whether an account exists outside
the tenant. Return the updated public profile. This endpoint cannot change role, tenant, or password.

## `superAdminSetAdminActive`

Input: `userId`, `active`. Confirm the target is an `ADMIN` in the caller's tenant. Update Firebase Auth's
disabled state and the Firestore profile consistently. On deactivation, revoke refresh tokens immediately.
The sign-in callable must also reject inactive profiles and disabled Auth users. Return the updated profile.

## Rules and tests

Deploy `firestore.rules`; clients have no direct write permission to `users`. Admin SDK functions bypass rules
and therefore must perform every authorization check above. Emulator tests must cover unauthenticated,
non-Super-Admin, inactive Super Admin, cross-tenant target, duplicate mobile, forced role payload, invalid data,
partial failure compensation, token revocation, and attempts to modify a Super Admin account.
