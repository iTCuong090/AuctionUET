package com.auctionuet.server.persistence.schema;

import com.auctionuet.protocol.enums.BidType;

import java.time.LocalDateTime;

public class BidSchema extends BaseSchema {
    private String auctionId;
    private String bidderId;
    private double amount;
    private LocalDateTime timestamp;
    private BidType bidType = BidType.MANUAL;

    protected BidSchema() {
    }

    public BidSchema(String id, LocalDateTime createdAt, LocalDateTime updatedAt,
                     String auctionId, String bidderId, double amount, LocalDateTime timestamp, BidType bidType) {
        super(id, createdAt, updatedAt);
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.timestamp = timestamp;
        this.bidType = bidType != null ? bidType : BidType.MANUAL;
    }

    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }
    
    public String getBidderId() { return bidderId; }
    public void setBidderId(String bidderId) { this.bidderId = bidderId; }
    
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public BidType getBidType() { return bidType != null ? bidType : BidType.MANUAL; }
    public void setBidType(BidType bidType) { this.bidType = bidType != null ? bidType : BidType.MANUAL; }
}
