# Notification architecture

## Flow and trust boundary

Order, payment, inventory, assignment, or feature changes are observed by Cloud Functions. The backend resolves recipients, creates one deterministic notification-history document, then sends FCM to every active device belonging to that user. Android never holds Admin SDK credentials and cannot create trusted notifications.

Push failure never rolls back the originating business operation. Permanent invalid-token responses deactivate the affected device record.

## Firestore schema

`users/{uid}/notifications/{sha256(eventKey)}` contains `recipientUid`, `recipientRole`, `event`, `category`, `priority`, `title`, `body`, optional `shopId`, `orderId`, `productId`, `reportPeriod`, `deepLinkRoute` and `metadata`, plus `createdAt` and nullable `readAt`.

`users/{uid}/devices/{sha256(fcmToken)}` contains the server-only token, platform, resolved role/shop, `isActive`, `createdAt`, and `updatedAt`. More than one active device is supported. Logout deactivates the current token; refresh/reinstall registers the replacement. Invalid FCM tokens are deactivated automatically.

Clients may read only their own notification history. All history/device writes are performed through authenticated, App Check-protected callable functions.

## Event mapping

- Customer: order placed/confirmed/preparing/out-for-delivery/delivered/cancelled, derived delivery delay, and verified payment changes.
- Admin: new orders, customer cancellations, verified payment receipt, low-stock threshold crossings, and delivery/collection issues.
- Delivery: assignment, reassignment/removal, assignment cancellation, and delivery-start reminders.
- Super Admin: feature configuration changes plus weekly and monthly business summaries.

Low-stock alerts fire only on a crossing from above to at/below the threshold; replenishing above the threshold makes a future crossing eligible. Delay, reminder, report, order and payment notifications use deterministic event keys, so trigger retries do not duplicate history or pushes.

## Android

`MeatBushMessagingService` registers refreshed tokens and handles foreground/data messages. Channels are `orders`, `payments`, `delivery`, `promotions`, and `system_reports`. Android 13 notification permission is requested contextually from the inbox; denying it does not disable the Firestore inbox.

Notification taps carry an allow-listed role route. Root authentication and `RoleNavigationRegistry` validate the route before navigation. Opening a system or inbox notification marks it read without blocking navigation.

## Scheduling

- Delayed-order detection: every 15 minutes.
- Delivery-start reminders: every 15 minutes, within 45 minutes of the slot.
- Weekly Super Admin summary: Monday 09:30 Asia/Kolkata for the previous seven days.
- Monthly Super Admin summary: first day 09:00 Asia/Kolkata for the previous calendar month.

Revenue includes only delivered orders whose payment status is `PAID` or `COLLECTED`. Reports calculate real revenue, order counts, delivery/cancellation/pending counts, customers, average order value, and top product.

## Deployment

Enable Cloud Messaging and a billing plan that supports scheduled functions. Deploy Functions, Firestore rules and indexes from the project root. App Check enforcement is enabled on notification callables; register debug tokens for development devices and use Play Integrity for production. Test-mode and live Firebase projects require their own `google-services.json`, App Check registration, and FCM token registrations.
