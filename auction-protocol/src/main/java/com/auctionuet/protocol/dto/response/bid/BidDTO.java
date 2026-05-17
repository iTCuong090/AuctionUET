package com.auctionuet.protocol.dto.response.bid;

import com.auctionuet.protocol.dto.ValidatableDTO;
import com.auctionuet.protocol.dto.response.user.UserDTO;
import com.auctionuet.protocol.enums.BidType;

import java.time.LocalDateTime;

public class BidDTO implements ValidatableDTO {
    private final String auctionId;
    private final UserDTO bidder;
    private final double amount;
    private final LocalDateTime timestamp;
    private final BidType bidType;

    public BidDTO(String auctionId, UserDTO bidder, double amount, LocalDateTime timestamp, BidType bidType) {
        this.auctionId = auctionId;
        this.bidder = bidder;
        this.amount = amount;
        this.timestamp = timestamp;
        this.bidType = bidType;
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
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp must not be null");
        }
        if (bidType == null) {
            throw new IllegalArgumentException("bidType must not be null");
        }
    }

    public String getAuctionId() { return auctionId; }
    public UserDTO getBidder() { return bidder; }
    public double getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public BidType getBidType() { return bidType; }
}
