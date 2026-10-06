# Admin order processing contract

Order processing uses callable Cloud Functions only. Direct client writes to order state, revision, processing timestamps, and actor fields are denied by Firestore Security Rules.

## Processing transitions

`adminStartPreparingOrder` performs `CONFIRMED -> PREPARING`. In the same transaction it validates an authenticated active Admin for the order's shop, compares `expectedRevision`, then writes:

```text
orderStatus: PREPARING
preparingAtEpochMillis: server time
preparingByAdminId: authenticated Admin UID
revision: revision + 1
```

`adminMarkOrderReadyForDelivery` requires `orderStatus == PREPARING` and no existing ready timestamp. It does not introduce another customer-visible order status. It writes:

```text
readyForDeliveryAtEpochMillis: server time
readyForDeliveryByAdminId: authenticated Admin UID
revision: revision + 1
```

Only orders with this ready milestone can enter delivery assignment. Assignment later performs `PREPARING -> OUT_FOR_DELIVERY` atomically.

Confirmation similarly records `confirmedAtEpochMillis` and `confirmedByAdminId` using server values.

## Cancellation rules

- A nonblank reason is mandatory and retained in the audit trail.
- `DELIVERED` and `CANCELLED` are terminal and cannot be cancelled.
- Pending, Confirmed, Preparing, and Out For Delivery orders may be cancelled only by an authorized Admin using a transaction and the expected revision.
- Cancelling a ready order releases any delivery reservation atomically.
- COD cancellation leaves no collectible balance. A verified online payment moves to `REFUND_PENDING`; it must never be silently marked refunded.
- Cancellation records server timestamp, acting Admin UID, reason, previous state, and increments revision.
- Invalid/stale transitions return `ABORTED` with `reason: INVALID_TRANSITION`.
