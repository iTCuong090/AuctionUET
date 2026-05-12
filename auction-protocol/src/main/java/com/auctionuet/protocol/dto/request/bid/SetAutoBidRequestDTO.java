package com.auctionuet.protocol.dto.request.bid;

import com.auctionuet.protocol.dto.ValidatableDTO;

public class SetAutoBidRequestDTO implements ValidatableDTO {
    private String auctionId;
    private double maxBid;
    private double increment;

    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }
    public double getMaxBid() { return maxBid; }
    public void setMaxBid(double maxBid) { this.maxBid = maxBid; }
    public double getIncrement() { return increment; }
    public void setIncrement(double increment) { this.increment = increment; }

    @Override
    public void validate() {
        if (auctionId == null || auctionId.isBlank()) {
            throw new IllegalArgumentException("auctionId must not be blank");
        }
        if (maxBid <= 0) {
            throw new IllegalArgumentException("maxBid must be greater than 0");
        }
        if (increment <= 0) {
            throw new IllegalArgumentException("increment must be greater than 0");
        }
    }
}
