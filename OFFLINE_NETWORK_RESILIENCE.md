# Offline and network resilience

## Catalog and checkout

Customer Home keeps only an in-memory snapshot of public catalog sections. Private location, cart, and notification values are never copied into that fallback. A fallback is visibly marked stale and exposes Retry. Product price, stock, offers, delivery fees, payment availability, and feature configuration are always revalidated by `customerPrepareCheckout` and `customerPlaceOrder`; stale values are never authoritative.

Sensitive Firestore data is not intentionally made available through a cross-account disk cache. Firebase SDK reconnect behavior may be used for an active authenticated session, but privileged writes continue through Cloud Functions. Image caching remains safe because catalog media is public and immutable at its finalized Storage path.

## Order creation

The client creates one random order idempotency key per reviewed quote and retains it with `SavedStateHandle` while submission is unresolved. `customerPlaceOrder` must transactionally create at most one order for `(customerUid, idempotencyKey)` and return that same immutable result for every repeat request.

For `UNAVAILABLE` or `DEADLINE_EXCEEDED`, Android treats the outcome as unknown and calls `customerReconcileOrderCreation({ idempotencyKey })` before presenting a retry. Repeating Place Order uses the same key. The server must retain idempotency records longer than the maximum client retry/recovery period and bind them to the authenticated customer and normalized request hash. A reused key with different input must fail.

## Payments

Payment order creation, retries, callbacks, and verification remain server-idempotent. A retry key is retained until a retry session is returned. Timeout or network loss never changes payment state locally; Android reconciles the pending attempt. Webhooks and secure verification are authoritative, and a new charge is permitted only after the server marks the previous attempt terminal and `retryAllowed`.

## Live tracking

The foreground service uses a conflated location channel: while a write is waiting for RTDB reconnection, only the latest additional coordinate is retained. Firebase RTDB listeners reconnect automatically. Every service restart re-authorizes the order, assignment, and feature gates. Delivery completion disables server authorization, removes the active node, closes pending coordinates, and prevents delayed writes through RTDB Rules.

## Retry policy

Read-only operations expose manual Retry and distinguish network/timeouts. Do not automatically retry non-idempotent mutations. Safe automatic recovery is restricted to reconciliation calls and requests with a server-enforced idempotency key. Use bounded exponential backoff with jitter in Cloud Functions integrations; never retry authentication, validation, forbidden, cancelled, or definitive payment failures as network errors.
