# Server-authoritative feature-management contract

Feature settings are stored at `shops/{shopId}/config/features` in each flavor's independent Firebase
project. The document contains every `FeatureConfig` field plus a monotonically increasing `revision` and a
server `updatedAt` timestamp. Android's DataStore copy improves startup and avoids repeated reads; it is never
an authorization source.

## Common authorization

Both callable functions must enforce App Check and Firebase Authentication, load the caller's trusted active
`users/{uid}` profile, and derive `shopId` server-side. The read callable accepts active tenant roles. The update
callable additionally requires `role == SUPER_ADMIN`. Never accept tenant or authority from the client.

## `getAllowedFeatureConfig`

Input: empty map. Any authenticated active tenant role may read its own tenant's allowances; derive the tenant
server-side and grant no write authority. Read the single feature document and return:

```json
{
  "config": {
    "deliveryStaffManagementAllowed": false,
    "realtimeTrackingAllowed": false,
    "codAllowed": false,
    "razorpayAllowed": false,
    "upiAllowed": false,
    "offersAllowed": false,
    "couponsAllowed": false,
    "scheduledDeliveryAllowed": false,
    "maxProducts": 100,
    "revision": 1
  }
}
```

## `superAdminUpdateFeatureConfig`

Input contains all feature fields, `maxProducts`, and `expectedRevision`. Validate booleans, require
`0 <= maxProducts <= 100000`, and reject realtime tracking when delivery staff management is disabled. In a
Firestore transaction, require the stored revision to equal `expectedRevision`, replace the allowed fields,
increment revision, set a server timestamp, and append an audit record. Return the committed config. Use
`ABORTED` for revision conflicts, `INVALID_ARGUMENT` for invalid settings, and `PERMISSION_DENIED` for failed
authorization.

## Mandatory downstream enforcement

Every privileged backend operation must read the authoritative config in its transaction or use a short-lived,
server-side invalidated cache. In particular:

- Delivery-user creation requires `deliveryStaffManagementAllowed`.
- RTDB tracking token/session creation requires both delivery management and realtime tracking.
- Razorpay order creation requires `razorpayAllowed`; verification remains server-only.
- COD, UPI, scheduled delivery, offers, and coupons require their corresponding allowance before writes.
- Product creation must transactionally enforce the authoritative `maxProducts`; list counts from Android are
  never sufficient and concurrent requests must not bypass the limit.

When a capability is disabled, its worker, listener, SDK flow, RTDB session, and callable must not start. Rules
and functions must fail closed even if a modified client sends a request.

## Client caching behavior

The Hilt-singleton repository loads one DataStore snapshot, performs one authorized refresh when first used,
and shares one `StateFlow` with all consumers. A successful server response replaces the cache. Failed writes
never update it. This prevents every screen from attaching its own Firestore listener or query.

## Deployment tests

For every flavor, Emulator Suite tests must cover non-Super-Admin writes, inactive callers, cross-tenant access,
invalid limits, stale revisions, tracking without delivery management, concurrent product-limit enforcement,
disabled payment calls, audit creation, and direct client writes to the feature document.
