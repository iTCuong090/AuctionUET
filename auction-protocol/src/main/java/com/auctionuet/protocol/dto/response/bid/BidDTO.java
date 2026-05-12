package com.auctionuet.protocol.dto.response.bid;

import com.auctionuet.protocol.dto.ValidatableDTO;

import java.time.LocalDateTime;

public class BidDTO implements ValidatableDTO {
    private final String auctionId;
    private final String bidderUsername;
    private final double amount;
    private final LocalDateTime timestamp;

    public BidDTO(String auctionId, String bidderUsername, double amount, LocalDateTime timestamp) {
        this.auctionId = auctionId;
        this.bidderUsername = bidderUsername;
        this.amount = amount;
        this.timestamp = timestamp;
        validate();
    }

    @Override
    public void validate() {
        if (auctionId == null || auctionId.isBlank()) {
            throw new IllegalArgumentException("auctionId must not be blank");
        }
        if (bidderUsername == null || bidderUsername.isBlank()) {
            throw new IllegalArgumentException("bidderUsername must not be blank");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp must not be null");
        }
    }

    public String getAuctionId() { return auctionId; }
    public String getBidderUsername() { return bidderUsername; }
    public double getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
