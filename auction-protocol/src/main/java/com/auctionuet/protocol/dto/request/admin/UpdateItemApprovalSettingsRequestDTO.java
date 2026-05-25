package com.auctionuet.protocol.dto.request.admin;

import com.auctionuet.protocol.dto.ValidatableDTO;

public class UpdateItemApprovalSettingsRequestDTO implements ValidatableDTO {
    private Boolean enabled;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void validate() {
        if (enabled == null) {
            throw new IllegalArgumentException("enabled must not be null");
        }
    }
}
