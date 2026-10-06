# Admin order validation contract

The Admin app reads orders through `adminGetOrders`. The callable must require an authenticated, active `ADMIN` user and return only orders belonging to that Admin's shop. Each order response includes its immutable item snapshots, customer/address snapshot, totals, instructions, payment method/status, order status, and `revision`.

## Authoritative transitions

Allowed order transitions are:

```text
PENDING -> CONFIRMED | CANCELLED
CONFIRMED -> PREPARING | CANCELLED
PREPARING -> OUT_FOR_DELIVERY | CANCELLED
OUT_FOR_DELIVERY -> DELIVERED | CANCELLED
DELIVERED -> (terminal)
CANCELLED -> (terminal)
```

`adminConfirmCodOrder` performs `PENDING -> CONFIRMED`. `adminCancelCodOrder` performs rejection as `PENDING -> CANCELLED` and records the required reason, actor UID, and server timestamp.

Every mutation must run transactionally, compare `expectedRevision`, verify the current state and Admin/shop authorization, then increment `revision`. Invalid or stale transitions return `ABORTED` with `reason: INVALID_TRANSITION`. Firestore Security Rules must deny direct client writes to order state, payment state, revision, and audit fields.
