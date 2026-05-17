package com.vernu.sms.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Worker;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;
import com.vernu.sms.AppConstants;
import com.vernu.sms.TextBeeUtils;
import com.vernu.sms.helpers.SecurePreferenceCrypto;
import com.vernu.sms.helpers.SMSHelper;
import com.vernu.sms.helpers.SharedPreferenceHelper;
import com.vernu.sms.models.SMSPayload;

public class SmsSendWorker extends Worker {
    private static final String TAG = "SmsSendWorker";
    private static final String QUEUE_NAME = "sms_send_queue";

    public static final String KEY_PHONE = "phone";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_SMS_ID = "sms_id";
    public static final String KEY_SMS_BATCH_ID = "sms_batch_id";
    public static final String KEY_SIM_SUBSCRIPTION_ID = "sim_subscription_id";
    public static final String KEY_SMS_PAYLOAD = "sms_payload";

    public SmsSendWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        WorkPayload payload = readWorkPayload();
        if (payload == null) {
            return Result.failure();
        }

        if (payload.phone == null || payload.message == null || payload.smsId == null) {
            Log.e(TAG, "Missing required parameters");
            return Result.failure();
        }

        Context context = getApplicationContext();

        // Resolve SIM: backend-provided > app preference > device default
        Integer resolvedSim = resolveSim(context, payload.simSubscriptionId);

        if (resolvedSim != null) {
            SMSHelper.sendSMSFromSpecificSim(payload.phone, payload.message, resolvedSim, payload.smsId, payload.smsBatchId, context);
        } else {
            SMSHelper.sendSMS(payload.phone, payload.message, payload.smsId, payload.smsBatchId, context);
        }

        // Enforce rate limit delay
        int delaySeconds = SharedPreferenceHelper.getSharedPreferenceInt(
                context, AppConstants.SHARED_PREFS_SMS_SEND_DELAY_SECONDS_KEY, AppConstants.DEFAULT_SMS_SEND_DELAY_SECONDS);
        delaySeconds = Math.max(0, Math.min(delaySeconds, 3600));

        if (delaySeconds > 0) {
            try {
                Thread.sleep(delaySeconds * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return Result.success();
    }

    private WorkPayload readWorkPayload() {
        String encryptedPayload = getInputData().getString(KEY_SMS_PAYLOAD);
        if (encryptedPayload != null) {
            try {
                String payloadJson = SecurePreferenceCrypto.decrypt(encryptedPayload);
                SMSPayload smsPayload = new Gson().fromJson(payloadJson, SMSPayload.class);
                if (smsPayload == null || smsPayload.getRecipients() == null || smsPayload.getRecipients().length == 0) {
                    Log.e(TAG, "Queued SMS payload is incomplete");
                    return null;
                }
                return new WorkPayload(
                        smsPayload.getRecipients()[0],
                        smsPayload.getMessage(),
                        smsPayload.getSmsId(),
                        smsPayload.getSmsBatchId(),
                        smsPayload.getSimSubscriptionId() != null ? smsPayload.getSimSubscriptionId() : -1
                );
            } catch (Exception e) {
                Log.e(TAG, "Failed to decrypt queued SMS send payload", e);
                return null;
            }
        }

        return new WorkPayload(
                getInputData().getString(KEY_PHONE),
                getInputData().getString(KEY_MESSAGE),
                getInputData().getString(KEY_SMS_ID),
                getInputData().getString(KEY_SMS_BATCH_ID),
                getInputData().getInt(KEY_SIM_SUBSCRIPTION_ID, -1)
        );
    }

    private Integer resolveSim(Context context, int backendSimId) {
        // Priority 1: backend-provided SIM
        if (backendSimId != -1 && TextBeeUtils.isValidSubscriptionId(context, backendSimId)) {
            Log.d(TAG, "Using backend-provided SIM subscription");
            return backendSimId;
        }

        // Priority 2: app preference
        int preferredSim = SharedPreferenceHelper.getSharedPreferenceInt(
                context, AppConstants.SHARED_PREFS_PREFERRED_SIM_KEY, -1);
        if (preferredSim != -1 && TextBeeUtils.isValidSubscriptionId(context, preferredSim)) {
            Log.d(TAG, "Using app-preferred SIM subscription");
            return preferredSim;
        }

        // Priority 3: device default
        return null;
    }

    public static void enqueue(Context context, String phone, String message,
                               String smsId, String smsBatchId, Integer simSubscriptionId) {
        SMSPayload smsPayload = new SMSPayload();
        smsPayload.setRecipients(new String[]{phone});
        smsPayload.setMessage(message);
        smsPayload.setSmsId(smsId);
        smsPayload.setSmsBatchId(smsBatchId);
        smsPayload.setSimSubscriptionId(simSubscriptionId);

        String encryptedPayload;
        try {
            encryptedPayload = SecurePreferenceCrypto.encrypt(new Gson().toJson(smsPayload));
        } catch (Exception e) {
            Log.e(TAG, "Failed to encrypt SMS send work payload", e);
            return;
        }

        Data inputData = new Data.Builder()
                .putString(KEY_SMS_PAYLOAD, encryptedPayload)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(SmsSendWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(context)
                .beginUniqueWork(QUEUE_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, workRequest)
                .enqueue();

        Log.d(TAG, "SMS enqueued for sending");
    }

    private static class WorkPayload {
        final String phone;
        final String message;
        final String smsId;
        final String smsBatchId;
        final int simSubscriptionId;

        WorkPayload(String phone, String message, String smsId, String smsBatchId, int simSubscriptionId) {
            this.phone = phone;
            this.message = message;
            this.smsId = smsId;
            this.smsBatchId = smsBatchId;
            this.simSubscriptionId = simSubscriptionId;
        }
    }
}
