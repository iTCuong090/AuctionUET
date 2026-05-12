package com.auctionuet.protocol.dto.request.auction;

import com.auctionuet.protocol.dto.ValidatableDTO;

public class AuctionIdRequestDTO implements ValidatableDTO {
    private String auctionId;

    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }

    @Override
    public void validate() {
        if (auctionId == null || auctionId.isBlank()) {
            throw new IllegalArgumentException("auctionId must not be blank");
        }
    }
}
