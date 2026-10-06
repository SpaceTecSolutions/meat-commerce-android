# Customer Cart Backend Contract

Phase 17 stores each customer's cart server-side so it persists across app restarts and devices. The Android badge is only a session projection; opening Home, Catalog, or Cart refreshes it from an authoritative response.

All cart callables require Firebase Authentication, valid App Check, an active `CUSTOMER`, and server-derived shop/customer identities:

- `customerGetCart`
- `customerSetCartQuantity`
- `customerRemoveCartItem`
- Existing `customerAddToCart`

## Authoritative response

Every operation returns the complete recalculated cart: bounded lines, current product projection, quantity and maximum quantity, availability, current regular/effective unit prices, subtotal, discount, delivery fee, total, delivery estimate, and any adjustments. Money uses integer minor units.

The backend must reload product, category, stock, valid offer, shop delivery settings, and enabled feature configuration. It removes or marks unavailable products, reduces quantities that exceed stock, and reports `PRICE_CHANGED`, `QUANTITY_REDUCED`, `OUT_OF_STOCK`, or `UNAVAILABLE` adjustments. Never calculate trusted totals from prices sent by Android.

Mutations must be transactional or otherwise concurrency-safe, idempotent where retried, bounded to quantities 1–99, and scoped to the authenticated customer's cart. Removing a line deletes only that customer's line. Maintain a server-side cart revision if needed for checkout conflict detection.

## Checkout boundary

Phase 17 does not create an order. A later checkout callable must reload and lock/revalidate the cart again immediately before order creation. Stale cart totals, Android-displayed prices, and Android payment callbacks must never be trusted as proof of the final amount.

## Verification

Test cross-user/shop access, invalid App Check, inactive/non-CUSTOMER callers, concurrent quantity mutations, product/category deactivation, archive, price/offer expiry, insufficient stock, delivery-fee changes, duplicate retries, badge totals, and persistence across sessions/devices.
