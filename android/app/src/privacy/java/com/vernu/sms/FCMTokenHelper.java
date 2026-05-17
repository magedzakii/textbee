package com.vernu.sms;

/**
 * Privacy flavor: FCM is not used. Token operations return empty results immediately.
 */
public class FCMTokenHelper {

    public interface TokenCallback {
        void onTokenReceived(String token);
        void onTokenFailed();
    }

    /**
     * Privacy flavor: no FCM. Immediately invokes onTokenReceived with an empty string
     * so that callers can proceed without an FCM token.
     */
    public static void getTokenAsync(TokenCallback callback) {
        callback.onTokenReceived("");
    }

    /**
     * Privacy flavor: no FCM. Returns null.
     */
    public static String getTokenSync() {
        return null;
    }
}
