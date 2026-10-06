# White-label client setup

The current baseline is the SpaceTecSolutions development application:

| Source set | Application ID | App name | Firebase configuration |
|---|---|---|---|
| `main` | `com.spacetecsolutions.meatapp` | MeatBush | Register separately before Firebase-enabled builds |

No production client flavor is configured in the baseline. Client flavors should be added only when their
branding, unique application ID, Firebase project, signing identity, and release configuration are ready.

Legacy sample source-set files may contain deliberately fake, non-production Firebase values. They must never
be used for a release.

## Add a client

1. Introduce a `client` flavor dimension when the first production client is approved.
2. Choose a lower-camel-case flavor name, such as `freshCuts`, and a globally unique application ID.
3. Add the flavor to `productFlavors` in `app/build.gradle.kts`:

   ```kotlin
   create("freshCuts") {
       dimension = "client"
       applicationId = "com.example.freshcuts"
   }
   ```

4. Create `app/src/freshCuts/res/values/brand.xml` with the stable resource contract:

   ```xml
   <resources>
       <string name="app_name">Fresh Cuts</string>
       <color name="brand_primary">#006A60</color>
       <color name="brand_secondary">#8B5000</color>
       <color name="launcher_background">#F4FBF8</color>
       <item name="img_splash_brand" type="drawable">@drawable/img_brand_logo</item>
   </resources>
   ```

5. Add these flavor-owned brand assets:

   ```text
   app/src/freshCuts/res/drawable/img_brand_logo.xml
   app/src/freshCuts/res/drawable/ic_launcher_foreground.xml
   app/src/freshCuts/res/mipmap-anydpi/ic_launcher.xml
   app/src/freshCuts/res/mipmap-anydpi/ic_launcher_round.xml
   app/src/freshCuts/res/mipmap-anydpi-v26/ic_launcher.xml
   app/src/freshCuts/res/mipmap-anydpi-v26/ic_launcher_round.xml
   ```

   Raster or WebP alternatives may be used for the brand logo where appropriate. Standard UI action icons
   remain in the shared Material icon catalog.

5. Create a separate Firebase project for the client. Register an Android app whose package name exactly
   matches the flavor's `applicationId`, download its `google-services.json`, and place it at:

   ```text
   app/src/freshCuts/google-services.json
   ```

6. Configure that Firebase project independently: Authentication, Firestore, Storage, Cloud Functions,
   FCM, App Check, Crashlytics, Analytics, and Realtime Database only if live tracking is enabled. Deploy the
   client's security rules and backend configuration separately.
7. Configure an independent release signing key and Play Console application. Never commit keystores,
   service-account files, Razorpay secrets, or Firebase Admin credentials.
8. Build and test the new variant:

   ```powershell
   .\gradlew.bat :app:assembleFreshCutsDebug test
   ```

## Source-set rule

Flavor source sets may contain only client configuration and branding resources. Do not place repositories,
use cases, ViewModels, navigation logic, role logic, or business screens under `app/src/<client>/java` or
`app/src/<client>/kotlin`. Shared behavior belongs in `main` or the appropriate shared module.

If a client-specific capability is ever unavoidable, model it as configuration or a feature flag first. A
code fork requires an explicit architectural review because it increases testing and maintenance cost.

## Release checklist

- Replace the fake sample Firebase file with the downloaded client file.
- Confirm the JSON package name equals the flavor application ID.
- Verify app name, logo, launcher icons, primary color, and secondary color.
- Confirm the release signing configuration and Play Console listing are client-specific.
- Build the release variant and run all tests.
- Verify disabled optional features do not initialize workers, listeners, permissions, or backend calls.
