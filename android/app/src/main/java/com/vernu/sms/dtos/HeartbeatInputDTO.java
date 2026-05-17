package com.vernu.sms.dtos;

public class HeartbeatInputDTO {
    private Integer batteryPercentage;
    private Boolean isCharging;
    private String networkType;
    private String appVersionName;
    private Integer appVersionCode;
    private Long deviceUptimeMillis;
    private Boolean receiveSMSEnabled;
    private Integer smsSendDelaySeconds;
    private SimInfoCollectionDTO simInfo;

    public HeartbeatInputDTO() {
    }

    public Integer getBatteryPercentage() {
        return batteryPercentage;
    }

    public void setBatteryPercentage(Integer batteryPercentage) {
        this.batteryPercentage = batteryPercentage;
    }

    public Boolean getIsCharging() {
        return isCharging;
    }

    public void setIsCharging(Boolean isCharging) {
        this.isCharging = isCharging;
    }

    public String getNetworkType() {
        return networkType;
    }

    public void setNetworkType(String networkType) {
        this.networkType = networkType;
    }

    public String getAppVersionName() {
        return appVersionName;
    }

    public void setAppVersionName(String appVersionName) {
        this.appVersionName = appVersionName;
    }

    public Integer getAppVersionCode() {
        return appVersionCode;
    }

    public void setAppVersionCode(Integer appVersionCode) {
        this.appVersionCode = appVersionCode;
    }

    public Long getDeviceUptimeMillis() {
        return deviceUptimeMillis;
    }

    public void setDeviceUptimeMillis(Long deviceUptimeMillis) {
        this.deviceUptimeMillis = deviceUptimeMillis;
    }

    public Boolean getReceiveSMSEnabled() {
        return receiveSMSEnabled;
    }

    public void setReceiveSMSEnabled(Boolean receiveSMSEnabled) {
        this.receiveSMSEnabled = receiveSMSEnabled;
    }

    public Integer getSmsSendDelaySeconds() {
        return smsSendDelaySeconds;
    }

    public void setSmsSendDelaySeconds(Integer smsSendDelaySeconds) {
        this.smsSendDelaySeconds = smsSendDelaySeconds;
    }

    public SimInfoCollectionDTO getSimInfo() {
        return simInfo;
    }

    public void setSimInfo(SimInfoCollectionDTO simInfo) {
        this.simInfo = simInfo;
    }
}
