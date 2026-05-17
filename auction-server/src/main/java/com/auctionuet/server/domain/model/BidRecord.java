package com.auctionuet.server.domain.model;

import com.auctionuet.protocol.enums.BidType;

import java.time.LocalDateTime;

public class BidRecord {
    //  immutable POJO

    private final String bidderId;
    private final String bidderName;
    private final double amount;
    private final LocalDateTime timestamp;
    private final BidType bidType;

    public BidRecord(String bidderId, String bidderName, double amount, LocalDateTime timestamp) {
        this(bidderId, bidderName, amount, timestamp, BidType.MANUAL);
    }

    public BidRecord(String bidderId, String bidderName, double amount, LocalDateTime timestamp, BidType bidType) {
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.amount = amount;
        this.timestamp = timestamp;
        this.bidType = bidType != null ? bidType : BidType.MANUAL;
    }

    // GETTERS

    public String getBidderId() {
        return bidderId;
    }

    public String getBidderName() {
        return bidderName;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public BidType getBidType() {
        return bidType;
    }
}
