# Product Management Backend Contract

Phase 13 uses callable Cloud Functions for every product read and mutation. The Android app never writes product documents directly. Each callable must require Firebase Authentication, valid App Check, an active `ADMIN` profile, and derive `shopId` from that trusted profile rather than accepting it from the client.

## Callable functions

- `adminGetProducts`: accepts bounded `search`, `filter`, optional `categoryId`, opaque `cursor`, and `pageSize` (maximum 50). Returns a page, `nextCursor`, authoritative `countedProducts`, and `productLimit`. Search/filter indexes belong on the server.
- `adminBeginProductImageUpload`: creates a short-lived, single-use upload token and a random path under `shops/{shopId}/products/uploads/`. It returns no public write URL.
- `adminCreateProduct`: validates all fields, active category membership, image tokens, and the product limit in one transaction.
- `adminUpdateProduct`: checks `expectedRevision`, validates the active category, consumes new image tokens, and atomically replaces the mutable product projection.
- `adminSetProductActive`: checks `expectedRevision` and changes availability. Active and inactive products both count toward the limit.

All money is stored as integer minor units. Stock and thresholds must be finite and non-negative; offer price must be lower than regular price. Attributes are bounded to 20 unique keys. Product image count is bounded to five.

## Product-limit enforcement

Creation must run a Firestore transaction over the shop counter and server-authoritative `FeatureConfig.maxProducts`. Reject at the boundary with `PRODUCT_LIMIT_REACHED`; never use a client count. Concurrent attempts for the last slot must result in exactly one success. Activating or deactivating does not change the count. Archived products do not count, but archive/restore is intentionally outside Phase 13; a future restore must use the same transaction and limit check. Lowering a limit below the current count blocks new creation without deleting or disabling existing products.

## Images

The client downsizes images to at most 1600 px and JPEG quality 82 before uploading, with a 5 MiB hard limit. Storage Rules allow only active admins to create JPEG upload objects. A callable verifies ownership, size, type, expiry, and token reuse before moving/finalizing the object and persisting its served URL. Replaced and expired orphan uploads should be removed by a scheduled cleanup. Storage objects are not directly mutable or deletable by clients.

## Historical-order integrity

At checkout, each order line must embed an immutable snapshot containing `productId`, product name, unit, selected attributes, quantity, regular unit price, applied offer/discount/tax, final unit price, line total, and optionally the display image URL. Order totals and payment records use these snapshots. Product edits, price changes, stock changes, image replacement, deactivation, or later deletion must never rewrite historical order lines.

## Required backend verification

- Permission tests for unauthenticated, inactive, cross-shop, CUSTOMER, DELIVERY, and SUPER_ADMIN callers.
- App Check rejection tests and payload bounds.
- Transaction tests for concurrent creation at the final product slot.
- Revision-conflict, inactive-category, duplicate-name, invalid money/stock, expired-token, token-reuse, and excessive-image tests.
- Emulator tests proving product edits leave existing order snapshots and totals unchanged.

Deploy the callable implementations and `storage.rules` separately for each flavor's Firebase project before enabling this feature in production.
