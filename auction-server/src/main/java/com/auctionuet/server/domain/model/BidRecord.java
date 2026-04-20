package com.auctionuet.server.domain.model;

import java.time.LocalDateTime;

public class BidRecord {
    //  immutable POJO

    private final String bidderId;
    private final String bidderUsername;
    private final double amount;
    private final LocalDateTime timestamp;

    public BidRecord(String bidderId, String bidderUsername, double amount, LocalDateTime timestamp) {
        this.bidderId = bidderId;
        this.bidderUsername = bidderUsername;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    // GETTERS

    public String getBidderId() {
        return bidderId;
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
