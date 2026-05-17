package com.vernu.sms;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FCMTokenHelper {
    private static final String TAG = "FCMTokenHelper";

    public interface TokenCallback {
        void onTokenReceived(String token);
        void onTokenFailed();
    }

    /**
     * Retrieves the FCM token asynchronously.
     */
    public static void getTokenAsync(TokenCallback callback) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "Failed to get FCM token: " + task.getException());
                        callback.onTokenFailed();
                        return;
                    }
                    callback.onTokenReceived(task.getResult());
                });
    }

    /**
     * Retrieves the FCM token synchronously (blocks up to 5 seconds).
     * Returns null if unable to obtain a token.
     */
    public static String getTokenSync() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            final String[] fcmToken = new String[1];
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    fcmToken[0] = task.getResult();
                }
                latch.countDown();
            });
            if (latch.await(5, TimeUnit.SECONDS)) {
                return fcmToken[0];
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get FCM token: " + e.getMessage());
        }
        return null;
    }
}
