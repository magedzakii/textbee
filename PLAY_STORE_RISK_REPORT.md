# Play Store Risk Report

Audit date: 2026-05-17

## Overall Risk

Current publish readiness score: 61/100

The technical APK risk has been reduced substantially, but Play policy risk remains high because the product is an SMS gateway. Google Play review can still reject or suspend the app if the restricted SMS use case is not approved.

The score cannot safely exceed 65 until:

- Release build and lint pass locally/CI.
- A signed AAB is generated and verified.
- SMS Permissions Declaration is approved.
- Privacy Policy and Data Safety are completed.
- Closed testing/pre-launch report passes.

## Critical Risks

### 1. SMS Restricted Permissions

Severity: Critical

The manifest still declares:

- `SEND_SMS`
- `RECEIVE_SMS`

Google Play only allows SMS/Call Log permissions for default handlers or approved exception cases. The app is an SMS gateway and does not currently act as the device default SMS handler.

Safest options:

1. Submit the SMS Permissions Declaration with a precise allowed exception.
2. Publish only a Play-limited build with SMS functionality removed.
3. Distribute the full SMS gateway outside Google Play.

### 2. Remote SMS Gateway Use Case

Severity: Critical

The app enables SMS sending/receiving through an API endpoint. This can be interpreted as connected-device, device-automation, enterprise/CRM, or remote-control behavior depending on how the listing and reviewer demo present it.

Do not describe the app as stealth, monitoring, background SMS forwarding, automation for third parties, marketing spam, bulk unsolicited SMS, surveillance, or remote phone control.

Required review positioning:

- The user owns/controls the Android device.
- The user explicitly configures the HTTPS endpoint.
- The user grants SMS permissions after in-app disclosure.
- Inbound SMS forwarding is off by default and user-toggle controlled.
- SMS use is the app's primary visible functionality.
- No hidden tracking SDKs or undisclosed destinations exist in the APK.

## High Risks

| Risk | Status | Notes |
| --- | --- | --- |
| Build validation missing | Blocked | No JDK/JAVA_HOME in environment. |
| Release signing not verified | Blocked | Requires upload keystore env vars. |
| Privacy policy missing/not verified | Manual | Must be public, non-PDF, and match Data Safety. |
| Data Safety not completed | Manual | Must declare SMS and device data collection/transmission. |
| Account deletion declaration | Manual | Required if the service lets users create accounts. |
| QR scanner camera permission | Needs merged-manifest check | ZXing may contribute `CAMERA`; declare camera access if present after manifest merge. |

## Medium Risks

| Risk | Status | Notes |
| --- | --- | --- |
| Backend FCM remains in repo | Not APK | Server still contains `firebase-admin` FCM push paths. Android app no longer registers FCM. |
| Web analytics | Not APK | `web/` includes Google Analytics and Microsoft Clarity. Disclose in service privacy policy. |
| Long SMS send delay in Worker | Needs reliability testing | A 3600-second sleep can exceed normal WorkManager expectations. |
| ZXing dependency age | Accepted for now | 4.3.0 is latest published but old. Monitor or replace if maintenance risk becomes unacceptable. |

## Low Risks

| Area | Status |
| --- | --- |
| Cleartext traffic | Fixed |
| Debuggable release | Fixed |
| R8/minify | Fixed |
| App backup of secrets | Fixed |
| Mutable PendingIntent | Fixed |
| Exported status receiver | Fixed |
| Boot receiver | Removed |
| Foreground service declaration | Removed |
| Dynamic code loading | Not found |
| WebView abuse | Not found |
| Accessibility abuse | Not found |
| Device admin abuse | Not found |
| Overlay permission | Not found |
| Install packages permission | Not found |
| Query all packages | Not found |
| All files access | Not found |
| Background location | Not found |
| Ads SDKs in APK | Not found |

## Play Protect / Malware Signal Review

Removed or avoided high-signal malware indicators:

- No hidden Firebase/FCM command channel in APK.
- No cleartext HTTP traffic.
- No self-update/download URL.
- No boot-start receiver.
- No sticky foreground service.
- No dynamic executable loading.
- No accessibility or device-admin privileges.
- No broad storage/package/install permissions.
- No suspicious WebView JavaScript bridge.
- No obfuscated malicious behavior added.

Remaining Play Protect sensitivity:

- SMS send/receive itself is sensitive.
- Remote API-triggered SMS sending is sensitive even when legitimate.
- Store listing, screenshots, demo video, and privacy policy must make this behavior obvious.

## Required Play Console Declarations

1. Sensitive permissions:
   - `SEND_SMS`
   - `RECEIVE_SMS`

2. Data Safety:
   - SMS/message content
   - Sender/recipient phone numbers
   - SMS delivery status and timestamps
   - Device/app identifiers
   - Device model/manufacturer/app version
   - SIM metadata used for SIM selection
   - Network/battery/gateway operational status

3. Security practices:
   - Data encrypted in transit: Yes, HTTPS required.
   - Data deletion request mechanism: Required if accounts exist.
   - Data encrypted at rest: Local sensitive app data is encrypted with Android Keystore.

4. App access:
   - Provide reviewer account/API key/demo endpoint.
   - Provide clear test steps for registering a device, granting permissions, sending SMS, enabling receive SMS, and deleting account/data.

5. Foreground services:
   - None after removal.

6. Ads:
   - Declare no ads for the Android APK unless an ad SDK is added later.

## Store Listing Requirements

The listing must prominently state:

- The app turns the user's Android device into an SMS gateway.
- The app sends SMS from the user's SIM/device when the user configures it.
- The app can forward received SMS only when the user enables the receive option.
- SMS content and phone numbers are sent to the configured HTTPS endpoint.
- The app is intended for legitimate user-owned device workflows only.

Avoid:

- "Silent"
- "Spy"
- "Monitor"
- "Track"
- "Hidden"
- "Read anyone's SMS"
- "Remote control phone"
- "Bulk marketing SMS"
- "Bypass carrier limits"

## Final Risk Assessment

Technical APK risk: Medium-low after hardening.

Google Play policy risk: High to critical because of SMS permissions.

Publish recommendation: Do not submit to production until the SMS declaration packet, privacy policy, signed build, and closed testing evidence are complete.

