package com.vernu.sms.dtos;

public class SimInfoDTO {
    private int subscriptionId;
    private String carrierName;
    private String displayName;
    private Integer simSlotIndex;
    private String subscriptionType;

    public SimInfoDTO() {
    }

    public int getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(int subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getCarrierName() {
        return carrierName;
    }

    public void setCarrierName(String carrierName) {
        this.carrierName = carrierName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Integer getSimSlotIndex() {
        return simSlotIndex;
    }

    public void setSimSlotIndex(Integer simSlotIndex) {
        this.simSlotIndex = simSlotIndex;
    }

    public String getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(String subscriptionType) {
        this.subscriptionType = subscriptionType;
    }
}
