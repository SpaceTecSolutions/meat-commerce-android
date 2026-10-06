# Admin reports backend contract

`adminGetReport` is an authenticated, App Check-protected callable function. It accepts only `period`, `startDate`, and `endDate`; it must derive the Admin's business/shop identity from the verified user profile or custom claims. The client must never submit a business ID.

Only active `ADMIN` users may call it. Every order, customer, and product query must include the derived business scope. Do not use collection-wide aggregates that can include another white-label tenant or shop.

Supported periods are `WEEKLY`, `CURRENT_MONTH_BY_WEEK`, `MONTHLY`, `YEARLY`, and `CUSTOM`. Validate ISO dates, ensure start is not after end, apply the business timezone, and limit custom ranges to 366 inclusive days.

The response contains `periodLabel`, `currencyCode`, `revenueMinor`, `orders`, `customers`, `products`, `activeProducts`, and optional `revenueSeries`. Revenue must use the established finalized-revenue policy: delivered online orders with `PAID`, and delivered COD orders with `COLLECTED`. Pending, failed, rejected, and cancelled orders contribute no revenue.

This callable is read-only. It cannot write or return mutation handles for `FeatureConfig`, Super Admin settings, product limits, or payment/delivery feature controls.
