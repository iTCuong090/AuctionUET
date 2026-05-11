package com.auctionuet.server.persistence.schema;

import java.time.LocalDateTime;
import com.auctionuet.protocol.enums.ItemType;

public abstract class ItemSchema extends BaseSchema {
    private String name;
    private String description;
    private double startingPrice;
    private ItemType type;
    private String sellerId;
    private String imageUrl;
    private String condition;
    private int auctionCount;

    protected ItemSchema() {
    }

    public ItemSchema(String id, LocalDateTime createdAt, LocalDateTime updatedAt,
                      String name, String description, double startingPrice,
                      ItemType type, String sellerId, String imageUrl,
                      String condition, int auctionCount) {
        super(id, createdAt, updatedAt);
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.type = type;
        this.sellerId = sellerId;
        this.imageUrl = imageUrl;
        this.condition = condition;
        this.auctionCount = auctionCount;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }
    
    public ItemType getType() { return type; }
    public void setType(ItemType type) { this.type = type; }
    
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    
    public int getAuctionCount() { return auctionCount; }
    public void setAuctionCount(int auctionCount) { this.auctionCount = auctionCount; }
}
