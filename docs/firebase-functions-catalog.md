# Firebase Functions Catalog

This document describes the Firebase Functions exported by `functions/src/index.ts`.
It is intended as the deployment and operations reference for the MeatBush white-label
backend and its MeatStation client project.

## Inventory summary

| Item | Count |
|---|---:|
| Total exported functions | **126** |
| Non-Razorpay functions | **118** |
| Razorpay functions | **8** |
| Callable functions (`onCall`) | **115** |
| Firestore event functions | **6** |
| Scheduled functions | **4** |
| HTTP webhook functions | **1** |

All functions currently use the `asia-south1` region. Callable functions enforce
Firebase App Check. Authenticated operations additionally validate the active user
profile and role in Firestore; UI visibility is not treated as authorization.

## Shared runtime conventions

- **Firestore database:** `(default)` / database ID `default`.
- **Default shop:** `shops/default`. Guest catalog calls require this document to
  exist and not have `active: false`.
- **Customer:** active `users/{uid}` profile with role `CUSTOMER` and no pending deletion.
- **Admin:** active profile with role `ADMIN`; data is scoped to its `shopId`.
- **Staff:** active profile with role `STAFF` plus the named permission.
- **Super Admin:** active profile with role `SUPER_ADMIN`.
- **Delivery:** active `DELIVERY` profile assigned to the relevant order, or the
  assigned Admin when the Admin delivers personally.
- **App Check:** debug builds require an allowlisted debug token. Release builds use
  Play Integrity.
- **Optimistic concurrency:** mutable business records commonly require an
  `expectedRevision`; stale updates are rejected instead of silently overwriting data.

## Required secrets

| Secret | Used by | Purpose |
|---|---|---|
| `AUTH_LOOKUP_HMAC_KEY` | Authentication, Admin creation and Staff management | Produces private deterministic lookup IDs for mobile numbers and auth flow tokens. Must remain stable. |
| `IDENTITY_TOOLKIT_API_KEY` | Password sign-in and Admin password change | Calls Firebase Identity Toolkit password verification APIs. |
| `DELIVERY_OTP_KEY` | Delivery OTP and tracking activation | Signs the short customer delivery-verification code. Must remain stable during active deliveries. |
| `MAPS_ROUTES_API_KEY` | `customerGetTrackingRoute` | Calls the Google Routes API from the trusted backend. |
| `RAZORPAY_KEY_ID` | Razorpay callable functions | Razorpay merchant key identifier. |
| `RAZORPAY_KEY_SECRET` | Razorpay callable functions | Signs and authenticates Razorpay server requests. |
| `RAZORPAY_WEBHOOK_SECRET` | `razorpayWebhook` | Verifies Razorpay webhook signatures. |

Razorpay is currently disabled for MeatStation. Its eight functions should remain
excluded from deployment until genuine matching credentials are installed and the
feature is deliberately enabled.

## Authentication — 6 functions

| Function | Access | Purpose and usage |
|---|---|---|
| `authSignInWithMobilePassword` | Public callable + App Check | Resolves the HMAC mobile lookup, verifies the password through Identity Toolkit, checks the active profile, and returns a role-bearing Firebase custom token. Used by legacy/password login. |
| `authVerifyCustomerRegistration` | Public callable + verified Firebase phone token | Determines whether a verified mobile already has an account; otherwise issues a short-lived registration token. |
| `authSignInCustomerWithPhone` | Public callable + verified Firebase phone token | Shared OTP login for Customer, Admin, Super Admin, and Staff. Uses the trusted profile role, repairs eligible legacy lookup records, and creates a new Customer profile only when no trusted account exists. |
| `authRegisterCustomer` | Public callable + registration token | Creates the Firebase Auth user, private mobile lookup, Customer profile, claims, and custom token for the older explicit registration flow. |
| `authVerifyPasswordResetCode` | Public callable + verified Firebase phone token | Validates phone ownership and issues a short-lived password-reset token for an existing active account. |
| `authResetPassword` | Public callable + reset token | Updates the Firebase Auth password, consumes the reset token, and revokes existing refresh tokens. |

## Feature and tenant administration — 13 functions

| Function | Access | Purpose and usage |
|---|---|---|
| `getAllowedFeatureConfig` | Public callable + App Check | Returns public feature availability with safe defaults; used during app startup and role-specific feature gating. |
| `superAdminUpdateFeatureConfig` | Super Admin | Atomically updates global feature flags, public notification projection, revision, and audit log. |
| `superAdminGetProductLimitStatus` | Super Admin | Reads the current global product-limit configuration and usage status. |
| `superAdminUpdateProductLimit` | Super Admin | Changes the maximum product limit with revision protection and audit logging. |
| `superAdminGetAuditLog` | Super Admin | Returns paged/filterable system audit activity for the Super Admin audit screen. |
| `superAdminListAdmins` | Super Admin | Lists Admin accounts and their active/shop state. |
| `superAdminCreateAdmin` | Super Admin | Creates an Admin Auth account, private mobile lookup, profile/claims, and audit entry. |
| `superAdminUpdateAdmin` | Super Admin | Updates an Admin's trusted profile information and related authentication metadata. |
| `superAdminSetAdminActive` | Super Admin | Enables/disables an Admin account and synchronizes Auth/profile state. |
| `adminGetPaymentConfig` | Admin | Reads the shop payment configuration shown in Admin settings. |
| `adminUpdatePaymentConfig` | Admin | Updates shop payment settings with revision protection; COD remains mandatory according to server validation. |
| `adminGetDeliveryConfig` | Admin | Reads delivery fees, minimums, schedules, and tracking configuration. |
| `adminUpdateDeliveryConfig` | Admin | Updates validated delivery configuration with revision protection and mandatory scheduled-delivery rules. |

## Admin profile, dashboard, customers and reports — 13 functions

| Function | Access | Purpose and usage |
|---|---|---|
| `adminGetDashboard` | Admin | Produces dashboard totals, pending count, unread count, revenue series, and top three selling products for the selected period. |
| `adminGetShopSettings` | Admin | Loads the authenticated Admin identity and shop/support/application settings. |
| `adminUpdateProfile` | Admin | Updates the Admin's display name/profile fields and writes an audit entry. |
| `adminSaveShopSettings` | Admin | Saves SHOP, APP, or SUPPORT settings with revision conflict protection and banner-edit permission checks. |
| `adminChangePassword` | Admin | Verifies the current password through Identity Toolkit before changing the Firebase Auth password. |
| `adminGetCustomers` | Admin | Lists Customers belonging to the Admin's shop. |
| `adminGetCustomerDetails` | Admin | Returns one Customer's details and summarized/recent order statistics. |
| `adminSetCustomerActive` | Admin | Activates/deactivates a Customer profile with revision/audit handling. |
| `adminGetReport` | Admin | Builds shop-scoped revenue, order, customer, trend, and top-product reporting for weekly/monthly/yearly/custom ranges. |
| `superAdminGetReport` | Super Admin | Builds cross-shop reporting, revenue series, customer metrics, and best-selling products. |
| `adminListStaff` | Admin; staff feature must be enabled | Lists Staff/Delivery accounts for the Admin's shop. |
| `adminCreateStaff` | Admin; `AUTH_LOOKUP_HMAC_KEY` | Creates a Staff/Delivery Auth user, mobile lookup, role, shop, permissions, and claims. |
| `adminUpdateStaff` | Admin; `AUTH_LOOKUP_HMAC_KEY` | Updates Staff/Delivery identity, activity, permissions, claims, and lookup state. |

## Admin order lifecycle — 10 functions

| Function | Access | Purpose and usage |
|---|---|---|
| `adminGetOrders` | Admin or Staff with `VIEW_ORDERS` | Loads up to 250 shop orders and enriches missing item category snapshots. |
| `adminConfirmCodOrder` | Admin or Staff with `ADJUST_FINAL_BILL` | Confirms a pending order; permits validated kg/g final-quantity adjustments for COD and recalculates the final bill. |
| `adminStartPreparingOrder` | Admin or Staff with `UPDATE_ORDER_STATUS` | Moves a confirmed order to `PREPARING`. |
| `adminMarkOrderReadyForDelivery` | Admin or permitted Staff | Records that preparation is complete and the order is ready for assignment/delivery. |
| `adminCancelCodOrder` | Admin or permitted Staff | Cancels an eligible order, records reason/actor, handles refund-pending state, and stops tracking/OTP. |
| `adminAssignOrderToSelf` | Admin or permitted Staff | Assigns delivery to the Admin after the order is ready. |
| `adminAssignDeliveryUser` | Admin or permitted Staff | Assigns an active Delivery user from the same shop. |
| `adminStartAssignedDelivery` | Assigned Admin | Moves an Admin-delivered ready order to `OUT_FOR_DELIVERY` and prepares its tracking session. |
| `adminCompleteDelivery` | Assigned Admin | Verifies delivery OTP when required, validates COD collection is not below amount due, completes the order, and stops tracking. |
| `adminReportCodMismatch` | Assigned Admin | Records a COD collection mismatch for operational follow-up. |

## Customer home, profile, addresses and FAQ — 17 functions

| Function | Access | Purpose and usage |
|---|---|---|
| `customerGetHome` | Guest or Customer | Returns the dashboard location labels, notification/cart counts, banners, categories, best sellers, recommendations, popular items, and quick picks. |
| `customerUpdateProfile` | Customer | Updates Customer first/last/display name fields. |
| `customerDeleteAccount` | Customer | Resumable account deletion: blocks active orders, anonymizes retained order facts, removes private subcollections/lookups, and deletes Firebase Auth identity. |
| `customerGetAddresses` | Customer | Lists saved delivery addresses. |
| `customerCreateAddress` | Customer | Validates coordinates/contact fields and creates a saved address, including default-address behavior. |
| `customerUpdateAddress` | Customer | Updates an owned address with validated map/contact data. |
| `customerSetDefaultAddress` | Customer | Atomically marks one owned address as default and clears the previous default. |
| `customerDeleteAddress` | Customer | Deletes an owned address while maintaining valid default-address state. |
| `customerGetFaqs` | Guest or Customer | Returns active FAQs for the public Customer Help screen. |
| `adminGetFaqs` | Admin or Staff with `MANAGE_FAQ` | Lists shop FAQs for management. |
| `adminSaveFaq` | Admin or Staff with `MANAGE_FAQ` | Creates or updates a validated FAQ. |
| `adminDeleteFaq` | Admin or Staff with `MANAGE_FAQ` | Deletes a shop FAQ. |
| `customerGetActiveCategories` | Guest or Customer | Returns active shop categories in configured display order. |
| `customerGetActiveSubcategories` | Guest or Customer; subcategory feature enabled | Returns active subcategories, optionally scoped to a category. |
| `customerGetProducts` | Guest or Customer | Returns active catalog products with supported category/subcategory/search filtering. |
| `customerGetProduct` | Guest or Customer | Returns one active product for Product Details. |
| `customerGetTrackingRoute` | Customer; `MAPS_ROUTES_API_KEY` | Validates ownership/tracking state and returns a backend-calculated route, distance, duration, and encoded path from rider to destination. |

## Category and subcategory management — 13 functions

| Function | Access | Purpose and usage |
|---|---|---|
| `adminBeginCategoryImageUpload` | Admin | Creates a short-lived authorized Storage upload session for a category image. |
| `adminGetCategories` | Admin or Staff with product/stock permission | Lists all categories, including inactive/admin metadata. |
| `adminCreateCategory` | Admin | Creates a category and finalizes its uploaded image. |
| `adminUpdateCategory` | Admin | Updates category fields/image with revision checks and cleans replaced finalized images. |
| `adminSetCategoryActive` | Admin | Enables/disables a category subject to server validation. |
| `adminDeleteCategory` | Admin | Deletes an eligible category and associated finalized Storage images without changing historical order snapshots. |
| `adminUpdateCategoryOrder` | Admin | Persists category display ordering. |
| `adminBeginSubcategoryImageUpload` | Admin | Creates a short-lived authorized Storage upload session for a subcategory image. |
| `adminGetSubcategories` | Admin; subcategory feature enabled | Lists Admin-visible subcategories. |
| `adminSaveSubcategory` | Admin; subcategory feature enabled | Creates/updates a subcategory, parent category, ordering, and optional image. |
| `adminDeleteSubcategory` | Admin | Deletes an eligible subcategory and its finalized images while retaining historical order snapshots. |
| `customerGetActiveCategories` | Guest or Customer | Customer-facing category read; also listed in the Customer section because it serves both areas. |
| `customerGetActiveSubcategories` | Guest or Customer | Customer-facing subcategory read; also listed in the Customer section because it serves both areas. |

The two shared Customer reads appear in both conceptual sections above but are counted
only once in the total of 126.

## Product and cart management — 14 functions

| Function | Access | Purpose and usage |
|---|---|---|
| `adminBeginProductImageUpload` | Admin or Staff with `MANAGE_PRODUCTS` | Creates an authorized temporary product-image upload session. |
| `adminGetProducts` | Admin or Staff with `MANAGE_PRODUCTS` or `MANAGE_STOCK` | Lists shop products for Admin/Staff management. |
| `adminCreateProduct` | Admin or Staff with `MANAGE_PRODUCTS` | Validates and creates a product, enforces limits, and finalizes uploaded images. |
| `adminUpdateProduct` | Admin or Staff with `MANAGE_PRODUCTS` | Updates product data/images with revision checks and cleans replaced files. |
| `adminSetProductActive` | Admin or Staff with product/stock permission | Enables/disables a product. |
| `adminArchiveProduct` | Admin or Staff with `MANAGE_PRODUCTS` | Archives a product and removes finalized images while leaving order/report snapshots intact. |
| `customerGetProducts` | Guest or Customer | Customer catalog listing. |
| `customerGetProduct` | Guest or Customer | Customer Product Details lookup. |
| `customerAddToCart` | Customer | Validates stock/product status and adds or increments an item in the shared server cart. |
| `customerGetCart` | Customer | Returns the canonical priced cart and delivery summary. |
| `customerSetCartQuantity` | Customer | Sets validated cart quantity against current availability/stock. |
| `customerRemoveCartItem` | Customer | Removes one product from the Customer cart. |
| `customerQuoteGuestCart` | Guest or Customer | Prices an anonymous/local cart against current catalog and delivery configuration without persisting it. |
| `customerMergeGuestCart` | Customer | Idempotently merges the local guest cart into the authenticated server cart after login. |

## Checkout and Customer orders — 6 functions

| Function | Access | Purpose and usage |
|---|---|---|
| `customerPrepareCheckout` | Customer | Revalidates cart, addresses, delivery rules, and feature/payment configuration before confirmation. |
| `customerPlaceOrder` | Customer | Atomically creates an idempotent COD/eligible order from the latest server quote and snapshots products/address. |
| `customerReconcileOrderCreation` | Customer | Resolves uncertain order creation after timeout/network interruption using the idempotency key. |
| `customerGetOrders` | Customer | Lists the Customer's shop orders and tracking/status summaries. |
| `customerCancelOrder` | Customer | Cancels an owned order only while its current status/payment rules permit it. |
| `customerReorder` | Customer | Rebuilds the cart from an old owned order using current product availability and prices. |

## Delivery and verification — 8 functions

| Function | Access | Purpose and usage |
|---|---|---|
| `deliveryGetAssignedCodOrders` | Delivery user | Lists COD orders assigned to the authenticated delivery user. |
| `deliveryStartAssignedOrder` | Assigned Delivery user | Starts an eligible ready assignment and moves it to `OUT_FOR_DELIVERY`. |
| `deliveryCompleteAssignedOrder` | Assigned Delivery user | Verifies OTP when enabled, validates COD amount, marks delivered, and stops tracking. |
| `deliveryReportCodMismatch` | Assigned Delivery user | Records that collected COD does not match the bill. |
| `activateDeliveryTracking` | Assigned Admin/Delivery; `DELIVERY_OTP_KEY` | Activates the scoped RTDB tracking session when global/admin feature gates and order assignment permit it. |
| `stopDeliveryTracking` | Assigned Admin/Delivery | Stops the current tracking session when delivery ends or is cancelled. |
| `customerGetDeliveryOtp` | Customer; `DELIVERY_OTP_KEY` | Returns the short delivery-completion code only for the Customer's actively tracked eligible order. |
| `verifyDeliveryOtp` | Assigned Admin/Delivery; `DELIVERY_OTP_KEY` | Verifies the Customer-provided code, rate-limits attempts, and records verification for completion. |

## Home banners — 6 functions

| Function | Access | Purpose and usage |
|---|---|---|
| `adminBeginBannerImageUpload` | Admin | Creates an authorized temporary banner-image upload session. |
| `adminGetBanners` | Admin | Lists all configured banners and placement/action metadata. |
| `adminCreateBanner` | Admin | Creates a banner and finalizes its image, enforcing the active-banner limit. |
| `adminUpdateBanner` | Admin | Updates image, text placements, CTA style/action, and revision. |
| `adminSetBannerActive` | Admin | Shows/hides a banner while enforcing active-count rules. |
| `adminDeleteBanner` | Admin | Deletes a banner and its finalized Storage image. |

## Notifications — 16 functions

### Callable notification functions

| Function | Access | Purpose and usage |
|---|---|---|
| `registerNotificationDevice` | Any active authenticated profile | Associates the FCM token with one user/device owner and role/shop context. |
| `unregisterNotificationDevice` | Any active authenticated profile | Deactivates the device token and removes its ownership mapping on logout/token removal. |
| `getNotificationHistory` | Any active authenticated profile | Returns recent notifications when the global in-app notification feature is enabled. |
| `markNotificationRead` | Any active authenticated profile | Marks one owned notification as read. |
| `markAllNotificationsRead` | Any active authenticated profile | Marks all unread notifications as read in bounded batches. |
| `clearAllNotifications` | Any active authenticated profile | Deletes the current user's notification history using a fixed cutoff and bounded batches. |

### Firestore event functions

| Function | Trigger | Purpose and usage |
|---|---|---|
| `notifyOrderCreated` | Create `shops/{shopId}/orders/{orderId}` | Notifies Customer and shop Admins when an accepted COD/paid order is created. |
| `notifyOrderUpdated` | Update `shops/{shopId}/orders/{orderId}` | Sends order status, final amount, payment, assignment, cancellation, and delivery-issue notifications. |
| `notifyLowStock` | Update `shops/{shopId}/products/{productId}` | Notifies Admins when stock crosses down to the low-stock threshold. |
| `notifyFeatureConfigChanged` | Update `appConfig/features` | Notifies Super Admins of effective global feature changes. |
| `notifyOfferActivated` | Write `shops/{shopId}/offers/{promotionId}` | Notifies eligible Customers when an allowed offer transitions to active. |
| `notifyCouponActivated` | Write `shops/{shopId}/coupons/{promotionId}` | Notifies eligible Customers when an allowed coupon transitions to active. |

### Scheduled functions

| Function | Schedule (`Asia/Kolkata`) | Purpose and usage |
|---|---|---|
| `sendDelayedOrderNotifications` | Every 15 minutes | Detects active orders past their delivery slot and notifies Customer/Admin once per event key. |
| `sendDeliveryStartReminders` | Every 15 minutes | Reminds assigned Admin/Delivery actors when the delivery start window is within 45 minutes. |
| `sendWeeklySuperAdminReport` | Monday at 09:30 | Sends the previous seven-day business summary to active Super Admins. |
| `sendMonthlySuperAdminReport` | First day of month at 09:00 | Sends the previous calendar month's business summary to active Super Admins. |

## Razorpay — 8 functions (currently excluded for MeatStation)

| Function | Access/trigger | Purpose and usage |
|---|---|---|
| `adminVerifyRazorpayConfiguration` | Admin callable | Performs a trusted server check that configured Razorpay credentials work. |
| `customerCreateRazorpayOrder` | Customer callable | Creates the internal payment attempt and corresponding Razorpay order for a validated checkout. |
| `customerRetryRazorpayPayment` | Customer callable | Creates/reuses an eligible retry for an owned failed/pending payment attempt. |
| `customerMarkRazorpaySdkOpened` | Customer callable | Records that the client checkout SDK was opened for the attempt. |
| `customerVerifyRazorpayPayment` | Customer callable | Verifies the returned payment signature and finalizes the paid order idempotently. |
| `customerRecordRazorpayFailure` | Customer callable | Records a sanitized SDK/payment failure against the owned attempt. |
| `customerReconcilePendingRazorpayPayment` | Customer callable | Queries/reconciles the latest pending attempt after interruption or app restart. |
| `razorpayWebhook` | Public HTTP POST with signature verification | Processes trusted Razorpay webhook events and reconciles payment/order state. |

## Deployment groups

### Everything, including Razorpay

```powershell
firebase deploy --project meatstationmobileapp --only functions
```

Do not use this for MeatStation until real Razorpay credentials are configured.

### Current MeatStation deployment

Deploy the **118 non-Razorpay functions** in batches. The approved PowerShell deployment
script dynamically reads `functions/src/index.ts` and excludes the eight names in the
Razorpay section. For a newly created project, use batches of three functions; the
initial MeatStation deployment demonstrated that ten concurrent Gen2 service creations
can exceed the `asia-south1` Cloud Run regional CPU quota. Larger batches can be used
later only after the regional quota is confirmed or increased.

### Core startup/login minimum

The minimum backend needed to load guest Home, read feature gates, quote a guest cart,
and complete all current authentication flows is:

```text
customerGetHome
customerQuoteGuestCart
getAllowedFeatureConfig
authSignInCustomerWithPhone
authVerifyCustomerRegistration
authRegisterCustomer
authSignInWithMobilePassword
authVerifyPasswordResetCode
authResetPassword
```

This minimum is only for smoke testing. Normal app release testing requires the full
non-Razorpay set because navigation reaches catalog, cart, checkout, orders, Admin,
notifications, reports, delivery, and account-deletion functions.

## Release verification checklist

1. Confirm all 118 intended non-Razorpay functions exist in `asia-south1`.
2. Confirm `shops/default` exists and is active.
3. Confirm Phone Authentication is enabled and SHA-1/SHA-256 fingerprints are registered.
4. Confirm the debug App Check token is allowlisted for debug tests.
5. Confirm Play Integrity App Check registration before release enforcement.
6. Verify Customer OTP login and Customer role navigation.
7. Verify pre-provisioned Admin, Super Admin, Staff, and Delivery OTP role navigation.
8. Verify Home, catalog, cart, COD checkout, order lifecycle, notifications, and account deletion.
9. Keep `razorpayAllowed: false` while Razorpay functions/credentials are excluded.
10. Create public Privacy Policy and Account Deletion pages before Play Console submission.
