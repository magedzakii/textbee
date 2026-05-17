package com.vernu.sms;

import android.content.Context;

import com.vernu.sms.services.GatewayApiService;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiManager {
    private static GatewayApiService apiService;
    private static String apiBaseUrl;

    public static synchronized GatewayApiService getApiService(Context context) {
        ApiEndpointConfig.ValidationResult validationResult = ApiEndpointConfig.validateAndNormalize(
                ApiEndpointConfig.getApiBaseUrl(context.getApplicationContext())
        );

        if (!validationResult.isValid()) {
            throw new IllegalStateException(validationResult.getMessage());
        }

        String configuredBaseUrl = validationResult.getUrl();
        if (apiService == null || !configuredBaseUrl.equals(apiBaseUrl)) {
            apiService = createApiService(configuredBaseUrl);
            apiBaseUrl = configuredBaseUrl;
        }
        return apiService;
    }

    public static synchronized void reset() {
        apiService = null;
        apiBaseUrl = null;
    }

    private static GatewayApiService createApiService(String configuredBaseUrl) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(configuredBaseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(GatewayApiService.class);
    }
}
