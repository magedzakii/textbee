package com.vernu.sms;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.vernu.sms.dtos.SimInfoDTO;

import java.util.ArrayList;
import java.util.List;

public class TextBeeUtils {
    private static final String TAG = "TextBeeUtils";
    
    public static boolean isPermissionGranted(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static List<SubscriptionInfo> getAvailableSimSlots(Context context) {

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return new ArrayList<>();
        }

        SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        return subscriptionManager.getActiveSubscriptionInfoList();

    }

    public static void logException(Throwable throwable, String message) {
        Log.e(TAG, message, throwable);
    }

    /**
     * Collects all available SIM information (physical SIMs and eSIMs) from the device
     * 
     * @param context The application context
     * @return List of SimInfoDTO objects containing SIM information, or empty list if permission not granted
     */
    public static List<SimInfoDTO> collectSimInfo(Context context) {
        List<SimInfoDTO> simInfoList = new ArrayList<>();

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "READ_PHONE_STATE permission not granted, cannot collect SIM info");
            return simInfoList;
        }

        try {
            SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
            List<SubscriptionInfo> subscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();

            if (subscriptionInfoList == null) {
                Log.d(TAG, "No active subscriptions found");
                return simInfoList;
            }

            for (SubscriptionInfo subscriptionInfo : subscriptionInfoList) {
                SimInfoDTO simInfo = new SimInfoDTO();
                simInfo.setSubscriptionId(subscriptionInfo.getSubscriptionId());

                // Get carrier name
                try {
                    CharSequence carrierName = subscriptionInfo.getCarrierName();
                    if (carrierName != null) {
                        simInfo.setCarrierName(carrierName.toString());
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Could not get carrier name for subscription");
                }

                // Get display name
                try {
                    CharSequence displayName = subscriptionInfo.getDisplayName();
                    if (displayName != null) {
                        simInfo.setDisplayName(displayName.toString());
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Could not get display name for subscription");
                }

                // Get SIM slot index
                try {
                    int simSlotIndex = subscriptionInfo.getSimSlotIndex();
                    if (simSlotIndex >= 0) {
                        simInfo.setSimSlotIndex(simSlotIndex);
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Could not get SIM slot index for subscription");
                }

                // Get subscription type (0 = physical SIM, 1 = eSIM)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        int subscriptionType = subscriptionInfo.getSubscriptionType();
                        if (subscriptionType == SubscriptionManager.SUBSCRIPTION_TYPE_LOCAL_SIM) {
                            simInfo.setSubscriptionType("PHYSICAL_SIM");
                        } else if (subscriptionType == SubscriptionManager.SUBSCRIPTION_TYPE_REMOTE_SIM) {
                            simInfo.setSubscriptionType("ESIM");
                        }
                    } else {
                        // For older Android versions, default to PHYSICAL_SIM
                        simInfo.setSubscriptionType("PHYSICAL_SIM");
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Could not get subscription type for subscription");
                }

                simInfoList.add(simInfo);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error collecting SIM info: " + e.getMessage(), e);
        }

        return simInfoList;
    }

    /**
     * Validates if a subscription ID exists in the active subscriptions
     * 
     * @param context The application context
     * @param subscriptionId The subscription ID to validate
     * @return true if the subscription ID exists, false otherwise
     */
    public static boolean isValidSubscriptionId(Context context, int subscriptionId) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        try {
            SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
            List<SubscriptionInfo> subscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();

            if (subscriptionInfoList == null) {
                return false;
            }

            for (SubscriptionInfo subscriptionInfo : subscriptionInfoList) {
                if (subscriptionInfo.getSubscriptionId() == subscriptionId) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error validating subscription ID: " + e.getMessage(), e);
        }

        return false;
    }
}
