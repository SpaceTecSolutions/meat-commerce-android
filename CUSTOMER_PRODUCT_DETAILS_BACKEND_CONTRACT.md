# Customer Product Details Backend Contract

Phase 16 adds `customerGetProduct`. The callable requires Firebase Authentication, valid App Check, an active `CUSTOMER`, and a server-resolved shop identity. It accepts only `productId`.

Return a complete display projection: versioned image URLs, title, description, regular and currently valid offer price in minor units, unit/weight, bounded attributes, stock quantity, active/archive state, and category identity. Reject cross-shop, archived, inactive-product, and inactive-category access with `PRODUCT_UNAVAILABLE`. Do not expose supplier costs, internal margins, Storage upload paths, or administrative metadata.

The detail response is informational. Both Add to Cart and Buy Now use the existing transactional `customerAddToCart`, passing the selected bounded quantity. That callable must reload product/category state, stock, price, and offer validity. Buy Now opens the cart only after a successful mutation; checkout remains a later phase and must revalidate again.

Use versioned, cacheable resized product images for the gallery. Product reads must be single-document/indexed reads and must not attach long-lived listeners. Test invalid App Check, inactive customer, cross-shop IDs, inactive/archived products, inactive categories, stock changes, expired offers, excessive attributes/images, and cart quantities greater than available stock.
