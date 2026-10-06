# Product category backend and policy

Categories live at `shops/{shopId}/categories/{categoryId}` with `name`, `normalizedName`, `description`,
`imagePath`, `imageUrl`, `active`, `sortOrder`, `revision`, and server timestamps. Product documents reference a
stable `categoryId`. All callable functions enforce App Check, Firebase Authentication, active role, and a
server-derived tenant.

## Role access

- `adminGetCategories`: active ADMIN only; returns active and inactive categories with bounded `productCount`
  and `activeProductCount` metadata, sorted by `sortOrder`.
- `customerGetActiveCategories`: active CUSTOMER only; server filters `active == true`, applies sort order, and
  never returns inactive categories. Android repeats the active filter as defense in depth.
- Create, update, activation, image upload, and reorder endpoints require active ADMIN. Super Admin feature
  allowances do not grant category mutation to other roles.

## Mutations

- `adminCreateCategory`: validate normalized-name uniqueness transactionally, allocate the next sort order, and
  finalize an optional verified image upload token.
- `adminUpdateCategory`: validate category tenant, revision, name uniqueness, and optional new upload token.
- `adminSetCategoryActive`: activate normally. Deactivation is rejected with reason `ACTIVE_PRODUCTS_EXIST`
  while any active products reference the category. Inactive products may remain and still count toward the
  product limit; they become accessible again if moved or the category is reactivated.
- `adminUpdateCategoryOrder`: accept every category ID exactly once, reject missing/foreign/duplicate IDs, and
  update sequential sort values in a transaction or idempotent batch. Return the committed ordered list.

Categories are not deleted in this phase. A future deletion policy must preserve historical order line snapshots
and reject or migrate existing product references.

## Image upload

`adminBeginCategoryImageUpload` returns a short-lived single-use `uploadToken` and server-derived path matching
`shops/{shopId}/categories/uploads/{randomId}`. Storage Rules require an active ADMIN in that shop, image MIME
type, and a maximum size of 5 MiB. Create/update callables consume the token, verify path/owner/expiry/content,
move or copy the object to its final immutable location, generate the returned display URL, and mark the token
used. Never accept an arbitrary storage path or URL from Android. A scheduled cleanup removes expired orphan
uploads and replaced images after a safe retention period.

## Product relationships

Product create/update functions must verify the category exists, belongs to the same shop, and is active before
making a product customer-visible. Customer product queries must require both product and category activity.
Deactivating a category never silently rewrites product state.

## Required tests

Emulator tests must cover role and tenant isolation, inactive-category customer filtering, duplicate normalized
names, stale revisions, sort completeness/concurrency, active-product deactivation rejection, inactive products,
invalid/oversized/non-image uploads, expired/replayed upload tokens, orphan cleanup, and foreign category IDs.
