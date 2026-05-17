package com.vernu.sms;

import android.content.Context;

import com.vernu.sms.helpers.SharedPreferenceHelper;

import java.net.URI;

public class ApiEndpointConfig {
    public static ValidationResult validateAndNormalize(String rawUrl) {
        if (rawUrl == null) {
            return ValidationResult.invalid("API endpoint is required");
        }

        String trimmedUrl = rawUrl.trim();
        if (trimmedUrl.isEmpty()) {
            return ValidationResult.invalid("API endpoint is required");
        }

        if (trimmedUrl.matches(".*\\s+.*")) {
            return ValidationResult.invalid("API endpoint must not contain spaces");
        }

        URI uri;
        try {
            uri = new URI(trimmedUrl);
        } catch (Exception e) {
            return ValidationResult.invalid("API endpoint must be a valid URL");
        }

        if (uri.isOpaque() || uri.getScheme() == null || !"https".equalsIgnoreCase(uri.getScheme())) {
            return ValidationResult.invalid("API endpoint must use https://");
        }

        if (uri.getHost() == null || uri.getHost().trim().isEmpty()) {
            return ValidationResult.invalid("API endpoint must include a host");
        }

        if (uri.getRawUserInfo() != null) {
            return ValidationResult.invalid("API endpoint must not include credentials");
        }

        if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
            return ValidationResult.invalid("API endpoint must not include query parameters or fragments");
        }

        String normalizedUrl = trimmedUrl.endsWith("/") ? trimmedUrl : trimmedUrl + "/";
        return ValidationResult.valid(normalizedUrl);
    }

    public static String getApiBaseUrl(Context context) {
        return SharedPreferenceHelper.getSharedPreferenceString(
                context,
                AppConstants.SHARED_PREFS_API_BASE_URL_KEY,
                ""
        );
    }

    public static ValidationResult saveApiBaseUrl(Context context, String rawUrl) {
        ValidationResult result = validateAndNormalize(rawUrl);
        if (!result.isValid()) {
            return result;
        }

        SharedPreferenceHelper.setSharedPreferenceString(
                context,
                AppConstants.SHARED_PREFS_API_BASE_URL_KEY,
                result.getUrl()
        );
        ApiManager.reset();
        return result;
    }

    public static class ValidationResult {
        private final boolean valid;
        private final String url;
        private final String message;

        private ValidationResult(boolean valid, String url, String message) {
            this.valid = valid;
            this.url = url;
            this.message = message;
        }

        public static ValidationResult valid(String url) {
            return new ValidationResult(true, url, null);
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, null, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getUrl() {
            return url;
        }

        public String getMessage() {
            return message;
        }
    }
}
