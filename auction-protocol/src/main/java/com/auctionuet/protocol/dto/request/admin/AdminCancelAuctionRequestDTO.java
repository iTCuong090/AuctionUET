package com.auctionuet.protocol.dto.request.admin;

import com.auctionuet.protocol.dto.ValidatableDTO;

public class AdminCancelAuctionRequestDTO implements ValidatableDTO {
    private String auctionId;
    private String reason;

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public void validate() {
        if (auctionId == null || auctionId.isBlank()) {
            throw new IllegalArgumentException("auctionId must not be blank");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
        auctionId = auctionId.trim();
        reason = reason.trim();
    }
}
