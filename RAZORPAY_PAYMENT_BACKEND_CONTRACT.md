# Razorpay payment backend contract

The Android integration uses Razorpay Standard Checkout `1.6.41`. It does not preload or construct
the SDK until a server-created, currently enabled payment session is returned. The public Key ID is
provided dynamically; `key_secret` never leaves the backend secret manager.

## Feature and billing gate

Before any Razorpay-specific Android repository operation, refresh the general feature policy.
Razorpay is effective only when Super Admin allows it, Admin enables it, and backend configuration
is complete. If any condition is false:

- omit Razorpay from checkout;
- do not preload, construct, or open the SDK;
- do not call Razorpay-specific Cloud Functions;
- do not attach Razorpay payment resources or listeners through an SDK launch;
- backend Razorpay functions must reject before contacting Razorpay.

## Create and launch

`customerCreateRazorpayOrder(appOrderId, idempotencyKey)` must authenticate the customer, apply App
Check/client scope, re-read the application order, repeat the effective-feature gate, and validate
that its selected payment method is Razorpay and payment is not finalized. Create one Razorpay Order
server-side using the authoritative amount/currency. Persist an attempt before the external call and
bind the idempotency key to customer and application order. Retries return the same result.

Return only a public launch session: attempt ID, application order ID, Razorpay order ID, public Key
ID, authoritative amount/currency, and safe display/prefill fields. Never return the secret.

## Callback and verification

Android SDK success is evidence only. `customerVerifyRazorpayPayment` receives attempt ID, callback
ID, payment ID, Razorpay order ID, and signature. The backend must:

1. Deduplicate by callback ID and Razorpay payment ID.
2. Load its persisted Razorpay order ID; do not trust the callback order ID.
3. Verify `HMAC_SHA256(server_order_id + "|" + payment_id, key_secret)` in constant-time.
4. Fetch Razorpay payment status when needed and require captured/settled policy before setting
   application `paymentStatus=PAID`.
5. Update the attempt and application order transactionally and audit the transition.

Invalid or inconclusive callbacks become `FAILED` or `VERIFICATION_PENDING`, never `PAID`.

## Failure, cancellation, retry, and recovery

- `customerRecordRazorpayFailure` records SDK errors/cancellation idempotently. Cancellation becomes
  `CANCELLED`; it does not cancel an already captured payment discovered by webhook/API checks.
- Retry creates or returns a new allowed attempt using a new idempotency key and never mutates a
  paid attempt.
- On app restart, `customerReconcilePendingRazorpayPayment` runs only after the general policy says
  Razorpay is allowed. It reconciles persisted attempts using backend state, Razorpay API data, and
  webhooks. Network loss keeps status `VERIFICATION_PENDING` and suppresses unsafe duplicate pay.
- Duplicate or out-of-order callbacks/webhooks must be idempotent and monotonic: terminal `PAID`
  cannot regress to `FAILED` or `CANCELLED`.

## Webhook consistency

Validate Razorpay webhook signatures using the webhook secret and deduplicate event IDs. Handle at
least captured, failed, and refund events. Webhooks are authoritative asynchronous evidence;
user-facing verification can additionally fetch payment status. A reconciliation job should repair
missed deliveries and alert on conflicts without trusting Android state.

## Refund-ready state

Persist provider refund IDs, amount in minor units, reason, actor, idempotency key, timestamps, and
status separately from the original payment. Supported aggregate states are `REFUND_PENDING`,
`PARTIALLY_REFUNDED`, and `REFUNDED`. Refund creation is server-only and outside Phase 23 UI.

Direct client writes to payment attempts, payment status, provider IDs, signatures, refunds, and
webhook records must be denied.
