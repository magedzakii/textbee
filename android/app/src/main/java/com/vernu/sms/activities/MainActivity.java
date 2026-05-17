package com.vernu.sms.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.vernu.sms.ApiEndpointConfig;
import com.vernu.sms.ApiManager;
import com.vernu.sms.AppConstants;
import com.vernu.sms.BuildConfig;
import com.vernu.sms.TextBeeUtils;
import com.vernu.sms.R;
import com.vernu.sms.dtos.RegisterDeviceInputDTO;
import com.vernu.sms.dtos.RegisterDeviceResponseDTO;
import com.vernu.sms.dtos.SimInfoCollectionDTO;
import com.vernu.sms.helpers.SharedPreferenceHelper;
import com.vernu.sms.helpers.VersionTracker;
import com.vernu.sms.helpers.HeartbeatManager;
import com.google.gson.Gson;
import okhttp3.ResponseBody;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private Context mContext;
    private Switch gatewaySwitch, receiveSMSSwitch;
    private EditText apiEndpointEditText, apiKeyEditText, deviceIdEditText, deviceNameEditText, smsSendDelayEditText;
    private Button registerDeviceBtn, grantSMSPermissionBtn, scanQRBtn, configureFilterBtn;
    private ImageButton copyDeviceIdImgBtn;
    private TextView deviceBrandAndModelTxt, deviceIdTxt, appVersionNameTxt, appVersionCodeTxt;
    private RadioGroup defaultSimSlotRadioGroup;
    private static final int SCAN_QR_REQUEST_CODE = 49374;
    private static final int PERMISSION_REQUEST_CODE = 0;
    private static final long SMS_DELAY_SAVE_DEBOUNCE_MS = 3000L;
    private final Handler smsDelaySaveHandler = new Handler(Looper.getMainLooper());
    private Runnable smsDelaySaveRunnable;
    private String deviceId = null;
    private boolean suppressGatewaySwitchChange = false;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getApplicationContext();
        deviceId = SharedPreferenceHelper.getSharedPreferenceString(mContext, AppConstants.SHARED_PREFS_DEVICE_ID_KEY, "");
        setContentView(R.layout.activity_main);
        gatewaySwitch = findViewById(R.id.gatewaySwitch);
        receiveSMSSwitch = findViewById(R.id.receiveSMSSwitch);
        apiEndpointEditText = findViewById(R.id.apiEndpointEditText);
        apiKeyEditText = findViewById(R.id.apiKeyEditText);
        deviceIdEditText = findViewById(R.id.deviceIdEditText);
        deviceNameEditText = findViewById(R.id.deviceNameEditText);
        registerDeviceBtn = findViewById(R.id.registerDeviceBtn);
        grantSMSPermissionBtn = findViewById(R.id.grantSMSPermissionBtn);
        scanQRBtn = findViewById(R.id.scanQRButton);
        deviceBrandAndModelTxt = findViewById(R.id.deviceBrandAndModelTxt);
        deviceIdTxt = findViewById(R.id.deviceIdTxt);
        copyDeviceIdImgBtn = findViewById(R.id.copyDeviceIdImgBtn);
        defaultSimSlotRadioGroup = findViewById(R.id.defaultSimSlotRadioGroup);
        appVersionNameTxt = findViewById(R.id.appVersionNameTxt);
        appVersionCodeTxt = findViewById(R.id.appVersionCodeTxt);
        configureFilterBtn = findViewById(R.id.configureFilterBtn);
        smsSendDelayEditText = findViewById(R.id.smsSendDelayEditText);

        deviceIdTxt.setText(deviceId);
        deviceIdEditText.setText(deviceId);
        deviceBrandAndModelTxt.setText(Build.BRAND + " " + Build.MODEL);
        
        // Set app version information
        String versionName = BuildConfig.VERSION_NAME;
        appVersionNameTxt.setText(versionName);
        appVersionCodeTxt.setText(String.valueOf(BuildConfig.VERSION_CODE));
        
        boolean gatewayEnabled = SharedPreferenceHelper.getSharedPreferenceBoolean(mContext, AppConstants.SHARED_PREFS_GATEWAY_ENABLED_KEY, false);

        // Schedule heartbeat if device is enabled and registered
        if (gatewayEnabled && !deviceId.isEmpty()) {
            HeartbeatManager.scheduleHeartbeat(mContext);
            Log.d(TAG, "Scheduling heartbeat on app start");
        }

        if (deviceId == null || deviceId.isEmpty()) {
            registerDeviceBtn.setText("Register");
        } else {
            registerDeviceBtn.setText("Update");
        }

        String[] missingPermissions = Arrays.stream(AppConstants.requiredPermissions).filter(permission -> !TextBeeUtils.isPermissionGranted(mContext, permission)).toArray(String[]::new);
        if (missingPermissions.length == 0) {
            grantSMSPermissionBtn.setEnabled(false);
            grantSMSPermissionBtn.setText("Permission Granted");
            renderAvailableSimOptions();
        } else {
            Snackbar.make(grantSMSPermissionBtn, "Please Grant Required Permissions to continue: " + Arrays.toString(missingPermissions), Snackbar.LENGTH_SHORT).show();
            grantSMSPermissionBtn.setEnabled(true);
            grantSMSPermissionBtn.setOnClickListener(this::handleRequestPermissions);
        }

        copyDeviceIdImgBtn.setOnClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Device ID", deviceId);
            clipboard.setPrimaryClip(clip);
            Snackbar.make(view, "Copied", Snackbar.LENGTH_LONG).show();
        });

        apiEndpointEditText.setText(ApiEndpointConfig.getApiBaseUrl(mContext));
        apiKeyEditText.setText(SharedPreferenceHelper.getSharedPreferenceString(mContext, AppConstants.SHARED_PREFS_API_KEY_KEY, ""));
        String storedDeviceName = SharedPreferenceHelper.getSharedPreferenceString(mContext, AppConstants.SHARED_PREFS_DEVICE_NAME_KEY, "");
        if (storedDeviceName.isEmpty()) {
            deviceNameEditText.setText(Build.BRAND + " " + Build.MODEL);
        } else {
            deviceNameEditText.setText(storedDeviceName);
        }
        gatewaySwitch.setChecked(SharedPreferenceHelper.getSharedPreferenceBoolean(mContext, AppConstants.SHARED_PREFS_GATEWAY_ENABLED_KEY, false));
        gatewaySwitch.setOnCheckedChangeListener((compoundButton, isCheked) -> {
            if (suppressGatewaySwitchChange) {
                return;
            }

            View view = compoundButton.getRootView();
            compoundButton.setEnabled(false);
            if (!persistApiEndpoint(view)) {
                boolean storedGatewayState = SharedPreferenceHelper.getSharedPreferenceBoolean(
                        mContext,
                        AppConstants.SHARED_PREFS_GATEWAY_ENABLED_KEY,
                        false
                );
                suppressGatewaySwitchChange = true;
                compoundButton.setChecked(storedGatewayState);
                suppressGatewaySwitchChange = false;
                compoundButton.setEnabled(true);
                return;
            }

            String key = apiKeyEditText.getText().toString();

            RegisterDeviceInputDTO registerDeviceInput = new RegisterDeviceInputDTO();
            registerDeviceInput.setEnabled(isCheked);
            registerDeviceInput.setAppVersionCode(BuildConfig.VERSION_CODE);
            registerDeviceInput.setAppVersionName(BuildConfig.VERSION_NAME);

            Call<RegisterDeviceResponseDTO> apiCall = ApiManager.getApiService(mContext).updateDevice(deviceId, key, registerDeviceInput);
            apiCall.enqueue(new Callback<RegisterDeviceResponseDTO>() {
                @Override
                public void onResponse(Call<RegisterDeviceResponseDTO> call, Response<RegisterDeviceResponseDTO> response) {
                    Log.d(TAG, "Device update response received: " + response.code());
                    if (!response.isSuccessful()) {
                        Snackbar.make(view, extractErrorMessage(response), Snackbar.LENGTH_LONG).show();
                        compoundButton.setEnabled(true);
                        return;
                    }
                    Snackbar.make(view, "Gateway " + (isCheked ? "enabled" : "disabled"), Snackbar.LENGTH_LONG).show();
                    SharedPreferenceHelper.setSharedPreferenceBoolean(mContext, AppConstants.SHARED_PREFS_GATEWAY_ENABLED_KEY, isCheked);
                    boolean enabled = Boolean.TRUE.equals(Objects.requireNonNull(response.body()).data.get("enabled"));
                    suppressGatewaySwitchChange = true;
                    compoundButton.setChecked(enabled);
                    suppressGatewaySwitchChange = false;
                    if (enabled) {
                        // Schedule heartbeat
                        HeartbeatManager.scheduleHeartbeat(mContext);
                    } else {
                        // Cancel heartbeat
                        HeartbeatManager.cancelHeartbeat(mContext);
                    }
                    compoundButton.setEnabled(true);
                }
                @Override
                public void onFailure(Call<RegisterDeviceResponseDTO> call, Throwable t) {
                    Snackbar.make(view, "An error occurred :(", Snackbar.LENGTH_LONG).show();
                    Log.e(TAG, "API error updating device", t);
                    TextBeeUtils.logException(t, "Error updating device");
                    compoundButton.setEnabled(true);
                }
            });
        });

        receiveSMSSwitch.setChecked(SharedPreferenceHelper.getSharedPreferenceBoolean(mContext, AppConstants.SHARED_PREFS_RECEIVE_SMS_ENABLED_KEY, false));
        receiveSMSSwitch.setOnCheckedChangeListener((compoundButton, isCheked) -> {
            View view = compoundButton.getRootView();
            SharedPreferenceHelper.setSharedPreferenceBoolean(mContext, AppConstants.SHARED_PREFS_RECEIVE_SMS_ENABLED_KEY, isCheked);
            compoundButton.setChecked(isCheked);
            Snackbar.make(view, "Receive SMS " + (isCheked ? "enabled" : "disabled"), Snackbar.LENGTH_LONG).show();
        });

        // TODO: check gateway status/api key/device validity and update UI accordingly
        registerDeviceBtn.setOnClickListener(view -> {
            String _deviceId = SharedPreferenceHelper.getSharedPreferenceString(mContext, AppConstants.SHARED_PREFS_DEVICE_ID_KEY, "");
            if (_deviceId == null || _deviceId.isEmpty()) {
                handleRegisterDevice();
            } else {
                handleUpdateDevice();
            }
        });
        scanQRBtn.setOnClickListener(view -> {
            IntentIntegrator intentIntegrator = new IntentIntegrator(MainActivity.this);
            intentIntegrator.setPrompt("Open your configured dashboard and click Register Device to generate QR Code");
            intentIntegrator.setRequestCode(SCAN_QR_REQUEST_CODE);
            intentIntegrator.initiateScan();
        });

        configureFilterBtn.setOnClickListener(view -> {
            Intent filterIntent = new Intent(MainActivity.this, SMSFilterActivity.class);
            startActivity(filterIntent);
        });

        // SMS Send Delay setting: save 3 seconds after user stops typing
        int currentDelay = SharedPreferenceHelper.getSharedPreferenceInt(
                mContext, AppConstants.SHARED_PREFS_SMS_SEND_DELAY_SECONDS_KEY, AppConstants.DEFAULT_SMS_SEND_DELAY_SECONDS);
        smsSendDelayEditText.setText(String.valueOf(currentDelay));
        smsDelaySaveRunnable = this::saveSendDelay;
        smsSendDelayEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                smsDelaySaveHandler.removeCallbacks(smsDelaySaveRunnable);
                smsDelaySaveHandler.postDelayed(smsDelaySaveRunnable, SMS_DELAY_SAVE_DEBOUNCE_MS);
            }
        });
        smsSendDelayEditText.setOnEditorActionListener((v, actionId, event) -> {
            smsDelaySaveHandler.removeCallbacks(smsDelaySaveRunnable);
            saveSendDelay();
            return false;
        });
    }

    private boolean persistApiEndpoint(View view) {
        ApiEndpointConfig.ValidationResult endpointResult = ApiEndpointConfig.saveApiBaseUrl(
                mContext,
                apiEndpointEditText.getText().toString()
        );

        if (!endpointResult.isValid()) {
            apiEndpointEditText.setError(endpointResult.getMessage());
            Snackbar.make(view, endpointResult.getMessage(), Snackbar.LENGTH_LONG).show();
            return false;
        }

        apiEndpointEditText.setError(null);
        apiEndpointEditText.setText(endpointResult.getUrl());
        apiEndpointEditText.setSelection(apiEndpointEditText.getText().length());
        return true;
    }

    private RegisterDeviceInputDTO buildDeviceInput(boolean enabled) {
        RegisterDeviceInputDTO input = new RegisterDeviceInputDTO();
        input.setEnabled(enabled);
        input.setBrand(Build.BRAND);
        input.setManufacturer(Build.MANUFACTURER);
        input.setModel(Build.MODEL);
        input.setBuildId(Build.ID);
        input.setOs(Build.VERSION.BASE_OS);
        input.setAppVersionCode(BuildConfig.VERSION_CODE);
        input.setAppVersionName(BuildConfig.VERSION_NAME);

        String deviceName = deviceNameEditText.getText().toString().trim();
        if (deviceName.isEmpty()) {
            deviceName = Build.BRAND + " " + Build.MODEL;
        }
        input.setName(deviceName);

        SimInfoCollectionDTO simInfoCollection = new SimInfoCollectionDTO();
        simInfoCollection.setLastUpdated(System.currentTimeMillis());
        simInfoCollection.setSims(TextBeeUtils.collectSimInfo(mContext));
        input.setSimInfo(simInfoCollection);

        return input;
    }

    private void saveSendDelay() {
        String text = smsSendDelayEditText.getText().toString().trim();
        if (text.isEmpty()) {
            int defaultDelay = AppConstants.DEFAULT_SMS_SEND_DELAY_SECONDS;
            smsSendDelayEditText.setText(String.valueOf(defaultDelay));
            SharedPreferenceHelper.setSharedPreferenceInt(mContext, AppConstants.SHARED_PREFS_SMS_SEND_DELAY_SECONDS_KEY, defaultDelay);
            Snackbar.make(smsSendDelayEditText, "SMS send delay saved (" + defaultDelay + " sec)", Snackbar.LENGTH_SHORT).show();
            return;
        }
        try {
            int value = Integer.parseInt(text);
            if (value < 0) {
                value = 0;
                smsSendDelayEditText.setText("0");
                SharedPreferenceHelper.setSharedPreferenceInt(mContext, AppConstants.SHARED_PREFS_SMS_SEND_DELAY_SECONDS_KEY, 0);
                Snackbar.make(smsSendDelayEditText, "Minimum delay is 0 seconds. Saved.", Snackbar.LENGTH_SHORT).show();
            } else if (value > 3600) {
                value = 3600;
                smsSendDelayEditText.setText("3600");
                SharedPreferenceHelper.setSharedPreferenceInt(mContext, AppConstants.SHARED_PREFS_SMS_SEND_DELAY_SECONDS_KEY, 3600);
                Snackbar.make(smsSendDelayEditText, "Maximum delay is 3600 seconds. Saved.", Snackbar.LENGTH_SHORT).show();
            } else {
                SharedPreferenceHelper.setSharedPreferenceInt(mContext, AppConstants.SHARED_PREFS_SMS_SEND_DELAY_SECONDS_KEY, value);
                Snackbar.make(smsSendDelayEditText, "SMS send delay saved (" + value + " sec)", Snackbar.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            int defaultDelay = AppConstants.DEFAULT_SMS_SEND_DELAY_SECONDS;
            smsSendDelayEditText.setText(String.valueOf(defaultDelay));
            SharedPreferenceHelper.setSharedPreferenceInt(mContext, AppConstants.SHARED_PREFS_SMS_SEND_DELAY_SECONDS_KEY, defaultDelay);
            Snackbar.make(smsSendDelayEditText, "Invalid value. Reset to " + defaultDelay + " sec.", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void renderAvailableSimOptions() {
        try {
            defaultSimSlotRadioGroup.removeAllViews();
            
            // Set radio group styling for dark mode compatibility
            defaultSimSlotRadioGroup.setBackgroundColor(getResources().getColor(R.color.background_secondary));
            defaultSimSlotRadioGroup.setPadding(16, 8, 16, 8);
            
            // Create the default radio button with proper styling
            RadioButton defaultSimSlotRadioBtn = new RadioButton(mContext);
            defaultSimSlotRadioBtn.setText("Device Default");
            defaultSimSlotRadioBtn.setId((int)123456);
            applyRadioButtonStyle(defaultSimSlotRadioBtn);
            defaultSimSlotRadioGroup.addView(defaultSimSlotRadioBtn);
            
            // Create radio buttons for each SIM with proper styling
            TextBeeUtils.getAvailableSimSlots(mContext).forEach(subscriptionInfo -> {
                String displayName = subscriptionInfo.getDisplayName() != null ? subscriptionInfo.getDisplayName().toString() : "Unknown";
                String simInfo = displayName + " (Subscription ID: " + subscriptionInfo.getSubscriptionId() + ")";
                RadioButton radioButton = new RadioButton(mContext);
                radioButton.setText(simInfo);
                radioButton.setId(subscriptionInfo.getSubscriptionId());
                applyRadioButtonStyle(radioButton);
                defaultSimSlotRadioGroup.addView(radioButton);
            });

            // Check the preferred SIM based on saved preferences
            int preferredSim = SharedPreferenceHelper.getSharedPreferenceInt(mContext, AppConstants.SHARED_PREFS_PREFERRED_SIM_KEY, -1);
            if (preferredSim == -1) {
                defaultSimSlotRadioGroup.check(defaultSimSlotRadioBtn.getId());
            } else {
                defaultSimSlotRadioGroup.check(preferredSim);
            }
            
            // Set the listener for SIM selection changes
            defaultSimSlotRadioGroup.setOnCheckedChangeListener((radioGroup, i) -> {
                RadioButton radioButton = findViewById(i);
                if (radioButton == null) {
                    return;
                }
                radioButton.setChecked(true);
                if("Device Default".equals(radioButton.getText().toString())) {
                    SharedPreferenceHelper.clearSharedPreference(mContext, AppConstants.SHARED_PREFS_PREFERRED_SIM_KEY);
                } else {
                    SharedPreferenceHelper.setSharedPreferenceInt(mContext, AppConstants.SHARED_PREFS_PREFERRED_SIM_KEY, radioButton.getId());
                }
            });
        } catch (Exception e) {
            Snackbar.make(defaultSimSlotRadioGroup.getRootView(), "Error: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
            Log.e(TAG, "SIM_SLOT_ERROR "+ e.getMessage());
        }
    }
    
    /**
     * Extracts error message from API response, trying multiple sources:
     * 1. Error message from response body (error field)
     * 2. Response message from HTTP headers
     * 3. Generic error with status code as fallback
     */
    private String extractErrorMessage(Response<?> response) {
        // Try to parse error from response body
        try {
            ResponseBody errorBody = response.errorBody();
            if (errorBody != null) {
                String errorBodyString = errorBody.string();
                if (errorBodyString != null && !errorBodyString.isEmpty()) {
                    try {
                        Gson gson = new Gson();
                        RegisterDeviceResponseDTO errorResponse = gson.fromJson(errorBodyString, RegisterDeviceResponseDTO.class);
                        if (errorResponse != null && errorResponse.error != null && !errorResponse.error.isEmpty()) {
                            return errorResponse.error;
                        }
                    } catch (Exception e) {
                        // If JSON parsing fails, try to extract message from raw string
                        Log.d(TAG, "Could not parse error response as JSON");
                    }
                }
            }
        } catch (IOException e) {
            Log.d(TAG, "Could not read error body: " + e.getMessage());
        }
        
        // Fall back to response message
        if (response.message() != null && !response.message().isEmpty()) {
            return response.message();
        }
        
        // Final fallback to generic error with status code
        return "An error occurred :( " + response.code();
    }
    
    /**
     * Apply the custom radio button style to a programmatically created radio button
     */
    private void applyRadioButtonStyle(RadioButton radioButton) {
        // Set text color using the color state list for proper dark/light mode handling
        setRadioButtonTextColor(radioButton);
        
        // Set button tint for the radio circle
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                radioButton.setButtonTintList(getResources().getColorStateList(R.color.radio_button_tint, getTheme()));
            } else {
                radioButton.setButtonTintList(getResources().getColorStateList(R.color.radio_button_tint));
            }
        }
        
        // Add proper padding for better touch experience
        radioButton.setPadding(
            radioButton.getPaddingLeft() + 8,
            radioButton.getPaddingTop() + 12,
            radioButton.getPaddingRight(),
            radioButton.getPaddingBottom() + 12
        );
    }
    
    /**
     * Helper method to set radio button text color in a backward-compatible way
     */
    private void setRadioButtonTextColor(RadioButton radioButton) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            radioButton.setTextColor(getResources().getColorStateList(R.color.radio_button_text_color, getTheme()));
        } else {
            radioButton.setTextColor(getResources().getColorStateList(R.color.radio_button_text_color));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != PERMISSION_REQUEST_CODE) {
            return;
        }
        boolean allPermissionsGranted = Arrays.stream(permissions).allMatch(permission -> TextBeeUtils.isPermissionGranted(mContext, permission));
        if (allPermissionsGranted) {
            Snackbar.make(findViewById(R.id.grantSMSPermissionBtn), "All Permissions Granted", Snackbar.LENGTH_SHORT).show();
            grantSMSPermissionBtn.setEnabled(false);
            grantSMSPermissionBtn.setText("Permission Granted");
            renderAvailableSimOptions();
        } else {
            Snackbar.make(findViewById(R.id.grantSMSPermissionBtn), "Please Grant Required Permissions to continue", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void handleRegisterDevice() {
        String newKey = apiKeyEditText.getText().toString();
        String deviceIdInput = deviceIdEditText.getText().toString();
        View view = findViewById(R.id.registerDeviceBtn);

        if (!persistApiEndpoint(view)) {
            return;
        }

        registerDeviceBtn.setEnabled(false);
        registerDeviceBtn.setText("Loading...");

        RegisterDeviceInputDTO registerDeviceInput = buildDeviceInput(true);
        Call<RegisterDeviceResponseDTO> apiCall;

        try {
            if (!deviceIdInput.isEmpty()) {
                Log.d(TAG, "Updating existing device");
                apiCall = ApiManager.getApiService(mContext).updateDevice(deviceIdInput, newKey, registerDeviceInput);
                enqueueDeviceSaveCall(apiCall, view, newKey, true, "Device Updated Successfully :)", "Error registering device");
                return;
            }

            apiCall = ApiManager.getApiService(mContext).registerDevice(newKey, registerDeviceInput);
            enqueueDeviceSaveCall(apiCall, view, newKey, true, "Device Registration Successful :)", "Error registering device");
        } catch (IllegalStateException e) {
            Snackbar.make(view, e.getMessage(), Snackbar.LENGTH_LONG).show();
            registerDeviceBtn.setEnabled(true);
            registerDeviceBtn.setText("Update");
        }
    }

    private void handleUpdateDevice() {
        String apiKey = apiKeyEditText.getText().toString();
        String deviceIdInput = deviceIdEditText.getText().toString();
        String deviceIdToUse = !deviceIdInput.isEmpty() ? deviceIdInput : deviceId;
        View view = findViewById(R.id.registerDeviceBtn);

        if (!persistApiEndpoint(view)) {
            return;
        }

        registerDeviceBtn.setEnabled(false);
        registerDeviceBtn.setText("Loading...");

        RegisterDeviceInputDTO updateDeviceInput = buildDeviceInput(true);

        try {
            Call<RegisterDeviceResponseDTO> apiCall = ApiManager.getApiService(mContext).updateDevice(deviceIdToUse, apiKey, updateDeviceInput);
            enqueueDeviceSaveCall(apiCall, view, apiKey, true, "Device Updated Successfully :)", "Error updating device");
        } catch (IllegalStateException e) {
            Snackbar.make(view, e.getMessage(), Snackbar.LENGTH_LONG).show();
            registerDeviceBtn.setEnabled(true);
            registerDeviceBtn.setText("Update");
        }
    }

    private void enqueueDeviceSaveCall(
            Call<RegisterDeviceResponseDTO> apiCall,
            View view,
            String apiKey,
            boolean deviceEnabled,
            String successMessage,
            String logMessage
    ) {
        apiCall.enqueue(new Callback<RegisterDeviceResponseDTO>() {
            @Override
            public void onResponse(Call<RegisterDeviceResponseDTO> call, Response<RegisterDeviceResponseDTO> response) {
                Log.d(TAG, "Device save response received: " + response.code());
                if (!response.isSuccessful()) {
                    Snackbar.make(view, extractErrorMessage(response), Snackbar.LENGTH_LONG).show();
                    registerDeviceBtn.setEnabled(true);
                    registerDeviceBtn.setText("Update");
                    return;
                }

                SharedPreferenceHelper.setSharedPreferenceString(mContext, AppConstants.SHARED_PREFS_API_KEY_KEY, apiKey);
                Snackbar.make(view, successMessage, Snackbar.LENGTH_LONG).show();

                if (response.body() != null && response.body().data != null && response.body().data.get("_id") != null) {
                    deviceId = response.body().data.get("_id").toString();
                    deviceIdTxt.setText(deviceId);
                    deviceIdEditText.setText(deviceId);
                    SharedPreferenceHelper.setSharedPreferenceString(mContext, AppConstants.SHARED_PREFS_DEVICE_ID_KEY, deviceId);
                    SharedPreferenceHelper.setSharedPreferenceBoolean(mContext, AppConstants.SHARED_PREFS_GATEWAY_ENABLED_KEY, deviceEnabled);

                    suppressGatewaySwitchChange = true;
                    gatewaySwitch.setChecked(deviceEnabled);
                    suppressGatewaySwitchChange = false;

                    if (response.body().data.get("heartbeatIntervalMinutes") != null) {
                        Object intervalObj = response.body().data.get("heartbeatIntervalMinutes");
                        if (intervalObj instanceof Number) {
                            int intervalMinutes = ((Number) intervalObj).intValue();
                            SharedPreferenceHelper.setSharedPreferenceInt(mContext, AppConstants.SHARED_PREFS_HEARTBEAT_INTERVAL_MINUTES_KEY, intervalMinutes);
                            Log.d(TAG, "Synced heartbeat interval from server");
                        }
                    }

                    if (response.body().data.get("name") != null) {
                        String deviceName = response.body().data.get("name").toString();
                        SharedPreferenceHelper.setSharedPreferenceString(mContext, AppConstants.SHARED_PREFS_DEVICE_NAME_KEY, deviceName);
                        deviceNameEditText.setText(deviceName);
                        Log.d(TAG, "Synced device name from server");
                    }

                    if (deviceEnabled) {
                        HeartbeatManager.scheduleHeartbeat(mContext);
                    }
                }

                VersionTracker.updateStoredVersion(mContext);

                registerDeviceBtn.setEnabled(true);
                registerDeviceBtn.setText("Update");
            }

            @Override
            public void onFailure(Call<RegisterDeviceResponseDTO> call, Throwable t) {
                Snackbar.make(view, "An error occurred :(", Snackbar.LENGTH_LONG).show();
                Log.e(TAG, "API error saving device", t);
                TextBeeUtils.logException(t, logMessage);
                registerDeviceBtn.setEnabled(true);
                registerDeviceBtn.setText("Update");
            }
        });
    }

    private void handleRequestPermissions(View view) {
        boolean allPermissionsGranted = Arrays.stream(AppConstants.requiredPermissions).allMatch(permission -> TextBeeUtils.isPermissionGranted(mContext, permission));
        if (allPermissionsGranted) {
            Snackbar.make(view, "Already got permissions", Snackbar.LENGTH_SHORT).show();
            return;
        }
        String[] permissionsToRequest = Arrays.stream(AppConstants.requiredPermissions).filter(permission -> !TextBeeUtils.isPermissionGranted(mContext, permission)).toArray(String[]::new);
        showSmsPermissionDisclosure(permissionsToRequest);
    }

    private void showSmsPermissionDisclosure(String[] permissionsToRequest) {
        new AlertDialog.Builder(this)
                .setTitle("SMS gateway permissions")
                .setMessage("textbee needs SMS and phone-state permissions to send SMS from this device, receive inbound SMS only when Receive SMS is enabled, choose the SIM used for sending, and report delivery status to your configured HTTPS API endpoint. SMS content, sender or recipient, delivery status, device registration details, and SIM selection metadata may be sent to that endpoint. No analytics, ads, crash reporting, or third-party tracking SDK is used.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Continue", (dialog, which) ->
                        ActivityCompat.requestPermissions(this, permissionsToRequest, PERMISSION_REQUEST_CODE)
                )
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SCAN_QR_REQUEST_CODE) {
            IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (intentResult == null || intentResult.getContents() == null) {
                Toast.makeText(getBaseContext(), "Canceled", Toast.LENGTH_SHORT).show();
                return;
            }
            String scannedQR = intentResult.getContents();
            apiKeyEditText.setText(scannedQR);
            if(deviceIdEditText.getText().toString().isEmpty()) {
                handleRegisterDevice();
            } else {
                handleUpdateDevice();
            }
        }
    }

}
