# Product limit policy and backend enforcement

## Counting behavior

- Active products count toward `maxProducts`.
- Inactive products also count. Inactive is a merchandising state and the product can be re-enabled without a
  capacity transition.
- Archived products do not count. Archiving is the explicit way to release a product slot.
- Restoring an archived product consumes a slot and is rejected when the limit is reached.
- Lowering `maxProducts` below the current counted total is rejected. The Super Admin must archive enough
  products first. This keeps the invariant `countedProducts <= maxProducts` true at all times.

## Authoritative storage

Store `maxProducts` and `revision` in `shops/{shopId}/config/features`. Maintain the authoritative
`countedProducts` value in `shops/{shopId}/counters/products`. The counter includes every non-archived product,
regardless of active state. Android displays this server-returned counter; it does not derive enforcement from a
downloaded product list.

## Required callables

### `superAdminGetProductLimitStatus`

Require App Check, an authenticated active `SUPER_ADMIN`, and a server-derived tenant. Read the feature config
and counter and return `countedProducts`, `maxProducts`, and config `revision`.

### `superAdminUpdateProductLimit`

Input: `maxProducts`, `expectedRevision`. Require the same Super Admin authorization. In one Firestore
transaction, read the config and product counter, reject a stale revision with `ABORTED`, reject values outside
`0..100000`, and reject values below `countedProducts` with `FAILED_PRECONDITION` plus reason
`LIMIT_BELOW_CURRENT_COUNT`. Update the limit, increment revision, set a server timestamp, and append an audit
record. The general `superAdminUpdateFeatureConfig` endpoint must apply this identical count check.

## Admin product mutation enforcement

Product creation, archive, restore, and permanent deletion must use authorized backend functions. Direct client
writes are denied. Each function derives the Admin's tenant and validates active role and relevant feature
configuration.

- Create: transactionally read config and counter; reject when `countedProducts >= maxProducts`; create the
  non-archived product and increment the counter.
- Active/inactive change: do not change the counter.
- Archive: atomically transition a non-archived product and decrement the counter exactly once.
- Restore: enforce capacity, transition an archived product, and increment the counter exactly once.
- Permanent deletion: allowed only under the eventual deletion policy; decrement only if deleting a counted
  non-archived product.

Use idempotency keys and verify the previous archived state so retries cannot double-increment or decrement.
Return `RESOURCE_EXHAUSTED` with reason `PRODUCT_LIMIT_REACHED` when capacity is unavailable. A periodic trusted
reconciliation job should compare the counter with an aggregate count of `archived == false`, alert on drift,
and repair only with an audit entry.

## Required tests

Emulator tests for every flavor must cover the exact limit boundary, limit zero, inactive counting, archive and
restore transitions, double-submit idempotency, concurrent creates for the final slot, lowering below count,
stale revisions, counter drift detection, cross-tenant requests, and non-Super-Admin limit changes.
