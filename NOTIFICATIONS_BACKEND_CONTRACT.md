# Notifications backend contract

FCM credentials and message creation belong only in trusted Cloud Functions. Android never selects a recipient, role, or event and never writes notification history directly.

## Callable functions

- `registerNotificationDevice({ token, platform })`: requires App Check and authentication. Store the token under the caller UID with its current server-resolved role, installation metadata, and `updatedAt`. A token moved between accounts must be detached from the old account.
- `unregisterNotificationDevice({ token })`: requires authentication and removes only the caller's token.
- `getNotificationHistory({ limit })`: returns only notifications whose `recipientUid` equals the caller UID, newest first, with a maximum limit of 100.
- `markNotificationRead({ notificationId })`: sets `readAt` only when the notification belongs to the caller.

Recommended paths:

```text
users/{uid}/notificationDevices/{tokenHash}
users/{uid}/notifications/{notificationId}
```

The raw token is server-only. Firestore client reads and writes for both paths should be denied because access is mediated by the callable functions.

## Authoritative event producers

Cloud Functions triggered by transactional order, payment, and assignment operations persist history first, then send FCM. Use an idempotency key such as `{event}:{aggregateId}:{stateRevision}` to prevent duplicate history and pushes.

- Customer: `ORDER_SUBMITTED`, `ORDER_CONFIRMED`, `ORDER_PREPARING`, `ORDER_OUT_FOR_DELIVERY`, `ORDER_DELIVERED`, `ORDER_CANCELLED`, `PAYMENT`.
- Admin: `NEW_ORDER`, `PAYMENT`, `OPERATIONAL`.
- Delivery: `ASSIGNMENT_CREATED`, `ASSIGNMENT_REMOVED`, `IMPORTANT_STATUS`.

Resolve recipients and their roles on the server. Never accept recipient UIDs, target roles, titles, totals, payment states, or order states from an Android notification request. Remove invalid/unregistered FCM tokens after permanent send failures. Payloads contain only display text, event, notification ID, and an optional authorized order ID; clients must re-fetch protected data before opening it.
