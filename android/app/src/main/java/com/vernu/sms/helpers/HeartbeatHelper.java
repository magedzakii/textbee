package com.vernu.sms.helpers;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.SystemClock;
import android.util.Log;

import com.vernu.sms.ApiManager;
import com.vernu.sms.AppConstants;
import com.vernu.sms.BuildConfig;
import com.vernu.sms.dtos.HeartbeatInputDTO;
import com.vernu.sms.dtos.HeartbeatResponseDTO;
import com.vernu.sms.dtos.SimInfoCollectionDTO;
import com.vernu.sms.TextBeeUtils;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

public class HeartbeatHelper {
    private static final String TAG = "HeartbeatHelper";

    /**
     * Collects device information and sends a heartbeat request to the API.
     * 
     * @param context Application context
     * @param deviceId Device ID
     * @param apiKey API key for authentication
     * @return true if heartbeat was sent successfully, false otherwise
     */
    public static boolean sendHeartbeat(Context context, String deviceId, String apiKey) {
        if (deviceId == null || deviceId.isEmpty()) {
            Log.d(TAG, "Device not registered, skipping heartbeat");
            return false;
        }

        if (apiKey == null || apiKey.isEmpty()) {
            Log.e(TAG, "API key not available, skipping heartbeat");
            return false;
        }

        // Collect device information
        HeartbeatInputDTO heartbeatInput = new HeartbeatInputDTO();

        try {
            // Get battery information
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, ifilter);
            if (batteryStatus != null) {
                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int batteryPct = (int) ((level / (float) scale) * 100);
                heartbeatInput.setBatteryPercentage(batteryPct);

                int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;
                heartbeatInput.setIsCharging(isCharging);
            }

            // Get network type
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                Network activeNetwork = cm.getActiveNetwork();
                NetworkCapabilities capabilities = activeNetwork != null ? cm.getNetworkCapabilities(activeNetwork) : null;
                if (capabilities != null) {
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        heartbeatInput.setNetworkType("wifi");
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        heartbeatInput.setNetworkType("cellular");
                    } else {
                        heartbeatInput.setNetworkType("other");
                    }
                } else {
                    heartbeatInput.setNetworkType("none");
                }
            }

            // Get app version
            heartbeatInput.setAppVersionName(BuildConfig.VERSION_NAME);
            heartbeatInput.setAppVersionCode(BuildConfig.VERSION_CODE);

            // Get device uptime
            heartbeatInput.setDeviceUptimeMillis(SystemClock.uptimeMillis());

            // Get receive SMS enabled status
            boolean receiveSMSEnabled = SharedPreferenceHelper.getSharedPreferenceBoolean(
                context,
                AppConstants.SHARED_PREFS_RECEIVE_SMS_ENABLED_KEY,
                false
            );
            heartbeatInput.setReceiveSMSEnabled(receiveSMSEnabled);

            // SMS send delay (device queue)
            int smsSendDelaySeconds = SharedPreferenceHelper.getSharedPreferenceInt(
                context,
                AppConstants.SHARED_PREFS_SMS_SEND_DELAY_SECONDS_KEY,
                AppConstants.DEFAULT_SMS_SEND_DELAY_SECONDS
            );
            heartbeatInput.setSmsSendDelaySeconds(smsSendDelaySeconds);

            // Collect SIM information
            SimInfoCollectionDTO simInfoCollection = new SimInfoCollectionDTO();
            simInfoCollection.setLastUpdated(System.currentTimeMillis());
            simInfoCollection.setSims(TextBeeUtils.collectSimInfo(context));
            heartbeatInput.setSimInfo(simInfoCollection);

            // Send heartbeat request
            Call<HeartbeatResponseDTO> call = ApiManager.getApiService(context).heartbeat(deviceId, apiKey, heartbeatInput);
            Response<HeartbeatResponseDTO> response = call.execute();

            if (response.isSuccessful() && response.body() != null) {
                HeartbeatResponseDTO responseBody = response.body();

                // Sync device name from heartbeat response (ignore if blank)
                if (responseBody.name != null && !responseBody.name.trim().isEmpty()) {
                    SharedPreferenceHelper.setSharedPreferenceString(
                        context,
                        AppConstants.SHARED_PREFS_DEVICE_NAME_KEY,
                        responseBody.name
                    );
                    Log.d(TAG, "Synced device name from heartbeat");
                }
                
                Log.d(TAG, "Heartbeat sent successfully");
                return true;
            } else {
                Log.e(TAG, "Failed to send heartbeat. Response code: " + (response.code()));
                return false;
            }
        } catch (IOException e) {
            Log.e(TAG, "Heartbeat API call failed: " + e.getMessage());
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error collecting device information: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if device is eligible to send heartbeat (registered, enabled, heartbeat enabled).
     * 
     * @param context Application context
     * @return true if device is eligible, false otherwise
     */
    public static boolean isDeviceEligibleForHeartbeat(Context context) {
        // Check if device is registered
        String deviceId = SharedPreferenceHelper.getSharedPreferenceString(
            context,
            AppConstants.SHARED_PREFS_DEVICE_ID_KEY,
            ""
        );

        if (deviceId.isEmpty()) {
            return false;
        }

        // Check if device is enabled
        boolean deviceEnabled = SharedPreferenceHelper.getSharedPreferenceBoolean(
            context,
            AppConstants.SHARED_PREFS_GATEWAY_ENABLED_KEY,
            false
        );

        if (!deviceEnabled) {
            return false;
        }

        // Check if heartbeat feature is enabled
        boolean heartbeatEnabled = SharedPreferenceHelper.getSharedPreferenceBoolean(
            context,
            AppConstants.SHARED_PREFS_HEARTBEAT_ENABLED_KEY,
            true // Default to true
        );

        return heartbeatEnabled;
    }
}
