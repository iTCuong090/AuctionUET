package com.auctionuet.protocol.dto.request.admin;

import com.auctionuet.protocol.dto.ValidatableDTO;
import com.auctionuet.protocol.enums.ItemApprovalStatus;

public class UpdateItemApprovalStatusRequestDTO implements ValidatableDTO {
    private String itemId;
    private ItemApprovalStatus status;

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public ItemApprovalStatus getStatus() {
        return status;
    }

    public void setStatus(ItemApprovalStatus status) {
        this.status = status;
    }

    @Override
    public void validate() {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId must not be blank");
        }
        if (status != ItemApprovalStatus.APPROVED && status != ItemApprovalStatus.REJECTED) {
            throw new IllegalArgumentException("status must be APPROVED or REJECTED");
        }
    }
}
