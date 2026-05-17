package com.vernu.sms.dtos;

public class RegisterDeviceInputDTO {
    private Boolean enabled;
    private String brand;
    private String manufacturer;
    private String model;
    private String name;
    private String buildId;
    private String os;
    private String appVersionName;
    private int appVersionCode;
    private SimInfoCollectionDTO simInfo;

    public RegisterDeviceInputDTO() {
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBuildId() {
        return buildId;
    }

    public void setBuildId(String buildId) {
        this.buildId = buildId;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getAppVersionName() {
        return appVersionName;
    }

    public void setAppVersionName(String appVersionName) {
        this.appVersionName = appVersionName;
    }

    public int getAppVersionCode() {
        return appVersionCode;
    }

    public void setAppVersionCode(int appVersionCode) {
        this.appVersionCode = appVersionCode;
    }

    public SimInfoCollectionDTO getSimInfo() {
        return simInfo;
    }

    public void setSimInfo(SimInfoCollectionDTO simInfo) {
        this.simInfo = simInfo;
    }
}
