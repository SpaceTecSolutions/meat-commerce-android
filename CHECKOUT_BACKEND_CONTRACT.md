# Checkout backend contract

Phase 21 treats every Android amount as display-only. Order creation must be authoritative and
transactional on the backend.

## `customerPrepareCheckout`

Require Firebase Auth, valid App Check, an active `CUSTOMER` profile, and matching client. Then:

1. Read the customer's server-owned cart.
2. Read every referenced product and reject inactive, archived, or missing products.
3. Revalidate stock and permitted quantity.
4. Recalculate current regular/offer price and discounts.
5. Read current delivery configuration; validate minimum order and recalculate delivery charge and
   free-delivery threshold.
6. Read current Super Admin feature policy and Admin payment configuration. Return only methods
   effective under both layers. Razorpay requires backend configuration.
7. Return customer-owned addresses, effective delivery options/slots, authoritative cart summary,
   expiry, and a short-lived opaque signed `quoteToken`.

Do not accept product names, prices, discounts, charges, totals, feature flags, configuration state,
or address ownership from Android.

## `customerPlaceOrder`

Accept only `quoteToken`, `addressId`, delivery option/slot ID, instructions, payment method, and a
customer-generated idempotency key. Inside a Firestore transaction or equivalently safe backend
workflow, repeat every validation listed above immediately before creation. Also:

- Verify the address belongs to the authenticated customer.
- Verify the selected slot is active and scheduled delivery is still allowed and enabled.
- Verify the payment method remains allowed and enabled. Razorpay additionally requires secure
  backend configuration; disabled Razorpay must not trigger any Razorpay API call.
- Recalculate the final total and persist immutable product, address, pricing, delivery, and payment
  snapshots on the order so later catalog changes cannot alter history.
- Atomically reserve/decrement stock, create the order, and clear/update the cart.
- Bind the idempotency key to customer UID and return the original result for safe retries.
- Use server timestamps and initialize separate `OrderStatus` and `PaymentStatus` fields.

If anything changed since review, return a typed reason such as `PRICE_CHANGED`, `STOCK_CHANGED`,
`DELIVERY_CHANGED`, `PAYMENT_UNAVAILABLE`, or `FEATURE_CHANGED`; do not create a partial order.

For Razorpay, contact Razorpay only after all validations succeed and the selected method is
effective. Keep `key_secret` exclusively in the Cloud Functions secret manager. A client callback
is never proof of payment; verification remains server-side.
