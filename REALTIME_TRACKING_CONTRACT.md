# Realtime delivery tracking contract

Tracking activates only when the Super Admin allows realtime tracking, the Admin enables it, the order is `OUT_FOR_DELIVERY`, its lifecycle/session is `READY`, and the authenticated assigned Admin/Delivery user started that delivery. Android checks cached/configured state to avoid unnecessary work; `activateDeliveryTracking` re-reads every condition server-side and is authoritative.

## Runtime lifecycle

1. The assignee starts delivery and receives an opaque session in `READY`.
2. Only then does Android request fine location (and notification permission where applicable).
3. After permission, `activateDeliveryTracking` creates the RTDB authorization using Admin SDK and changes the order lifecycle to `ACTIVE` idempotently.
4. A location foreground service starts. It requests fused updates every 10 seconds and publishes at most when 15 seconds elapsed or movement is at least 25 metres.
5. The service survives app backgrounding. For process restoration it stores only order/session/actor locally and re-runs backend activation before restarting GPS or writes.
6. Firebase listeners reconnect automatically and are removed when their Flow collector is cancelled.
7. Delivery completion calls `stopDeliveryTracking`, removes `activeDeliveryTracking/{sessionId}`, stops fused updates and the foreground service, and customer collectors must be cancelled. The backend also marks authorization inactive and the order lifecycle `STOPPED`.

When either feature switch is off, Android does not request permissions, start a service, access GPS, instantiate RTDB through the lazy accessor, attach a listener, schedule a worker, or write coordinates.

## RTDB data

`trackingAuthorizations/{sessionId}` is backend-only metadata containing the assigned writer UID, customer UID, order ID, and active flag. `activeDeliveryTracking/{sessionId}/location` contains only the latest temporary coordinate. Deploy `database.rules.json`; clients cannot create or edit authorization records.

Required callable functions:

- `activateDeliveryTracking`: validates identity, assignment, active user, both feature switches, `OUT_FOR_DELIVERY`, and session; creates/refreshes RTDB authorization and returns idempotently.
- `stopDeliveryTracking`: validates an authorized participant or trusted order-completion path, marks authorization inactive, removes the active node with Admin SDK, and writes lifecycle `STOPPED`.

RTDB server cleanup is mandatory even when the Android process is killed or offline at delivery completion. The Android cleanup is defense in depth, not the sole cleanup mechanism.
