# Super Admin reports and finalized-revenue policy

Reports are returned by the App Check-protected callable `superAdminGetReport`. The function requires an
authenticated, active `SUPER_ADMIN`, derives the tenant and shop time zone from trusted profiles/configuration,
and never accepts a client-supplied shop ID.

## Period definitions

All boundaries are half-open `[start, end)` instants calculated in the shop's configured time zone.

- `WEEKLY`: current calendar week, grouped by local day.
- `CURRENT_MONTH_BY_WEEK`: current calendar month, grouped into local calendar-week buckets.
- `MONTHLY`: current calendar month, grouped by local day.
- `YEARLY`: current calendar year, grouped by month.
- `CUSTOM`: inclusive client-supplied ISO local dates, converted server-side to a half-open instant range and
  limited to 366 days. Group by day, week, or month according to range length.

## Revenue eligibility

Revenue is recognized only when both conditions are true:

```text
order.status == DELIVERED
payment.status == PAID       // online payment
OR
payment.status == COLLECTED  // cash on delivery collected
```

Pending, confirmed, preparing, out-for-delivery, cancelled, payment-pending, processing, failed, refund-pending,
refunded, and partially-refunded records contribute zero finalized revenue. A paid online order that has not
been delivered also contributes zero. Revenue uses the immutable finalized order total in minor currency units,
not an Android-calculated cart amount. COD is recognized only after trusted collection confirmation.

## Metric definitions

- Revenue: sum of eligible finalized order totals recognized in the range.
- Orders: all tenant orders created in the range, including pending and cancelled; this is volume, not revenue.
- Delivered: orders whose trusted delivered timestamp falls in the range.
- Cancelled: orders whose trusted cancelled timestamp falls in the range.
- Average order value: finalized revenue divided by count of revenue-eligible orders; zero when none qualify.
- Customers: customer profiles existing at the range end.
- New customers: customer profiles created in the range.
- Active customers: distinct customers placing at least one non-cancelled order in the range.
- Best selling products: quantity from line items belonging only to revenue-eligible orders, ranked by quantity
  with finalized allocated line revenue as the secondary sort.

## Callable request and response

Input contains `period`; custom reports also contain `startDate` and `endDate` as `YYYY-MM-DD`. Return a report
with `currencyCode`, all metrics in integers, monetary values in minor units, bounded revenue-series points, and
at most ten best-selling products. Product names are stored order-line snapshots so archived or renamed products
remain historically correct.

## Performance and integrity

Do not download full collections to Android. Maintain trusted daily reporting rollups and distinct-customer
materializations from backend-controlled order/payment state transitions. Reads should be bounded by tenant and
date. State transitions must be idempotent so retries do not double-count revenue. Run a scheduled reconciliation
against authoritative orders, alert on drift, and audit repairs. All timestamp, order, payment, COD collection,
refund, and line-total updates require trusted functions or restrictive rules.

## Required tests

Emulator tests must cover every order/payment state combination, paid-but-pending orders, cancelled paid orders,
COD before and after collection, refunds, duplicate transition delivery, shop-time-zone boundaries, custom-range
limits, cross-tenant access, inactive/non-Super-Admin callers, average value with zero orders, and archived product
name snapshots.
