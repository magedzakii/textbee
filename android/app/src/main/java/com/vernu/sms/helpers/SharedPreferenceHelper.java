package com.vernu.sms.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.vernu.sms.AppConstants;

public class SharedPreferenceHelper {
    private final static String PREF_FILE = "PREF";
    private static final String TAG = "SharedPreferenceHelper";


    public static void setSharedPreferenceString(Context context, String key, String value) {
        SharedPreferences settings = context.getSharedPreferences(PREF_FILE, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, prepareStringForStorage(key, value));
        editor.apply();
    }

    public static void setSharedPreferenceInt(Context context, String key, int value) {
        SharedPreferences settings = context.getSharedPreferences(PREF_FILE, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public static void setSharedPreferenceBoolean(Context context, String key, boolean value) {
        SharedPreferences settings = context.getSharedPreferences(PREF_FILE, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static String getSharedPreferenceString(Context context, String key, String defValue) {
        SharedPreferences settings = context.getSharedPreferences(PREF_FILE, 0);
        String value = settings.getString(key, defValue);
        return readStoredString(context, key, value, defValue);
    }


    public static int getSharedPreferenceInt(Context context, String key, int defValue) {
        SharedPreferences settings = context.getSharedPreferences(PREF_FILE, 0);
        return settings.getInt(key, defValue);
    }


    public static boolean getSharedPreferenceBoolean(Context context, String key, boolean defValue) {
        SharedPreferences settings = context.getSharedPreferences(PREF_FILE, 0);
        return settings.getBoolean(key, defValue);
    }

    public static void clearSharedPreference(Context context, String key) {
        SharedPreferences settings = context.getSharedPreferences(PREF_FILE, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.remove(key);
        editor.apply();
    }

    private static String prepareStringForStorage(String key, String value) {
        if (!shouldEncrypt(key) || value == null || value.isEmpty() || SecurePreferenceCrypto.isEncrypted(value)) {
            return value;
        }

        try {
            return SecurePreferenceCrypto.encrypt(value);
        } catch (Exception e) {
            Log.e(TAG, "Failed to encrypt preference " + key, e);
            return "";
        }
    }

    private static String readStoredString(Context context, String key, String value, String defValue) {
        if (!shouldEncrypt(key) || value == null || value.isEmpty()) {
            return value;
        }

        if (SecurePreferenceCrypto.isEncrypted(value)) {
            try {
                return SecurePreferenceCrypto.decrypt(value);
            } catch (Exception e) {
                Log.e(TAG, "Failed to decrypt preference " + key, e);
                return defValue;
            }
        }

        migratePlaintextString(context, key, value);
        return value;
    }

    private static void migratePlaintextString(Context context, String key, String value) {
        try {
            String encryptedValue = SecurePreferenceCrypto.encrypt(value);
            SharedPreferences settings = context.getSharedPreferences(PREF_FILE, 0);
            settings.edit().putString(key, encryptedValue).apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to migrate plaintext preference " + key, e);
        }
    }

    private static boolean shouldEncrypt(String key) {
        return AppConstants.SHARED_PREFS_API_KEY_KEY.equals(key)
                || AppConstants.SHARED_PREFS_DEVICE_ID_KEY.equals(key)
                || AppConstants.SHARED_PREFS_DEVICE_NAME_KEY.equals(key)
                || AppConstants.SHARED_PREFS_SMS_FILTER_CONFIG_KEY.equals(key);
    }
}
