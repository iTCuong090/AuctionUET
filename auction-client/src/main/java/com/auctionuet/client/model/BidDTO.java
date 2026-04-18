package com.auctionuet.client.model;

import java.time.LocalDateTime;

public class BidDTO {
    private final String auctionId;
    private final String bidderUsername;
    private final double amount;
    private final LocalDateTime timestamp;

    /**
     * Constructor khởi tạo toàn bộ thông tin cho BidDTO.
     */
    public BidDTO(String auctionId, String bidderUsername, double amount, LocalDateTime timestamp) {
        this.auctionId = auctionId;
        this.bidderUsername = bidderUsername;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    // GETTERS

    public String getAuctionId() {
        return auctionId;
    }

    public String getBidderUsername() {
        return bidderUsername;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}

