package com.auctionuet.protocol.dto.response.admin;

import com.auctionuet.protocol.dto.ValidatableDTO;

public class ItemApprovalSettingsDTO implements ValidatableDTO {
    private final boolean enabled;

    public ItemApprovalSettingsDTO(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void validate() {
        // Boolean luôn hợp lệ.
    }
}
