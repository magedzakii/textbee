package com.vernu.sms;

import android.content.Context;

import com.vernu.sms.helpers.SharedPreferenceHelper;
import com.vernu.sms.services.GatewayApiService;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiManager {
    private static GatewayApiService apiService;
    private static String currentBaseUrl = null;

    /**
     * Returns a GatewayApiService using the user-configured base URL (from SharedPreferences)
     * or falls back to the build-time constant. Returns null if no URL is available.
     */
    public static synchronized GatewayApiService getApiService(Context context) {
        String baseUrl = resolveBaseUrl(context);
        if (baseUrl == null || baseUrl.isEmpty()) {
            return null;
        }
        if (apiService == null || !baseUrl.equals(currentBaseUrl)) {
            currentBaseUrl = baseUrl;
            apiService = createApiService(baseUrl);
        }
        return apiService;
    }

    /**
     * Returns a GatewayApiService using the build-time API_BASE_URL constant.
     * For the privacy flavor use {@link #getApiService(Context)} instead.
     */
    public static GatewayApiService getApiService() {
        return getApiService(null);
    }

    /**
     * Clears the cached service instance so that the next call to getApiService
     * creates a new one (e.g., after the user changes the base URL).
     */
    public static synchronized void resetApiService() {
        apiService = null;
        currentBaseUrl = null;
    }

    private static String resolveBaseUrl(Context context) {
        if (context != null) {
            String savedUrl = SharedPreferenceHelper.getSharedPreferenceString(
                    context, AppConstants.SHARED_PREFS_API_BASE_URL_KEY, "");
            if (!savedUrl.isEmpty()) {
                return savedUrl;
            }
        }
        // Fall back to build-time constant (empty string for privacy flavor when not configured)
        String buildUrl = AppConstants.API_BASE_URL;
        return (buildUrl != null && !buildUrl.isEmpty()) ? buildUrl : null;
    }

    private static GatewayApiService createApiService(String baseUrl) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit.create(GatewayApiService.class);
    }
}
