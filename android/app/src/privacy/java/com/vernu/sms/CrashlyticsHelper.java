package com.vernu.sms;

import android.util.Log;

import java.util.Map;

/**
 * Privacy flavor: no crash reporting. Exceptions are logged locally only.
 */
public class CrashlyticsHelper {
    private static final String TAG = "CrashlyticsHelper";

    public static void setCustomKeys(String deviceId, String deviceModel, String appVersion, int appVersionCode) {
        // No-op: privacy flavor does not use Crashlytics
    }

    public static void logException(Throwable throwable, String message, Map<String, Object> customData) {
        // Log locally only; no data leaves the device
        Log.e(TAG, message, throwable);
    }
}
