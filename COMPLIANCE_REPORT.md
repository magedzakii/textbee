# Compliance Report

Audit date: 2026-05-17

Scope: Android application under `android/`. Repository-level notes for `api/` and `web/` are included where they affect product privacy/compliance, but they are not packaged inside the Android app bundle.

## Executive Verdict

The Android app is significantly safer than the starting state, but it is not yet publish-ready for Google Play because it still depends on restricted SMS permissions:

- `SEND_SMS`
- `RECEIVE_SMS`
- `READ_PHONE_STATE`

Google Play treats SMS/Call Log permissions as high-risk. The app must either be approved through the SMS Permissions Declaration flow or be distributed outside Google Play. The current app is an SMS gateway, not a default SMS handler, so Play rejection risk remains critical even after technical hardening.

## Official Policy References Used

- Google Play SMS/Call Log policy: https://support.google.com/googleplay/android-developer/answer/10208820
- Google Play User Data policy: https://support.google.com/googleplay/android-developer/answer/10144311
- Google Play Data safety form guidance: https://support.google.com/googleplay/android-developer/answer/10787469
- Google Play target API requirements: https://support.google.com/googleplay/android-developer/answer/11926878
- Google Play foreground service requirements: https://support.google.com/googleplay/android-developer/answer/13392821
- Google Play Device and Network Abuse policy: https://support.google.com/googleplay/android-developer/answer/13315670
- Android Network Security Configuration: https://developer.android.com/privacy-and-security/security-config
- Android PendingIntent API guidance: https://developer.android.com/reference/android/app/PendingIntent.html
- Android Gradle Plugin 8.9.0 compatibility: https://developer.android.com/build/releases/agp-8-9-0-release-notes

## Current Android Configuration

| Area | Current status |
| --- | --- |
| Package namespace | `com.vernu.sms` |
| Production applicationId | `com.vernu.sms` |
| Dev applicationId | `com.vernu.sms.dev` |
| compileSdk | `35` |
| targetSdk | `35` |
| minSdk | `24` |
| AGP | `8.9.0` |
| Gradle wrapper | `8.11.1` |
| Release minify | Enabled |
| Release shrinkResources | Enabled |
| Release debuggable | Disabled |
| Release signing | Environment-variable upload keystore only |
| Cleartext traffic | Disabled |
| Firebase Android SDKs | Removed |
| FCM Android service | Removed |
| Crashlytics/Analytics Android SDKs | Removed |
| Foreground services | Removed |
| Boot receiver | Removed |
| WebView | Not present in Android app |
| Dynamic code loading | Not found in Android app |

## Permission Audit

| Permission | Status | Reason | Play risk |
| --- | --- | --- | --- |
| `SEND_SMS` | Still required | Core SMS gateway send feature | Critical restricted permission |
| `RECEIVE_SMS` | Still required | Optional inbound SMS forwarding when user enables receive mode | Critical restricted permission |
| `READ_PHONE_STATE` | Still required | SIM selection and SIM metadata display for multi-SIM sending | High privacy disclosure requirement |
| `INTERNET` | Required | API communication to user-configured HTTPS endpoint | Normal |
| `ACCESS_NETWORK_STATE` | Required | WorkManager/network-aware heartbeat and send retries | Normal |
| `READ_SMS` | Removed | Not needed after using SMS receive broadcast only | Risk reduced |
| `RECEIVE_BOOT_COMPLETED` | Removed | Boot scheduling removed | Risk reduced |
| `FOREGROUND_SERVICE` | Removed | Sticky foreground service removed | Risk reduced |
| Invalid `android.provider.Telephony.SMS_RECEIVED` permission | Removed | Not a valid app permission | Risk reduced |

## Manifest Component Audit

| Component | Exported | Status |
| --- | --- | --- |
| `.activities.MainActivity` | `true` | Launcher only |
| `.activities.SMSFilterActivity` | `false` | Internal only |
| `.receivers.SMSBroadcastReceiver` | `true` | Required for system SMS broadcasts; protected by `android.permission.BROADCAST_SMS` |
| `.receivers.SMSStatusReceiver` | `false` | Internal PendingIntent target only |
| `.services.FCMService` | Removed | FCM removed from APK |
| `.services.StickyNotificationService` | Removed | Foreground service policy risk removed |
| `.receivers.BootCompletedReceiver` | Removed | Boot/background launch risk removed |
| `androidx.startup.InitializationProvider` | Removed via manifest tools node | App initializes WorkManager explicitly |

## Fixes Applied

- Removed Firebase Android plugins and dependencies: Analytics, Crashlytics, Messaging, Google Services.
- Deleted Android FCM service and removed Firebase sample config files.
- Removed sticky foreground notification service and foreground-service permission.
- Removed boot receiver and boot permission.
- Replaced hardcoded API base URL with user-configurable HTTPS-only endpoint validation.
- Disabled cleartext traffic in manifest and Network Security Config.
- Disabled app backup and device transfer extraction for sensitive local state.
- Added prominent in-app disclosure immediately before SMS/phone permission request.
- Made SMS status PendingIntents explicit and immutable.
- Made `SMSStatusReceiver` non-exported.
- Protected SMS receive broadcast with system `BROADCAST_SMS` permission.
- Removed max-priority SMS broadcast interception.
- Removed `READ_SMS`; app no longer reads SMS inbox storage.
- Reduced SIM data collection: removed ICC ID, card ID, MCC, MNC, country ISO.
- Removed serial/OS-version DTO fields and heartbeat memory/storage/timezone/locale collection.
- Encrypted API key, device ID, device name, SMS filter config, and queued SMS payloads with Android Keystore AES/GCM.
- Removed plaintext API keys from WorkManager input data.
- Removed plaintext device IDs from new received/status WorkManager jobs.
- Removed startup version-report network ping.
- Removed hardcoded update/download URL opening.
- Reduced logs that exposed senders, recipients, device IDs, SIM subscription IDs, and raw API error bodies.
- Enabled release R8 and resource shrinking.
- Updated Android toolchain to target API 35.
- Updated Retrofit/Gson runtime dependencies.

## Remaining Compliance Risks

| Risk | Severity | Required action |
| --- | --- | --- |
| SMS permission policy approval | Critical | Complete SMS Permissions Declaration. Provide a defensible allowed use case, demo video, and store listing copy that makes SMS gateway functionality the primary purpose. |
| App is not default SMS handler | Critical | Google may reject unless the app qualifies under an exception, such as connected device companion, enterprise/CRM, device automation, or another listed exception. |
| Build not validated | High | Install JDK 17 and run release lint/tests/bundle. |
| No signed release artifact validated | High | Configure upload keystore env vars and build AAB. |
| Privacy policy must be updated | High | Publish a public non-PDF privacy policy that matches the Data Safety form and in-app disclosure. |
| Backend still contains FCM server code | Medium | Not in APK, but should be removed or gated if the full product no longer supports Android FCM. |
| Web app contains GA/Clarity analytics | Medium | Not in APK, but the public privacy policy must disclose web tracking if the same service/listing points users there. |

## Google Play Console Declarations Required

- App content > Sensitive permissions: declare `SEND_SMS` and `RECEIVE_SMS`.
- App content > Data safety: declare SMS/message content, phone numbers/senders/recipients, device identifiers, diagnostics/status metadata, and endpoint transmission.
- App content > Privacy policy: provide a public URL.
- App content > Data deletion: required if users can create accounts in the app or service.
- App content > App access: provide reviewer credentials and setup instructions for endpoint/API key flow.
- App content > Ads: declare "No ads" for the Android app if no ad SDK is added.
- App content > Foreground services: no declaration needed after service removal.
- App content > Financial features: no declaration unless Play listing/service adds financial use cases.
- Target audience/content rating: must describe SMS gateway behavior accurately and restrict from children if not intended for children.

## Publish Status

Status: Not ready for Play production.

Main blockers:

- SMS restricted-permission approval.
- Missing JDK/build validation.
- Missing signed AAB validation.
- Missing privacy policy/Data Safety completion.

