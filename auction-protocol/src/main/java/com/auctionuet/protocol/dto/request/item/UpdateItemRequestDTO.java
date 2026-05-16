package com.auctionuet.protocol.dto.request.item;

import com.auctionuet.protocol.dto.ValidatableDTO;
import com.auctionuet.protocol.enums.ItemCondition;

import java.util.Map;

public class UpdateItemRequestDTO implements ValidatableDTO {
    private String id;
    private String name;
    private String description;
    private double startingPrice;
    private String imageUrl;
    private ItemCondition condition;
    private Map<String, Object> extraFields;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public ItemCondition getCondition() { return condition; }
    public void setCondition(ItemCondition condition) { this.condition = condition; }
    public Map<String, Object> getExtraFields() { return extraFields; }
    public void setExtraFields(Map<String, Object> extraFields) { this.extraFields = extraFields; }

    @Override
    public void validate() {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (startingPrice < 0) {
            throw new IllegalArgumentException("startingPrice must be greater than or equal to 0");
        }
        if (name != null && name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank when provided");
        }
    }
}
