# Privacy Audit

Audit date: 2026-05-17

Scope: Android app under `android/`, with ecosystem notes for `api/` and `web/`.

## Summary

The Android APK no longer contains Firebase Analytics, Crashlytics, FCM, ads SDKs, or third-party tracking SDKs. The app transmits data only to the user-configured HTTPS API endpoint.

The app still handles highly sensitive SMS data. This must be disclosed in the app, privacy policy, Play Data Safety, and SMS Permissions Declaration.

## In-App Disclosure Added

The permission flow now shows a disclosure before requesting SMS/phone permissions. It explains:

- SMS and phone-state permissions are used for SMS gateway functionality.
- SMS content, sender/recipient, delivery status, device registration details, and SIM metadata may be sent to the configured HTTPS endpoint.
- No analytics, ads, crash reporting, or third-party tracking SDK is used in the Android APK.

## Data Inventory

### User Provided

| Data | Stored locally | Transmitted | Purpose |
| --- | --- | --- | --- |
| API endpoint URL | SharedPreferences | Used for all API calls | User-selected server |
| API key | Encrypted SharedPreferences | API header to configured endpoint | Authentication |
| Device ID | Encrypted SharedPreferences | API path/body where needed | Device registration |
| Device name | Encrypted SharedPreferences | Registration/heartbeat | User-friendly device label |
| SMS filter rules | Encrypted SharedPreferences | Not transmitted by app | Local inbound SMS filtering |
| Preferred SIM ID | SharedPreferences integer | Registration/heartbeat as SIM metadata | SIM selection |

### SMS Data

| Data | Collection scenario | Transmitted |
| --- | --- | --- |
| Recipient phone number | Outbound SMS command queued/sent | To configured endpoint as status context/server already provided it |
| SMS body | Outbound send command and inbound receive forwarding | To configured endpoint |
| Sender phone number | Inbound SMS receive forwarding | To configured endpoint |
| Received timestamp | Inbound SMS receive forwarding | To configured endpoint |
| SMS delivery status | Sent/delivered/failed callbacks | To configured endpoint |
| Error code/message | Failed SMS status | To configured endpoint |
| Message fingerprint | Local dedupe/work name/status payload | To configured endpoint if included in DTO |

### Device and App Data

| Data | Transmitted | Purpose |
| --- | --- | --- |
| Brand/manufacturer/model | Registration/update | Device identification in dashboard |
| Build ID/base OS | Registration/update | Device compatibility/support |
| App version name/code | Registration/heartbeat | Release support |
| Battery percentage/charging | Heartbeat | Gateway health |
| Network type | Heartbeat | Gateway health |
| Device uptime | Heartbeat | Gateway health |
| Receive SMS enabled | Heartbeat | Gateway state |
| SMS send delay | Heartbeat | Gateway configuration |
| SIM carrier/display name/slot/type/subscription ID | Registration/heartbeat | SIM selection and routing |

Removed from Android collection:

- Firebase token
- SIM ICC ID
- SIM card ID
- MCC/MNC/country ISO
- Device serial DTO field
- Memory byte stats
- Storage byte stats
- Timezone
- Locale

## Third-Party Sharing

### Android APK

No third-party analytics, ads, crash reporting, or push SDK is packaged in the Android app after hardening.

The app sends data to the user-configured HTTPS endpoint. If that endpoint is operated by the developer/service, disclose it as first-party collection. If the user configures their own server, disclose it as user-directed transfer to the endpoint they choose.

### Repository Ecosystem

Not packaged in the Android APK:

- `api/` still contains `firebase-admin` and FCM messaging paths.
- `web/` contains Google Analytics and Microsoft Clarity scripts.

If the same product privacy policy covers Android, web, and backend, disclose those web/server services or remove/gate them.

## Google Play Data Safety Draft

Use this as a draft. Final answers must match actual production backend behavior.

### Data Collection

Declare: Yes, the app collects/transmits user data off-device.

Data types likely required:

| Play category | Data | Purpose | Required? |
| --- | --- | --- | --- |
| Messages | SMS content, sender, recipient, timestamps, delivery status | App functionality | Yes |
| Personal info | Phone numbers in SMS sender/recipient fields | App functionality | Yes |
| Device or other IDs | App device ID, SIM subscription ID | App functionality/account management | Yes |
| App info and performance | App version, gateway enabled state, receive SMS state, error/status metadata | App functionality, diagnostics | Yes |
| Device info | Device model/manufacturer, battery/network state, SIM carrier/display/slot/type | App functionality | Yes |
| Photos/videos | None collected | QR scanner camera only, no image upload | No collection |
| Location | None | Not requested | No |
| Contacts | None | Not requested | No |
| Files/docs | None | Not requested | No |
| Financial info | None | Not in Android APK | No |
| Health/fitness | None | Not requested | No |

### Sharing

Suggested answer:

- Shared with third parties: No for Android SDKs/trackers.
- If user configures a non-developer endpoint, treat as user-directed transfer and explain clearly.
- If the default service endpoint is operated by TextBee, treat as first-party collection by the developer/service provider.

### Security Practices

- Data encrypted in transit: Yes, HTTPS-only endpoint validation and cleartext disabled.
- Data encrypted at rest: Yes for local API key, device ID/name, filter config, and queued SMS payloads.
- Users can request data deletion: Must be true only after you provide working account/data deletion flow.
- Independent security review: No unless completed.

## Privacy Policy Requirements

The policy must be public, active, non-PDF, non-editable, and linked in Play Console and in/near the app/service.

It must disclose:

- Developer/company identity and privacy contact.
- The app's purpose as an SMS gateway.
- SMS permissions used and why.
- Phone-state permission used and why.
- The data types listed above.
- That data is transmitted to the configured HTTPS endpoint.
- Whether the endpoint is first-party, user-hosted, or user-selected.
- Retention periods for SMS records, device metadata, API keys, logs, and backups.
- Data deletion/account deletion process.
- Security practices: HTTPS, local encryption, backup disabled.
- No Android ads/tracking SDKs in the APK.
- Any web analytics/server Firebase usage if the policy covers the web/backend service.
- Child-directed use: state not intended for children if applicable.
- Lawful use restrictions: user must only use the app with devices/SIMs/accounts they own or are authorized to manage.

## Privacy Gaps Before Publishing

| Gap | Severity | Required action |
| --- | --- | --- |
| Final privacy policy not verified | High | Publish and link policy before Play submission. |
| Data deletion flow not verified | High | Provide in-app and web deletion path if accounts exist. |
| Backend FCM/web analytics not reconciled | Medium | Remove/gate or disclose in service policy. |
| Merged manifest camera permission not verified | Medium | After build, inspect merged manifest and declare camera access if present. |

