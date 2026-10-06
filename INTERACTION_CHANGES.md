# Customer interaction updates

## Deployment still required

- Compile-only changes; no APK or Firebase deployment is performed.
- Deploy `clearAllNotifications` and all notification producers using the updated shared `notification-core`.
- Deploy `firestore.indexes.json` to the configured **named `default` database** to enable TTL on `notifications.expiresAt`.
- New notification records expire three days after creation. TTL deletion is asynchronous, not an exact three-day UI timer. Orders are never deleted by this policy.
- Existing records need expiration backfill: with authorized Application Default Credentials and `GOOGLE_CLOUD_PROJECT` set, run `node scripts/backfill-notification-expiry.cjs` from `functions` for a read-only count. Review it before rerunning with `--apply`. The migration has NOT been run.
- Clear all is authenticated/App Check protected, only deletes the caller's notification history, and preserves messages arriving after the operation started. It processes up to 8,000 records per invocation; repeat if an unusually large history remains.
- TTL and explicit deletes incur Firestore operations; retention reduces accumulated storage, not all notification costs. See https://firebase.google.com/docs/firestore/ttl.

## Behavior

- Cold-start notification navigation keeps the role dashboard as the root. Consumed notification intents are cleared; Home does not restore a nested saved stack.
- Product details uses existing cart quantity state to show View Cart after successful addition. Search/category/product internal back handling precedes root back navigation.
- Orders retain live observation, add pull-to-refresh and a local creation-date filter. Invoice and Report issue are explicitly marked coming soon; no PDF or ticket is generated.
- Address form remains full-screen with map preview, search and current-location control. Search uses existing Places integration and GPS uses the existing permission/repository flow. Map pins can be tapped or dragged and must be confirmed.
- Address and cart swipe actions reuse existing confirmation and deletion paths, with an accessibility Delete action.
- Notification cards have subtle elevation; Clear all asks for confirmation. Firestore observers update history/unread badges after deletion.

## Device checks still required

Verification completed: `:app:compileMeatBushDebugKotlin` passed, `functions` TypeScript compilation passed, and the backfill script passed `node --check`. No APK assembled, backend deployed, or device interaction tests performed.

Cold-start notification → details → Back → Orders → Dashboard; category/product back; Add → View Cart; swipe cancel/confirm; date filter and pull refresh; GPS denied/granted; place search and pin confirmation; notification clear and badge update.
