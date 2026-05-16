package com.auctionuet.protocol.dto.response.bid;

import com.auctionuet.protocol.dto.ValidatableDTO;
import com.auctionuet.protocol.dto.response.user.UserDTO;

public class AutoBidConfigDTO implements ValidatableDTO {
    private final String auctionId;
    private final UserDTO bidder;
    private final double maxBid;
    private final double increment;

    public AutoBidConfigDTO(String auctionId, UserDTO bidder, double maxBid, double increment) {
        this.auctionId = auctionId;
        this.bidder = bidder;
        this.maxBid = maxBid;
        this.increment = increment;
        validate();
    }

    @Override
    public void validate() {
        if (auctionId == null || auctionId.isBlank()) {
            throw new IllegalArgumentException("auctionId must not be blank");
        }
        if (bidder == null) {
            throw new IllegalArgumentException("bidder must not be null");
        }
        if (maxBid <= 0) {
            throw new IllegalArgumentException("maxBid must be greater than 0");
        }
        if (increment <= 0) {
            throw new IllegalArgumentException("increment must be greater than 0");
        }
    }

    public String getAuctionId() { return auctionId; }
    public UserDTO getBidder() { return bidder; }
    public double getMaxBid() { return maxBid; }
    public double getIncrement() { return increment; }
}
