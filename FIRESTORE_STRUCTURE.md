# Initial Firebase data structure

Each product flavor uses a separate Firebase project. Data is never shared between client flavors. The
following structure is the initial contract; collections should be created only when the corresponding
feature is implemented.

## Cloud Firestore

```text
users/{uid}
shops/{shopId}
shops/{shopId}/config/features
shops/{shopId}/config/shop
shops/{shopId}/config/payment
shops/{shopId}/config/delivery
shops/{shopId}/categories/{categoryId}
shops/{shopId}/products/{productId}
shops/{shopId}/orders/{orderId}
shops/{shopId}/offers/{offerId}
shops/{shopId}/coupons/{couponId}
shops/{shopId}/notifications/{notificationId}
shops/{shopId}/auditLogs/{auditLogId}
shops/{shopId}/counters/products
users/{uid}/devices/{deviceId}
```

### `users/{uid}`

```text
mobileNumber
displayName
role                 SUPER_ADMIN | ADMIN | DELIVERY | CUSTOMER
shopId               nullable only for a provisioned platform-level identity
active
createdAt             server timestamp
updatedAt             server timestamp
```

The document ID must equal the Firebase Authentication UID. Clients must never be able to set or elevate
their own role. Customer registration creates `CUSTOMER` only. Admin and Delivery identities require trusted
Cloud Functions following the authority hierarchy.

### Shop configuration

The fixed documents under `shops/{shopId}/config` map to the shared core models:

- `features`: Super Admin allowances and `maxProducts`.
- `shop`: display name, currency, locale, time zone, contact details, and active state.
- `payment`: Admin enablement/configured state for permitted payment methods. No secrets are stored here.
- `delivery`: delivery staff, scheduled delivery, and tracking enablement settings.

Only Super Admin may modify feature allowances. Admin may configure settings only within those allowances.
Configuration documents should be observed only where they affect active UI or runtime behavior.
The `features` document also contains a monotonically increasing `revision` and server `updatedAt`. Client SDK
writes are denied; an authorized callable performs revision-checked transactional updates.

### Product counter

`shops/{shopId}/counters/products` stores `countedProducts`, the number of non-archived products. Active and
inactive products count; archived products do not. Product mutation functions update this counter in the same
transaction as the product state and enforce `maxProducts` before create or restore operations.

### Commerce collections

Products, categories, orders, offers, and coupons are scoped below `shops/{shopId}` so queries and security
rules always include tenant ownership. Use server timestamps, bounded queries, pagination, and required
composite indexes. Order status and payment status remain separate fields.

### Devices and notifications

FCM registration tokens belong under `users/{uid}/devices/{deviceId}` with platform and updated timestamp.
Tokens are private to the authenticated user and trusted notification backend. Notification fan-out and
privileged writes belong in Cloud Functions.

## Firebase Storage

```text
shops/{shopId}/products/{productId}/{fileName}
shops/{shopId}/categories/{categoryId}/{fileName}
users/{uid}/profile/{fileName}
```

Storage Rules must verify authentication, shop membership, role, ownership, content type, and file-size
limits. Download URLs are data, not authorization boundaries.

## Realtime Database

Realtime Database is reserved for temporary active delivery tracking:

```text
liveTracking/{shopId}/{orderId}
  deliveryUserId
  latitude
  longitude
  heading
  accuracy
  updatedAt
```

The Android DI graph exposes this database only through `RealtimeTrackingDatabaseAccessor`. Constructing the
accessor does not obtain a database instance. A future tracking feature may call it only after all of these
are true: Super Admin allows tracking, Admin enables it, the order is active, the assigned Delivery user is
authenticated, and required permission/foreground-service conditions are satisfied. When tracking is off,
no service, worker, listener, coordinate read, or coordinate write may start.

## Cloud Functions

Use callable or authenticated HTTPS functions for privileged identity creation, payment order creation and
verification, role changes, notification fan-out, product-limit enforcement, and other operations requiring
Admin SDK authority. Android callbacks never directly prove payment success.

## Security and deployment checklist

- Deploy Firestore, Storage, and Realtime Database Rules independently for every flavor's Firebase project.
- Enable App Check enforcement only after debug/release providers are registered and verified.
- Register release SHA-256 certificates for Play Integrity.
- Add required Firestore indexes from version-controlled index definitions when queries are implemented.
- Never store service accounts, Admin SDK credentials, Razorpay secrets, or backend secrets in client data.
- Test rules with the Firebase Emulator Suite before production deployment.
