package com.auctionuet.protocol.dto.response.bid;

import com.auctionuet.protocol.dto.ValidatableDTO;

public class AutoBidConfigDTO implements ValidatableDTO {
    private final double maxBid;
    private final double increment;

    public AutoBidConfigDTO(double maxBid, double increment) {
        this.maxBid = maxBid;
        this.increment = increment;
        validate();
    }

    @Override
    public void validate() {
        if (maxBid <= 0) {
            throw new IllegalArgumentException("maxBid must be greater than 0");
        }
        if (increment <= 0) {
            throw new IllegalArgumentException("increment must be greater than 0");
        }
    }

    public double getMaxBid() { return maxBid; }
    public double getIncrement() { return increment; }
}
