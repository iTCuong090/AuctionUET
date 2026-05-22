package com.auctionuet.protocol.dto.response.bid;

import com.auctionuet.protocol.dto.ValidatableDTO;
import com.auctionuet.protocol.dto.response.user.UserDTO;
import com.auctionuet.protocol.enums.AutoBidStatus;

public class AutoBidConfigDTO implements ValidatableDTO {
    private final String auctionId;
    private final UserDTO bidder;
    private final double maxBid;
    private final double increment;
    private final AutoBidStatus status;
    private final double protectedUntil;

    public AutoBidConfigDTO(String auctionId, UserDTO bidder, double maxBid, double increment) {
        this(auctionId, bidder, maxBid, increment, AutoBidStatus.WAITING, maxBid);
    }

    public AutoBidConfigDTO(
            String auctionId,
            UserDTO bidder,
            double maxBid,
            double increment,
            AutoBidStatus status,
            double protectedUntil) {
        this.auctionId = auctionId;
        this.bidder = bidder;
        this.maxBid = maxBid;
        this.increment = increment;
        this.status = status;
        this.protectedUntil = protectedUntil;
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
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (protectedUntil < 0) {
            throw new IllegalArgumentException("protectedUntil must not be negative");
        }
    }

    public String getAuctionId() { return auctionId; }
    public UserDTO getBidder() { return bidder; }
    public double getMaxBid() { return maxBid; }
    public double getIncrement() { return increment; }
    public AutoBidStatus getStatus() { return status; }
    public double getProtectedUntil() { return protectedUntil; }
}
