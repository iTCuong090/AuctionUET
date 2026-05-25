package com.auctionuet.server.persistence.schema;

import java.time.LocalDateTime;

public class SystemSettingSchema extends BaseSchema {
    private Boolean itemApprovalEnabled;

    protected SystemSettingSchema() {
    }

    public SystemSettingSchema(String id, LocalDateTime createdAt, LocalDateTime updatedAt,
            boolean itemApprovalEnabled) {
        super(id, createdAt, updatedAt);
        this.itemApprovalEnabled = itemApprovalEnabled;
    }

    public boolean isItemApprovalEnabled() {
        return itemApprovalEnabled == null || itemApprovalEnabled;
    }

    public void setItemApprovalEnabled(boolean itemApprovalEnabled) {
        this.itemApprovalEnabled = itemApprovalEnabled;
    }
}
