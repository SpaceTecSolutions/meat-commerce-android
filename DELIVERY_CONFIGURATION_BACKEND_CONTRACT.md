# Delivery configuration backend contract

Phase 19 uses callable Cloud Functions so delivery policy cannot be bypassed by modifying Android.

## Functions

### `adminGetDeliveryConfig`

- Require Firebase Auth, valid App Check, an active profile, and `ADMIN` role.
- Return `{ config: DeliveryConfig }`, including `currencyCode` and `revision`.

### `adminUpdateDeliveryConfig`

- Apply the same authentication and role checks in a Firestore transaction.
- Read the server-side Super Admin `FeatureConfig` in that transaction.
- Reject `scheduledDeliveryEnabled = true` when `scheduledDeliveryAllowed = false` with
  `FAILED_PRECONDITION`. Never trust a permission value from Android.
- Validate non-negative minor-unit amounts, unique slot IDs, non-empty labels, valid bounds, and
  non-overlapping active slots.
- Compare `expectedRevision`, increment it atomically, and return the authoritative config.
- Audit actor UID, previous/new values, client ID, and server timestamp.

Suggested document: `clients/{clientId}/config/delivery`. Deny direct client writes. Checkout must
read this authoritative document and recalculate minimum order, charge, free-delivery threshold,
and selected slot. It must re-check the Super Admin flag before accepting a scheduled order.

If Super Admin later disables scheduled delivery, effective scheduled delivery becomes false.
Existing placed orders retain their promised slot; new scheduled orders are rejected.
