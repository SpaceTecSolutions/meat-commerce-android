# Delivery start contract

Assignment does not mean departure. A ready assigned order remains `PREPARING` until its assignee opens the order and explicitly chooses **Start Delivery**. **Navigate** opens the device map for the immutable delivery-address snapshot and does not mutate the order.

`adminStartAssignedDelivery` is available only when `adminDeliveringPersonally == true`. `deliveryStartAssignedOrder` is available only when `assignedDeliveryUserId` equals the authenticated active Delivery user's UID. Both callables transactionally verify shop ownership, `orderStatus == PREPARING`, ready timestamp, active assignment, and `expectedRevision`.

On success the backend writes:

```text
orderStatus: OUT_FOR_DELIVERY
deliveryStartedAtEpochMillis: server time
deliveryStartedByUserId: authenticated UID
revision: revision + 1
```

## Tracking lifecycle

The backend re-reads `realtimeTrackingAllowed`; it never trusts a client flag.

- When disabled, it stores `trackingLifecycle: DISABLED` and creates no tracking session or Realtime Database data. Delivery continues through status updates.
- When enabled, it creates an unguessable `trackingSessionId` and stores `trackingLifecycle: READY` in the order transaction. `READY` means authorized and prepared only.
- Starting an order does **not** initialize Firebase Realtime Database, request location permission, start a location service, or publish coordinates. A later tracking phase must explicitly activate `READY -> ACTIVE` after permission and service checks.
- Completion/cancellation eventually performs `READY|ACTIVE -> STOPPED`, revokes writes, and applies retention cleanup.

Firestore and Realtime Database Rules permit tracking reads/writes only for the active session's assigned actor and authorized customer/Admin, and reject unassigned users.
