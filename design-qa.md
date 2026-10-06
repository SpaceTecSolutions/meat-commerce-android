# Customer Live Delivery Tracking Design QA

- Source visual truth: `C:/Users/akash/AppData/Local/Temp/codex-clipboard-0a06cf6b-4529-4ad5-91ba-553b16b69612.png`
- Source pixels: 256 × 258, including the surrounding reference composition and phone frame
- Implementation screenshot: unavailable for the newly compiled build
- Intended viewport: Android portrait, compact phone through tablet
- CSS size/density normalization: not applicable to this native Android screen; runtime density comparison unavailable
- State: active OUT_FOR_DELIVERY order with active tracking session, fresh rider coordinate, fixed destination, and route

**Findings**

- [P1] Runtime visual comparison is blocked.
  Location: Customer Live Delivery Tracking screen.
  Evidence: the source reference was opened, but the newly compiled Android build was not installed or captured. The connected device contains an older build.
  Impact: exact header height, map/card proportions, marker scale, typography, and route visibility cannot be compared at matching density and state.
  Fix: install the updated build, open an eligible order with an active rider update, and capture the full tracking screen.

- [P1] Fresh live-state visual evidence is unavailable.
  Location: active order tracking data.
  Evidence: RTDB contains the last rider coordinate, but the session authorization is inactive and the sender stopped updating. The assigned sender must use Resume Live Tracking from the updated build.
  Impact: smooth rider motion, freshness copy, and route refresh behavior cannot be visually verified from the repository alone.
  Fix: keep the assigned sender's foreground tracking service active while capturing the customer screen.

- [Resolved] Legacy order destination coordinates no longer block the live map.
  Evidence: the protected Routes request now accepts the immutable order address as an address waypoint and returns the resolved route endpoint to Android.
  Result: Track Live opens the map for an otherwise eligible active order; rider location can render while the destination route is resolving.

**Required Fidelity Surfaces**

- Fonts and typography: centralized app typography used with a bold status title, compact order ID, ETA, and delivery-card hierarchy; visual comparison blocked.
- Spacing and layout rhythm: compact brand header, map-majority layout, overlaid state/recenter controls, and compact bottom card implemented; visual comparison blocked.
- Colors and visual tokens: centralized brand primary red, standard map colors, red destination pin, and distinct red delivery marker used.
- Image quality and assets: official Material delivery icon is used as the rider glyph; Google Maps remains the map surface.
- Copy and content: dynamic ETA/distance, Live/Updating/stale/route-failure states, delivery identity, call action, and destination summary are present.

**Comparison History**

- Initial implementation used a generic white top app bar, removed/redrew the polyline on every location update, lacked recenter behavior, and did not surface route failure.
- Fixes applied: compact brand status header, persistent polyline replacement, gesture-respecting camera, explicit recenter action, smooth position/heading interpolation, contextual state pill, and compact delivery card.
- Post-fix visual evidence is unavailable because installation and manual device testing remain with the user.

**Implementation Checklist**

- Install the updated MeatBush debug build.
- Use either an order with stored destination coordinates or a legacy order with a complete delivery-address snapshot.
- Start delivery from the assigned Admin/Delivery device and keep its foreground tracking service active.
- Capture the customer tracking screen at normal-phone dimensions.
- Compare the captured header, map height, route, marker sizes, and bottom card with the source.

**Follow-up Polish**

- Adjust map padding or header/card height only after a matching eligible runtime capture is available.

final result: blocked

## Employee / Staff experience — compile-only revision (2026-09-23)

- Source visual truth: all eight `screen.png` files under `C:/Users/akash/Documents/staff_or_employee_design_reference`.
- Secondary references: all eight sibling `code.html` files; no `DESIGN.md` or separate asset files were present.
- Implementation screenshot: unavailable because the user explicitly prohibited APK generation, install, emulator/device launch, and UI testing.
- Intended viewport: responsive Android portrait, compact phone through tablet.
- State: permission-driven STAFF shell covering full operational access and restricted VIEW_ORDERS-only access.

**Findings**

- [P1] Runtime visual comparison is blocked.
  Location: Employee Home, Orders, Order Details, Products, More, and Profile.
  Evidence: every source reference was opened and inspected, but the new native Compose implementation was compile-validated only and has no same-state runtime capture.
  Impact: exact density-dependent typography, scroll positions, card heights, and navigation-bar proportions cannot be certified against Stitch.
  Fix: after the user installs the build, capture full-permission and VIEW_ORDERS-only Staff accounts at the reference phone width and compare each state.

**Required Fidelity Surfaces**

- Fonts and typography: existing centralized Android sans-serif typography is retained; Hanken Grotesk from the web export was not added as a new dependency.
- Spacing and layout rhythm: off-white canvas, compact Staff header, white grouped surfaces, 16dp cards, permission-adaptive navigation, and max-width content were implemented from the references.
- Colors and visual tokens: existing centralized `#D71920` brand red, pale-red emphasis, neutral inset panels, border, ink, and muted tokens are reused.
- Image quality and assets: existing Material/project icons and real product images are reused; Stitch sample employee/product images were not shipped or hardcoded.
- Copy and content: Staff identity, orders, products, and assigned permissions come from authenticated repositories/state; Stitch sample names, counts, stations, shifts, and analytics were not hardcoded.

**Implementation Checklist**

- Capture full operational Staff Home, Orders, Products, More, and Profile.
- Capture VIEW_ORDERS-only Orders and read-only Order Details.
- Verify bottom tabs adapt to permissions and unauthorized routes remain absent.
- Compare at matching viewport/density and correct any P0/P1/P2 visual drift.

final result: blocked

## Orders / Notifications / Address / Admin charts — compile-only revision

- Sources: codex-clipboard-d46c6c62-af94-4d08-9994-5cb8fb933033.png (Orders), codex-clipboard-ecd5a69d-8c54-413c-8040-b9928657e24a.png (Notifications), existing Super Admin report chart.
- Changes: compact flat order cards and fitted tabs; icon/title/time notification rows with dividers; dashboard line plot; report bars styled after Super Admin; polished address cards; address form and map picker are full screens with Back handling, not draggable sheets.
- No implementation screenshot or viewport comparison: user explicitly requested compilation only, without assembly/install/run.
- Typography, spacing, colours, image fidelity and wrapping have not been visually verified. Existing vector icons are reused; the reference's bespoke delivery/product illustrations are not reproduced exactly.
- No pixel-perfect claim. Runtime map gestures, back navigation and small/tablet layouts need review after the user builds the APK.
- Compile output: design-compile.log. No deployment or APK generation requested in this revision.

final result: blocked
