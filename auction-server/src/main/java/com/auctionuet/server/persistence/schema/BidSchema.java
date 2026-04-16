package com.auctionuet.server.persistence.schema;

import java.time.LocalDateTime;

public class BidSchema extends BaseSchema {
    private String auctionId;
    private String bidderId;
    private double amount;
    private LocalDateTime timestamp;

    protected BidSchema() {
    }

    public BidSchema(String id, LocalDateTime createdAt, LocalDateTime updatedAt,
                     String auctionId, String bidderId, double amount, LocalDateTime timestamp) {
        super(id, createdAt, updatedAt);
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }
    
    public String getBidderId() { return bidderId; }
    public void setBidderId(String bidderId) { this.bidderId = bidderId; }
    
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
