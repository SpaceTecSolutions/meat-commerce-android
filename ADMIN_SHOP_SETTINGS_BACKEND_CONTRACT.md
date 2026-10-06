# Admin profile and shop settings contract

The authenticated, App Check-protected callables are `adminGetShopSettings`, `adminSaveShopSettings`, and `adminChangePassword`. Every function requires an active `ADMIN` user and derives the shop ID from the server-side profile or claims. Android never supplies a shop ID.

`adminGetShopSettings` returns business-local shop, address, contact, support, and Admin profile data plus `revision` and `bannerEditingAllowed`. The permission is derived from server-authoritative configuration.

`adminSaveShopSettings` validates field lengths and formats, uses `expectedRevision` transactionally, and updates only the caller's shop and profile. Ignore or reject banner fields unless `bannerEditingAllowed` is currently true. It must never update Super Admin feature controls, product limits, allowed payment/delivery capabilities, roles, application IDs, app names, logos, launcher icons, or theme colors.

`adminChangePassword` verifies the current mobile/password credential mapping on the server, enforces the configured password policy, revokes old authentication sessions where appropriate, and atomically updates the secure authentication record. Passwords must never be stored in Firestore, returned to Android, logged, or placed in analytics.

App name, package/application ID, logo, launcher icon, and primary/secondary branding remain product-flavor resources configured at build time.
