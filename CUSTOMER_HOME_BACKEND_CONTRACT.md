# Customer Home Backend Contract

Phase 14 loads the attraction screen through one callable Cloud Function: `customerGetHome`. It must require Firebase Authentication, valid App Check, an active `CUSTOMER` profile, and derive the shop/customer identity from trusted server data.

## Response

Return a bounded, display-ready snapshot containing:

- Delivery-location label only; do not return sensitive address fields not needed by this screen.
- Unread notification count and total cart quantity.
- Active promotional banners ordered by server-controlled priority and validity window.
- Active categories with available products.
- Bounded best-seller, recommended, new-product, and offer lists.
- Authoritative `offersEnabled`, resolved from Super Admin permission and shop configuration.

Product projections include only active, non-archived, in-stock products and the fields needed by cards. Offer products require an enabled offers feature and a currently valid offer price. When offers are disabled, the server must skip the offer query and return no offer payload; the client omits the section entirely.

## Query and performance policy

- Do not scan full collections. Use maintained ranking fields or server-produced aggregate collections with indexed, bounded queries.
- Cap each product section and banner/category list. Duplicate products may be removed server-side to improve variety.
- Use HTTPS cache validation where appropriate, but never cache one customer's cart, location, or notification values into another session.
- Image URLs should point to resized delivery assets where available. Storage metadata should include correct content types and long-lived cache headers for versioned objects.
- This endpoint creates no Realtime Database listener, tracking worker, payment request, or disabled-feature query.

## Authorization tests

Test unauthenticated, invalid App Check, inactive CUSTOMER, cross-shop, ADMIN, DELIVERY, and SUPER_ADMIN callers. Verify disabled offers perform no offer read and never appear in the response. Verify every returned product/category is active and belongs to the resolved shop, list bounds are enforced, and cart/notification counts belong to the authenticated customer.
