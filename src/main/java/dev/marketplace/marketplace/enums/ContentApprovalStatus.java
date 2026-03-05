package dev.marketplace.marketplace.enums;

public enum ContentApprovalStatus {
    PENDING("Pending Review"),
    APPROVED("Approved"),
    DECLINED("Declined");

    private final String displayName;

    ContentApprovalStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

