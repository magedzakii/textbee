package com.vernu.sms.helpers;

import android.content.Context;

import com.vernu.sms.AppConstants;
import com.vernu.sms.BuildConfig;

public class VersionTracker {
    /**
     * Updates the stored version information with current version
     * @param context Application context
     */
    public static void updateStoredVersion(Context context) {
        SharedPreferenceHelper.setSharedPreferenceInt(
                context,
                AppConstants.SHARED_PREFS_LAST_VERSION_CODE_KEY,
                BuildConfig.VERSION_CODE
        );
        
        SharedPreferenceHelper.setSharedPreferenceString(
                context,
                AppConstants.SHARED_PREFS_LAST_VERSION_NAME_KEY,
                BuildConfig.VERSION_NAME
        );
    }
}
