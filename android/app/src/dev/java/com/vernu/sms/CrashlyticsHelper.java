package com.vernu.sms;

import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.Map;

public class CrashlyticsHelper {
    private static final String TAG = "CrashlyticsHelper";

    public static void setCustomKeys(String deviceId, String deviceModel, String appVersion, int appVersionCode) {
        try {
            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
            crashlytics.setCustomKey("device_id", deviceId != null ? deviceId : "not_registered");
            crashlytics.setCustomKey("device_model", deviceModel);
            crashlytics.setCustomKey("app_version", appVersion);
            crashlytics.setCustomKey("app_version_code", appVersionCode);
        } catch (Exception e) {
            Log.e(TAG, "Error setting Crashlytics custom keys", e);
        }
    }

    public static void logException(Throwable throwable, String message, Map<String, Object> customData) {
        try {
            Log.e(TAG, message, throwable);
            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
            crashlytics.log(message);
            if (customData != null) {
                for (Map.Entry<String, Object> entry : customData.entrySet()) {
                    if (entry.getValue() instanceof String) {
                        crashlytics.setCustomKey(entry.getKey(), (String) entry.getValue());
                    } else if (entry.getValue() instanceof Boolean) {
                        crashlytics.setCustomKey(entry.getKey(), (Boolean) entry.getValue());
                    } else if (entry.getValue() instanceof Integer) {
                        crashlytics.setCustomKey(entry.getKey(), (Integer) entry.getValue());
                    } else if (entry.getValue() instanceof Long) {
                        crashlytics.setCustomKey(entry.getKey(), (Long) entry.getValue());
                    } else if (entry.getValue() instanceof Float) {
                        crashlytics.setCustomKey(entry.getKey(), (Float) entry.getValue());
                    } else if (entry.getValue() instanceof Double) {
                        crashlytics.setCustomKey(entry.getKey(), (Double) entry.getValue());
                    } else if (entry.getValue() != null) {
                        crashlytics.setCustomKey(entry.getKey(), entry.getValue().toString());
                    }
                }
            }
            crashlytics.recordException(throwable);
        } catch (Exception e) {
            Log.e(TAG, "Error logging exception to Crashlytics", e);
        }
    }
}
