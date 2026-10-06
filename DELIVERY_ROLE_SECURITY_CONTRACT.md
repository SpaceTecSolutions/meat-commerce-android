# Delivery role UI and security contract

The Delivery graph contains Home, Assigned Orders, Delivery History, Profile, Notifications, and Help only. Revenue, reports, products, feature settings, and Admin configuration routes are absent from the graph and fail route/deep-link validation.

`deliveryGetAssignedCodOrders` is the only order-list boundary used by Delivery screens. It must require an authenticated, active `DELIVERY` user and return only documents where `assignedDeliveryUserId` equals the authenticated UID, the assignment is active, and the order belongs to the same shop. History is the terminal subset of that authorized response; it is not a broader order query.

Delivery order details contain customer delivery information, immutable items and quantities, instructions, payment collection state, and amount due. They expose no revenue aggregates, margins, catalog controls, reports, feature flags, staff management, or Admin processing actions.

Firestore Security Rules deny Delivery users collection-wide order reads and all access to product-management, report, feature configuration, payment configuration, delivery configuration, and user-management writes. Every Delivery mutation callable repeats assignment, active-user, shop, revision, and valid-transition checks.
