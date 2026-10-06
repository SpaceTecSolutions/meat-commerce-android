# Firebase mobile-number/password sign-in setup

The Android UI accepts a mobile number and password. Firebase Authentication stores the actual password on an
internal Email/Password identity. The internal email is resolved only by the trusted callable backend through a
keyed HMAC lookup; it is never returned to Android.

## Firebase Console

1. Authentication → Sign-in method → enable **Email/Password**.
2. App Check → register the Android app with Play Integrity for production.
3. During local debug, copy the App Check debug token from Logcat and register it in App Check → Apps → Debug tokens.
4. Do not enable App Check enforcement until the debug token is registered and a debug login succeeds.

## Install and build Functions

```powershell
cd C:\Users\akash\Projects\meatbush\functions
npm install
npm run build
npm test
```

## Configure secrets

Generate one random HMAC key and save it in a password manager. The exact same value is used once by the
provisioning script and stored as a Functions secret. Never place it in Android or Firestore.

```powershell
$bytes = New-Object byte[] 32
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
$hmacKey = [Convert]::ToBase64String($bytes)

firebase functions:secrets:set AUTH_LOOKUP_HMAC_KEY
firebase functions:secrets:set IDENTITY_TOOLKIT_API_KEY
```

Paste `$hmacKey` for the first secret. For `IDENTITY_TOOLKIT_API_KEY`, paste the Web API key shown under Firebase
Project settings → General. The Firebase API key identifies the project; keeping it in Functions configuration
also prevents the internal credential flow from being duplicated in Android feature code.

## Deploy

```powershell
cd C:\Users\akash\Projects\meatbush
firebase use MeatStation
firebase deploy --only functions:authSignInWithMobilePassword,firestore:rules
```

## Provision the initial Super Admin

Download a service-account JSON from Project settings → Service accounts → Generate new private key. Keep it
outside the repository and delete/revoke it when provisioning is finished.

```powershell
$env:GOOGLE_APPLICATION_CREDENTIALS = 'C:\secure\meatbush-service-account.json'
$env:AUTH_LOOKUP_HMAC_KEY = $hmacKey
$env:SUPER_ADMIN_MOBILE = '+919876543210'
$env:SUPER_ADMIN_PASSWORD = 'replace-with-a-strong-unique-password'
$env:SUPER_ADMIN_FIRST_NAME = 'Super'
$env:SUPER_ADMIN_LAST_NAME = 'Admin'
$env:SUPER_ADMIN_UID = 'paste-the-existing-authentication-uid-here'

cd C:\Users\akash\Projects\meatbush\functions
npm run provision:super-admin
```

Clear sensitive PowerShell variables afterward:

```powershell
Remove-Item Env:GOOGLE_APPLICATION_CREDENTIALS
Remove-Item Env:AUTH_LOOKUP_HMAC_KEY
Remove-Item Env:SUPER_ADMIN_MOBILE
Remove-Item Env:SUPER_ADMIN_PASSWORD
Remove-Item Env:SUPER_ADMIN_FIRST_NAME
Remove-Item Env:SUPER_ADMIN_LAST_NAME
Remove-Item Env:SUPER_ADMIN_UID
$hmacKey = $null
$bytes = $null
```

If you already created the Authentication user/profile manually, set `SUPER_ADMIN_UID` to that exact UID. The
script will reuse it, replace its internal email/password safely, and update the existing profile. Omit the
variable only when creating the initial identity from scratch.

The script creates or updates all three required records atomically where possible:

- Firebase Authentication internal Email/Password user
- `users/{uid}` with `role = SUPER_ADMIN`
- server-only `authCredentials/{HMAC-SHA256(E164 mobile)}` mapping

Do not create `authCredentials` manually and never store a password or password hash in Firestore.

## Optional cleanup policy

Configure a Firestore TTL policy on `authRateLimits.expireAt` so expired rate-limit documents are deleted
automatically. The function remains correct without TTL, but TTL prevents old limiter records accumulating.
