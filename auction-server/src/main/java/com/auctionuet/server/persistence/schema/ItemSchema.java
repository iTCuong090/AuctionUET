package com.auctionuet.server.persistence.schema;

import com.auctionuet.protocol.enums.ItemCondition;
import com.auctionuet.protocol.enums.ItemType;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class ItemSchema extends BaseSchema {
    private String name;
    private String description;
    private double startingPrice;
    private ItemType type;
    private String sellerId;
    private String imageUrl;
    private ItemCondition condition;
    private int auctionCount;
    private Map<String, Object> extraFields;

    protected ItemSchema() {
    }

    public ItemSchema(
            String id,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String name,
            String description,
            double startingPrice,
            ItemType type,
            String sellerId,
            String imageUrl,
            ItemCondition condition,
            int auctionCount,
            Map<String, Object> extraFields) {
        super(id, createdAt, updatedAt);
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.type = type;
        this.sellerId = sellerId;
        this.imageUrl = imageUrl;
        this.condition = condition;
        this.auctionCount = auctionCount;
        this.extraFields = extraFields != null ? new LinkedHashMap<>(extraFields) : new LinkedHashMap<>();
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
    public ItemCondition getCondition() { return condition; }
    public void setCondition(ItemCondition condition) { this.condition = condition; }
    public int getAuctionCount() { return auctionCount; }
    public void setAuctionCount(int auctionCount) { this.auctionCount = auctionCount; }
    public Map<String, Object> getExtraFields() { return extraFields; }
    public void setExtraFields(Map<String, Object> extraFields) {
        this.extraFields = extraFields != null ? new LinkedHashMap<>(extraFields) : new LinkedHashMap<>();
    }
}
