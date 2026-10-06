# MeatBush architecture

The app uses practical Clean Architecture with dependencies pointing toward stable core contracts.

## Modules

- `app`: Android entry point, Hilt composition root, client flavor configuration.
- `core:model`: framework-free role and feature configuration models.
- `core:common`: framework-free result, error, and coroutine contracts.
- `core:data`: DataStore implementation, dispatchers, and Firebase dependency foundation.
- `core:domain`: framework-free repository and service contracts.
- `core:designsystem`: theme tokens, adaptive layout, icons, shared components, and UI states.
- `core:navigation`: common authentication and role-root navigation contract.

Feature modules should be introduced only when their business phase begins. Each feature should expose
domain contracts inward and keep Firebase implementations in its data layer.

## Adding a client flavor

Add a product flavor in `app/build.gradle.kts` and a matching `app/src/<client>/res` source set containing
`app_name`, `brand_primary`, `brand_secondary`, launcher/splash assets, and that client's Firebase
`google-services.json`. Business sources remain shared.

Feature visibility is resolved from both Super Admin allowance and Admin configuration. Backend rules and
Cloud Functions must enforce the same authority; UI visibility is never an authorization boundary.
