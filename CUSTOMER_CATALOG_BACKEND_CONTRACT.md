# Customer Catalog Backend Contract

Phase 15 adds two callable Cloud Functions. Both require Firebase Authentication, valid App Check, an active `CUSTOMER` profile, and a server-resolved shop/customer identity.

## `customerGetProducts`

Accepts optional `categoryId`, bounded `search`, `filter` (`ALL`, `IN_STOCK`, or `OFFERS`), opaque `cursor`, and `pageSize` capped at 40. Return a bounded page of product card projections, `nextCursor`, current cart quantity, and authoritative `offersEnabled`.

- Never return inactive, archived, cross-shop, or inactive-category products. The UI still treats such data as unavailable defensively.
- Search and filters must be implemented with indexed queries or a maintained search projection, not full collection reads or client filtering.
- Cursors must encode the stable server sort and must not expose trusted authorization data.
- `OFFERS` is allowed only when offers are server-authorized. When disabled, perform no offer-specific query and return `offersEnabled = false`.
- Product projections include stock state, regular price, valid offer price, unit, category, and versioned image URLs. Prices use minor units.

## `customerAddToCart`

Accepts `productId` and bounded positive `quantity`. In a transaction, reload the product and category, reject inactive/archived/out-of-stock/cross-shop products, validate current stock and current authoritative price, then create or increment the authenticated customer's cart line. Return the authoritative total cart quantity.

Cart lines should retain the product reference and current display projection, but checkout must reload product availability, stock, price, offer validity, feature configuration, and delivery constraints. Client state is never authoritative. Use `OUT_OF_STOCK` or `PRODUCT_UNAVAILABLE` reason codes for safe UI feedback.

## Verification

Test unauthenticated, invalid App Check, inactive customer, cross-shop and non-CUSTOMER calls; category inactivity; stable pagination without duplicates; bounded search; disabled offers generating no offer read; concurrent cart increments; stock changes between listing and add; and inactive/archived/out-of-stock product rejection.
