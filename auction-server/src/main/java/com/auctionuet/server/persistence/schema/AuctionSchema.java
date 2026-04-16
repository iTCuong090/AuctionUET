package com.auctionuet.server.persistence.schema;

import java.time.LocalDateTime;
import com.auctionuet.server.domain.enums.AuctionStatus;

public class AuctionSchema extends BaseSchema {
    private String itemId;
    private String sellerId;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private double highestBid;
    private String winnerId;

    protected AuctionSchema() {
    }

    public AuctionSchema(String id, LocalDateTime createdAt, LocalDateTime updatedAt,
                         String itemId, String sellerId, String title, String description,
                         LocalDateTime startTime, LocalDateTime endTime,
                         AuctionStatus status, double highestBid, String winnerId) {
        super(id, createdAt, updatedAt);
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.highestBid = highestBid;
        this.winnerId = winnerId;
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    
    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }
    
    public double getHighestBid() { return highestBid; }
    public void setHighestBid(double highestBid) { this.highestBid = highestBid; }
    
    public String getWinnerId() { return winnerId; }
    public void setWinnerId(String winnerId) { this.winnerId = winnerId; }
}
