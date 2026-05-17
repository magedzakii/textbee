# Security Report

Audit date: 2026-05-17

Scope: Android app under `android/`.

## Executive Summary

The highest-risk technical behaviors were removed or reduced. The app no longer bundles Firebase/FCM/Crashlytics, no longer allows cleartext traffic, no longer starts a sticky foreground service, no longer listens on boot, and no longer stores API keys or queued SMS payloads in plaintext.

The remaining high-risk area is functional rather than hidden: the app sends and receives SMS. That must be openly disclosed and approved by Google Play.

## Hardening Applied

### Network Security

- `android:usesCleartextTraffic` changed to `false`.
- Added `res/xml/network_security_config.xml` with `cleartextTrafficPermitted="false"`.
- API base URL now comes from user settings, not `BuildConfig`.
- URL validation requires:
  - `https` scheme only
  - host present
  - no credentials in URL
  - no query string
  - no fragment
  - trailing slash normalized for Retrofit
- Removed hardcoded update/download URL.
- Removed startup version-report ping.

### Local Storage

Protected with Android Keystore AES/GCM:

- API key
- Device ID
- Device name
- SMS filter configuration
- Received SMS WorkManager payloads
- SMS status WorkManager payloads
- Outbound SMS WorkManager payloads

Other storage hardening:

- `android:allowBackup="false"`
- `android:fullBackupContent="false"`
- `dataExtractionRules` excludes all cloud backup and device transfer data.

### Manifest and Component Exposure

- Removed FCM service.
- Removed sticky foreground service.
- Removed boot completed receiver.
- Removed invalid Telephony pseudo-permission.
- Removed `READ_SMS`.
- Set `SMSStatusReceiver` to `exported=false`.
- Kept `SMSBroadcastReceiver` exported only because system SMS broadcasts require it.
- Added `android:permission="android.permission.BROADCAST_SMS"` to protect the SMS receiver from spoofed third-party broadcasts.
- Removed max-priority SMS receiver setting.

### PendingIntent Safety

- SMS sent/delivered callbacks use explicit receiver intents.
- PendingIntents use `FLAG_IMMUTABLE | FLAG_UPDATE_CURRENT`.
- No mutable PendingIntent usage found.

### Logging and PII Reduction

Removed or reduced logs containing:

- Phone numbers
- SMS senders
- Device IDs
- SIM subscription IDs
- Raw API error bodies
- Device names

### Data Minimization

Removed from Android collection:

- Firebase token
- SIM ICC ID
- SIM card ID
- MCC/MNC/country ISO
- Device serial field
- Heartbeat memory bytes
- Heartbeat storage bytes
- Heartbeat timezone/locale

Kept only the data needed for SMS gateway operation:

- SMS content/sender/recipient/status/timestamps
- Device registration name/model/app version
- Battery/network/gateway status
- SIM carrier/display/slot/subscription type/subscription ID for SIM selection

### Release Build Security

- Release `minifyEnabled true`.
- Release `shrinkResources true`.
- Release `debuggable false`.
- Release signing now uses environment variables and no debug signing fallback unless a release keystore is explicitly provided.
- Added Gson keep rules for DTO/model field serialization under R8.

## Static Security Checks

No Android matches found for:

- Firebase/FCM/Crashlytics/Google Services
- `READ_SMS`
- `RECEIVE_BOOT_COMPLETED`
- `FOREGROUND_SERVICE`
- Cleartext traffic enabled
- Mutable PendingIntent
- MD5
- `Runtime.exec`
- `ProcessBuilder`
- `DexClassLoader` / `PathClassLoader`
- `WebView` / `addJavascriptInterface`
- Accessibility service
- Device admin
- Overlay permission
- Install packages permission
- Query all packages
- Manage external storage
- Notification listener
- VPN service
- MediaProjection
- Usage stats permission
- Battery optimization bypass

## Dependency Audit

Runtime Android dependencies:

| Dependency | Version | Status |
| --- | --- | --- |
| `androidx.appcompat:appcompat` | `1.7.1` | Trusted AndroidX |
| `com.google.android.material:material` | `1.13.0` | Trusted Google Material |
| `androidx.constraintlayout:constraintlayout` | `2.2.1` | Trusted AndroidX |
| `com.google.code.gson:gson` | `2.13.2` | Updated |
| `com.squareup.retrofit2:retrofit` | `2.12.0` | Updated |
| `com.squareup.retrofit2:converter-gson` | `2.12.0` | Updated |
| `com.journeyapps:zxing-android-embedded` | `4.3.0` | Latest published, but old; monitor or replace if future maintenance is required |
| `androidx.work:work-runtime` | `2.11.2` | Current WorkManager line |

Test-only dependencies:

- `junit:junit:4.13.2`
- `androidx.test.ext:junit:1.1.3`
- `androidx.test.espresso:espresso-core:3.4.0`

Test dependencies do not ship in release. They can be updated in a separate test maintenance pass.

## Remaining Security Risks

| Risk | Severity | Notes |
| --- | --- | --- |
| SMS gateway behavior | Critical | Any app that sends/receives SMS is high scrutiny. Must be transparent and approved. |
| WorkManager send delay uses `Thread.sleep` | Medium | Off-main-thread, so not ANR, but delays near 3600 seconds are not reliable for WorkManager. Consider scheduling delayed work instead. |
| Backend FCM code remains in repo | Medium | Not in APK, but the full stack still has Firebase Admin messaging paths. Remove/gate if FCM is intentionally retired. |
| Web analytics present in `web/` | Medium | Not in APK. Must be disclosed in web/service privacy policy. |
| Release build not executed | High | Missing JDK blocks lint, unit tests, and AAB generation. |

## Blocked Validation

Command attempted:

```bash
cd android
./gradlew :app:lintProdRelease :app:bundleProdRelease
```

Result:

```text
ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
```

Required:

- Install JDK 17.
- Set `JAVA_HOME`.
- Re-run release lint/tests/bundle commands.

