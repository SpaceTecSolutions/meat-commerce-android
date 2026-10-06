# Admin dashboard backend contract

## `adminGetDashboard`

This App Check-protected callable requires Firebase Authentication and a trusted `users/{uid}` profile with
`role == ADMIN` and `active == true`. Derive the shop from that profile; reject Super Admin, Delivery, Customer,
inactive, and cross-tenant requests. Return only bounded aggregate values for the caller's shop.

All date boundaries use the shop's configured time zone:

- Today revenue: finalized revenue recognized during the current local day.
- Month revenue: finalized revenue recognized during the current local calendar month.
- Today's orders: orders created during the current local day, regardless of status.
- Pending, Preparing, Out for Delivery: current operational backlog in each exact order state.
- Delivered: orders delivered during the current local day.
- Customers: current CUSTOMER profile count for the tenant.
- Products: current non-archived product counter from `shops/{shopId}/counters/products`.
- Product limit: authoritative `maxProducts` from the feature configuration.
- Low stock: optional. Return `lowStockProducts` only after inventory quantities and the shop's low-stock
  threshold are implemented. The Android card is omitted when this field is absent.

Revenue follows `REPORTS_BACKEND_CONTRACT.md`: only delivered orders with online `PAID` or COD `COLLECTED`
payment count. Pending or cancelled orders never contribute revenue.

Use trusted idempotent daily/monthly rollups rather than unbounded collection downloads. Reconcile rollups
against authoritative orders and audit repairs.

## `getAllowedFeatureConfig`

This shared read callable replaces the Super Admin-only read name. It permits any authenticated active tenant
role to read that tenant's allowance booleans and product limit. It accepts no shop ID and performs no mutation.
Only `superAdminUpdateFeatureConfig` may write allowances. Android stores one DataStore snapshot and shares one
Hilt-singleton `StateFlow`, preventing dashboard and feature screens from issuing separate repeated reads.

## Dynamic UI and disabled capabilities

The dashboard composes capability labels only for allowances returned as true. It does not expose switches,
configuration controls, or a Feature Management route. Disabled capabilities do not start listeners, workers,
tracking, payment SDK flows, or feature-specific backend calls.

## Required tests

Emulator tests must cover exact role authorization, inactive Admins, tenant isolation, shop-time-zone boundaries,
finalized revenue states, operational status counts, product counter/limit consistency, nullable low-stock data,
and feature-config reads without write authority.
