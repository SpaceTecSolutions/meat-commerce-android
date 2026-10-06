# Delivery assignment contract

`Deliver Myself` is available for every ready, unassigned order regardless of the Delivery Staff feature flag. `Assign Delivery User` is available only while `deliveryStaffManagementAllowed` is true.

## Atomic assignment

`adminAssignOrderToSelf` and `adminAssignDeliveryUser` run Firestore transactions. Both require:

- authenticated active `ADMIN` for the order's shop;
- `orderStatus == PREPARING` and a ready-for-delivery timestamp;
- matching `expectedRevision`;
- no `assignedDeliveryUserId` and `adminDeliveringPersonally == false`.

Staff assignment additionally re-reads the target user in the transaction and verifies `role == DELIVERY`, matching shop, and `active == true`. It also revalidates the server feature flag. Client filtering is convenience only.

On success, the transaction writes exactly one assignment form, records `assignedAtEpochMillis` from server time and `assignedByAdminId`, and increments revision. The ready order remains `PREPARING` until the assignee explicitly starts delivery. Staff assignment stores the Delivery UID and name snapshot; self-assignment stores `adminDeliveringPersonally: true` with no Delivery UID.

A stale or existing assignment returns `ABORTED` with `reason: ALREADY_ASSIGNED`. An inactive target returns `FAILED_PRECONDITION` with `reason: DELIVERY_USER_INACTIVE`.

## Delivery access

Firestore Security Rules deny Delivery users direct access to the general orders collection. `deliveryGetAssignedCodOrders` returns an order only when the authenticated UID equals its active `assignedDeliveryUserId`, the user remains active, and both belong to the same shop. Delivery mutation callables repeat this check transactionally. Self-delivered and unassigned orders are never returned to Delivery users.
