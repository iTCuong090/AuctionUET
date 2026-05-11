package com.auctionuet.protocol.dto.response;

public class AutoBidConfigDTO {
    private double maxBid;
    private double increment;

    public AutoBidConfigDTO(double maxBid, double increment) {
        this.maxBid = maxBid;
        this.increment = increment;
    }

    public double getMaxBid() { return maxBid; }
    public double getIncrement() { return increment; }
}

