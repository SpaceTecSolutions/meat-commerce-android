# COD payment backend contract

COD order creation from `customerPlaceOrder` must persist:

```text
paymentMethod = COD
paymentStatus = PENDING
orderStatus = PENDING
```

The order stores the authoritative immutable amount due in minor currency units. Android never sets
initial statuses or the amount due.

## Admin operations

- `adminGetCodOrders`: active Admin role; return only the current client's COD orders.
- `adminConfirmCodOrder`: transactionally require COD, `orderStatus=PENDING`,
  `paymentStatus=PENDING`, matching revision, and a non-cancelled order. Set order to `CONFIRMED`.
- `adminCancelCodOrder`: require a non-empty reason and reject delivered/already-cancelled orders.
  Set `orderStatus=CANCELLED`; leave COD as `PENDING` and record cancellation metadata. A cancelled
  order can never become delivered or collected.
- `adminStartPersonalCodDelivery`: mark the authenticated Admin as the delivery actor and transition
  an eligible confirmed/preparing order to `OUT_FOR_DELIVERY` atomically.
- `adminCompleteCodDelivery`: allow only when that Admin is the recorded personal-delivery actor.

## Delivery operations

- `deliveryGetAssignedCodOrders`: return only orders assigned to the authenticated active Delivery
  user. Do not accept a delivery-user UID from Android.
- `deliveryCompleteCodDelivery`: require the authenticated user to still be assigned to the order.

## Atomic delivery and collection

Both completion functions receive `collectedMinor` and `expectedRevision`. In one transaction:

1. Re-read the order and validate actor/client authorization and revision.
2. Require `paymentMethod=COD`, `orderStatus=OUT_FOR_DELIVERY`, and `paymentStatus=PENDING`.
3. Reject cancelled orders and duplicate completion.
4. Compare `collectedMinor` exactly with the immutable authoritative COD amount due.
5. Only on an exact match set `orderStatus=DELIVERED` and `paymentStatus=COLLECTED` together.
6. Store collector UID/role, collected amount, server timestamp, and audit entry.

Never mark only one of the two statuses if the transaction fails.

## Amount mismatch

`adminReportCodMismatch` and `deliveryReportCodMismatch` apply the same actor checks and store the
reported actual amount plus an audit event. They must leave the order `OUT_FOR_DELIVERY` and payment
`PENDING`; no finalized revenue is recognized. Resolution/refund/override requires a separate,
explicitly authorized future workflow—never silently alter the order total or mark it collected.

All mutation functions require Firebase Auth, App Check, active role profile, server-derived client
scope, optimistic revision checks, and server timestamps. Deny direct client writes to order status,
payment status, assignments, COD amounts, and collection metadata.
