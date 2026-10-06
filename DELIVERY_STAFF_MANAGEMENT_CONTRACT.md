# Delivery staff management contract

Delivery staff management is available only while the server-authoritative `deliveryStaffManagementAllowed` flag is `true`.

## Android gating

- The Admin Settings menu omits the Delivery Staff item when disabled; no spacer or placeholder is emitted.
- There is no role-registry/deep-link route for this feature. It is composed from the gated Admin Settings branch only.
- The `DeliveryStaffViewModel` is created only after the enabled menu item is opened. Consequently, a disabled feature starts no staff query, snapshot listener, or repository work.
- This implementation uses explicit callable refreshes and does not attach a Firestore staff listener.

## Callable operations

```text
adminListDeliveryStaff
adminCreateDeliveryStaff
adminUpdateDeliveryStaff
adminSetDeliveryStaffActive
```

Every callable must verify Firebase Auth, active `ADMIN` role, shop ownership, and the current feature flag before reading or writing staff. A cached client flag is never authorization. When disabled, return `PERMISSION_DENIED` with `reason: FEATURE_DISABLED`.

Creation is performed with the Firebase Admin SDK in trusted backend code, always assigns `role: DELIVERY` and the acting Admin's `shopId`, and rejects duplicate normalized mobile numbers. The client cannot submit a role. Delivery users have no registration path; common customer registration always creates `CUSTOMER`.

Activation/deactivation must update both the user profile and Firebase Auth disabled state consistently. Admins may manage only Delivery users belonging to their own shop. Firestore Security Rules deny direct client creation, role changes, shop changes, and active-state writes.
