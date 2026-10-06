# Customer and Admin fixes

## Implemented

1. **Delivery map:** fixed camera initialization before any coordinates exist. The camera now waits for a rider/destination and reacts when the destination arrives. Added Google Places search and a tap-to-select Google map to address creation/editing, alongside current location. A selected coordinate is required to save. Address/building details remain reviewable/editable; there is no standalone manual-location mode. Existing order snapshots are not rewritten.
2. **Checkout re-entry:** a completed checkout is cleared on a new visit. An unresolved online payment is preserved rather than discarded. Payment restoration is no longer hidden behind the missing-quote error state.
3. **Payment notifications:** COD notifies on creation. Online order-placed/new-order notifications wait for PAID, using the existing deterministic event keys to prevent duplicate history. Existing payment verification and the Admin confirmation guard are retained. The existing architecture still creates a pending order/payment reservation before opening Razorpay; this change does not move reservation creation after payment. Pending-payment recovery is now restricted to the authenticated customer.
4. **Registration:** fixed a defensive consistency issue: a failure to issue a custom token after the profile transaction commits no longer deletes the newly created Auth account. The reported signup failure itself remains unconfirmed: retrieved logs had no recent failed signup, and the exact error/step is needed.
5. **Notification badge:** Home observes the authenticated user's unread notifications, including mark-all-read changes. A refreshed Home response cannot overwrite the newer observed count. The listener is removed when its ViewModel is cleared.
6. **Revenue graphs:** dashboard reuses the report bar chart with currency revenue slabs and explicit period-axis labels. Report labels use backend buckets instead of assuming Monday-first labels. Stale filter responses are ignored; dashboard requests are versioned.
7. **Customer profile:** added a full Profile Information form with first/last name, read-only mobile and role, and authenticated profile save. Removed the payment-method menu entry.
8. **Admin customer details:** replaced the details dialog with a bottom sheet showing customer information, purchase total, order count, highest order and the latest three orders. The details query is customer-scoped; highest order is calculated before the response is reduced to three orders.
9. **Banners:** separate normalized text/button positions, drag placement, left/centre/right controls, shared preview/customer rendering, and category/product dropdown destinations. Legacy banner alignment remains supported. Category actions are handled by Customer Home.

## Configuration and device verification

- Enable **Places API (New)** and include it alongside **Maps SDK for Android** in the existing Android key's API restrictions. Retain Android package + signing SHA-1 restrictions. No server secret is embedded in the app.
- Google reference: https://developers.google.com/maps/documentation/places/android-sdk/place-autocomplete
- Existing addresses without coordinates should be edited to select and review a delivery pin. No coordinates were guessed or written into existing orders.
- No ADB device was connected. Physical-device signup, Places search, map gestures, banner dragging, badge updates and the complete Razorpay/webhook flow still require device acceptance tests.
- Registration investigation needs the exact displayed error and whether it occurs before OTP, after OTP, or after Register.
- Existing resource packaging emits STRING_TOO_LARGE diagnostics. They did not fail the completed build; their runtime impact has not been verified.

## Verification

- Backend TypeScript build and 32 tests passed, including online-payment notification acceptance.
- Final MeatBush and SampleButcher debug APK builds passed (BUILD SUCCESSFUL, 3m 7s); verification is recorded in verification-build.log.
- Checkout unit tests: 3 passed. Address unit tests: 3 passed, including missing/invalid coordinates.
- Scoped Firebase deployment completed successfully: 13 affected functions and the paymentAttempts index deployed to meatbush-c157e (named Firestore database default). See verification-deploy.log. No Maps key, payment secret or RTDB rule was changed.

## Main changed areas

- feature/address: AddressMapPicker, AddressMapResolver, form/route/ViewModel/validator and tests; Places/Maps dependencies.
- feature/checkout: CheckoutViewModel and CheckoutScreen.
- feature/customerhome: live Home badge, profile form, banner rendering/navigation.
- feature/orders: CustomerDeliveryMap camera lifecycle.
- feature/admin: revenue charts/filter handling, customer bottom sheet, banner editor and destination controls.
- core/model, core/domain, core/data: profile save, unread count, banner placement and customer-summary contracts/mapping.
- core/designsystem: shared PositionedBannerContent.
- functions: payment notifications/tests, customer-profile callable, customer-address validation, registration cleanup, customer-details query, banner fields/Home response, customer-scoped Razorpay recovery.
- firestore.indexes.json: customerId + status index for paymentAttempts.
