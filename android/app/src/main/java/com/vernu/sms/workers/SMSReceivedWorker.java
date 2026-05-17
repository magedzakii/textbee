package com.vernu.sms.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.gson.Gson;
import com.vernu.sms.ApiManager;
import com.vernu.sms.AppConstants;
import com.vernu.sms.dtos.SMSDTO;
import com.vernu.sms.dtos.SMSForwardResponseDTO;
import com.vernu.sms.helpers.SecurePreferenceCrypto;
import com.vernu.sms.helpers.SharedPreferenceHelper;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Response;

public class SMSReceivedWorker extends Worker {
    private static final String TAG = "SMSReceivedWorker";
    private static final int MAX_RETRIES = 5;
    
    public static final String KEY_DEVICE_ID = "device_id";
    public static final String KEY_SMS_DTO = "sms_dto";
    public static final String KEY_RETRY_COUNT = "retry_count";
    
    public SMSReceivedWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        String deviceId = SharedPreferenceHelper.getSharedPreferenceString(
                getApplicationContext(),
                AppConstants.SHARED_PREFS_DEVICE_ID_KEY,
                ""
        );
        if (deviceId.isEmpty()) {
            deviceId = getInputData().getString(KEY_DEVICE_ID);
        }
        String apiKey = SharedPreferenceHelper.getSharedPreferenceString(
                getApplicationContext(),
                AppConstants.SHARED_PREFS_API_KEY_KEY,
                ""
        );
        String smsDtoJson = getInputData().getString(KEY_SMS_DTO);
        int retryCount = getInputData().getInt(KEY_RETRY_COUNT, 0);
        
        if (deviceId == null || apiKey.isEmpty() || smsDtoJson == null) {
            Log.e(TAG, "Missing required parameters");
            return Result.failure();
        }
        
        // Check if we've exceeded the maximum retry count
        if (retryCount >= MAX_RETRIES) {
            Log.e(TAG, "Maximum retry count reached for received SMS");
            return Result.failure();
        }
        
        SMSDTO smsDTO;
        try {
            String decryptedSmsDtoJson = SecurePreferenceCrypto.decrypt(smsDtoJson);
            smsDTO = new Gson().fromJson(decryptedSmsDtoJson, SMSDTO.class);
        } catch (Exception e) {
            Log.e(TAG, "Failed to decrypt queued received SMS payload", e);
            return Result.failure();
        }
        
        try {
            Call<SMSForwardResponseDTO> call = ApiManager.getApiService(getApplicationContext()).sendReceivedSMS(deviceId, apiKey, smsDTO);
            Response<SMSForwardResponseDTO> response = call.execute();
            
            if (response.isSuccessful()) {
                Log.d(TAG, "Received SMS sent to server successfully");
                return Result.success();
            } else {
                Log.e(TAG, "Failed to send received SMS to server. Response code: " + response.code());
                return Result.retry();
            }
        } catch (IOException e) {
            Log.e(TAG, "API call failed: " + e.getMessage());
            return Result.retry();
        } catch (IllegalStateException e) {
            Log.e(TAG, "API endpoint is not configured: " + e.getMessage());
            return Result.retry();
        }
    }
    
    public static void enqueueWork(Context context, String deviceId, SMSDTO smsDTO) {
        String smsDtoPayload;
        try {
            smsDtoPayload = SecurePreferenceCrypto.encrypt(new Gson().toJson(smsDTO));
        } catch (Exception e) {
            Log.e(TAG, "Failed to encrypt received SMS work payload", e);
            return;
        }

        Data inputData = new Data.Builder()
                .putString(KEY_SMS_DTO, smsDtoPayload)
                .putInt(KEY_RETRY_COUNT, 0)
                .build();
        
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(SMSReceivedWorker.class)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .setInputData(inputData)
                .addTag("sms_received")
                .build();
        
        // Use fingerprint for unique work name if available, otherwise fallback to timestamp
        String uniqueWorkName;
        if (smsDTO.getFingerprint() != null && !smsDTO.getFingerprint().isEmpty()) {
            uniqueWorkName = "sms_received_" + smsDTO.getFingerprint();
        } else {
            // Fallback to timestamp if fingerprint is not available
            uniqueWorkName = "sms_received_" + System.currentTimeMillis();
            Log.w(TAG, "Fingerprint not available, using timestamp for work name");
        }
        
        WorkManager.getInstance(context)
                .beginUniqueWork(uniqueWorkName, 
                        androidx.work.ExistingWorkPolicy.KEEP, 
                        workRequest)
                .enqueue();
        
        Log.d(TAG, "Work enqueued for received SMS");
    }
}
