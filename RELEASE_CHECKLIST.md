# Release Checklist

Audit date: 2026-05-17

## Current Blockers

- JDK is missing in this environment.
- `JAVA_HOME` is not set.
- Release lint/AAB build could not run.
- Upload signing keystore env vars are not configured here.
- Google Play SMS permission approval is not complete.
- Privacy Policy/Data Safety/SMS declaration are not complete.

## Required Local Environment

Install/configure:

- JDK 17
- Android SDK Platform 35
- Android SDK Build Tools 35.0.0
- Gradle wrapper from repo
- Upload keystore for Play App Signing

Required signing environment variables:

```bash
export ANDROID_UPLOAD_STORE_FILE=/absolute/path/to/upload-keystore.jks
export ANDROID_UPLOAD_STORE_PASSWORD='...'
export ANDROID_UPLOAD_KEY_ALIAS='...'
export ANDROID_UPLOAD_KEY_PASSWORD='...'
```

Required JDK:

```bash
export JAVA_HOME=/path/to/jdk17
export PATH="$JAVA_HOME/bin:$PATH"
```

## Build Validation Commands

Run from `android/`:

```bash
./gradlew --version
./gradlew :app:clean
./gradlew :app:lintProdRelease
./gradlew :app:testProdReleaseUnitTest
./gradlew :app:bundleProdRelease
```

Expected release artifact:

```text
android/app/build/outputs/bundle/prodRelease/app-prod-release.aab
```

Optional APK smoke build:

```bash
./gradlew :app:assembleProdRelease
```

## Artifact Verification

After a successful AAB:

```bash
jarsigner -verify -verbose -certs android/app/build/outputs/bundle/prodRelease/app-prod-release.aab
```

If `bundletool` is installed:

```bash
bundletool validate --bundle=android/app/build/outputs/bundle/prodRelease/app-prod-release.aab
bundletool dump manifest --bundle=android/app/build/outputs/bundle/prodRelease/app-prod-release.aab
```

Verify merged manifest:

- `targetSdkVersion` is 35.
- No Firebase/FCM services.
- No foreground service declarations.
- No boot receiver.
- No `READ_SMS`.
- Cleartext traffic is false.
- Exported components match `COMPLIANCE_REPORT.md`.
- Check whether ZXing contributes `CAMERA`; if yes, Play declarations/privacy policy must mention QR scanning camera access.

## Runtime QA Matrix

Test on Android 10, 12, 13, 14, and 15 devices where possible:

- Fresh install.
- Permission disclosure appears before system permission prompt.
- Denying SMS permissions leaves gateway disabled/limited.
- Granting SMS permissions works.
- API endpoint rejects `http://`.
- API endpoint rejects URL credentials/query/fragment.
- API endpoint accepts valid `https://host/path/`.
- API key is saved and survives restart.
- Device registration succeeds.
- Gateway toggle updates backend.
- Receive SMS is off by default.
- Receive SMS only forwards after user enables it.
- SMS filtering works locally.
- Sending SMS from default SIM works.
- Sending SMS from selected SIM works on multi-SIM devices.
- Delivery status callbacks work.
- App restart does not auto-send to old hardcoded endpoints.
- No crashes when endpoint is missing.
- No crashes when API returns errors.
- Airplane mode/no service handling is understandable.
- WorkManager retries only against configured endpoint.

## Security QA

- Inspect logcat during normal use and confirm no API key, SMS body, phone number, sender, or device ID is logged.
- Check app data backup is disabled.
- Check no cleartext network calls with a proxy or Android Network Security logs.
- Verify release build is not debuggable.
- Verify R8 does not break JSON field serialization.
- Verify no dynamic code or self-update behavior exists.

## Play Console Submission Checklist

### App Content

- Privacy Policy URL.
- Data Safety form.
- SMS Permissions Declaration form.
- App access instructions for reviewers.
- Data deletion URL if accounts exist.
- Content rating questionnaire.
- Target audience questionnaire.
- Ads declaration: No ads, if still true.
- Foreground service declaration: None, if merged manifest confirms none.
- Full-screen intent declaration: None.

### SMS Permissions Declaration Evidence

Prepare:

- Demo video showing:
  - User opens app.
  - User sees SMS permission disclosure.
  - User configures HTTPS endpoint/API key.
  - User grants permissions.
  - User sends SMS through the gateway.
  - User enables inbound SMS receiving manually.
  - User sees/uses SMS gateway as the core app purpose.
- Store listing text that clearly describes SMS gateway behavior.
- Test credentials/API key and endpoint for reviewer.
- Explanation of why alternatives like SMS Intent/SMS Retriever do not satisfy the core gateway functionality.
- Explanation that no SMS data is sold or used for ads/tracking.

### Store Listing

- App title and screenshots must not impersonate another brand.
- Short/long description must clearly state SMS gateway purpose.
- Do not imply Google/carrier endorsement.
- Do not mention stealth, hidden monitoring, spying, or bypassing carrier limits.
- Include support contact.

## Pre-Launch Release Flow

1. Build signed AAB.
2. Upload to internal testing.
3. Complete all App Content forms.
4. Run Play pre-launch report.
5. Fix any crashes, ANRs, policy warnings, or device compatibility warnings.
6. Run closed testing with real SMS devices/SIMs.
7. Confirm Data Safety and Privacy Policy match actual runtime behavior.
8. Submit for review only after SMS declaration packet is complete.

## Final Go/No-Go Gate

Go only when all are true:

- `:app:lintProdRelease` passes.
- `:app:testProdReleaseUnitTest` passes.
- `:app:bundleProdRelease` passes.
- Signed AAB verified.
- Merged manifest reviewed.
- SMS declaration completed and defensible.
- Privacy Policy and Data Safety completed.
- Closed testing passes.
- Pre-launch report has no serious issues.

