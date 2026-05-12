package com.auctionuet.protocol.dto.request.item;

import com.auctionuet.protocol.dto.ValidatableDTO;
import com.auctionuet.protocol.enums.ItemType;

import java.util.LinkedHashMap;
import java.util.Map;

public class CreateItemRequestDTO implements ValidatableDTO {
    private String name;
    private String description;
    private double startingPrice;
    private ItemType type;
    private String imageUrl;
    private String condition;
    private Map<String, Object> extraFields;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }
    public ItemType getType() { return type; }
    public void setType(ItemType type) { this.type = type; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public Map<String, Object> getExtraFields() { return extraFields; }
    public void setExtraFields(Map<String, Object> extraFields) { this.extraFields = extraFields; }

    @Override
    public void validate() {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (startingPrice <= 0) {
            throw new IllegalArgumentException("startingPrice must be greater than 0");
        }
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (extraFields == null) {
            throw new IllegalArgumentException("extraFields must not be null");
        }
        this.extraFields = new LinkedHashMap<>(type.normalizeAndValidateExtraFields(extraFields));
        if (condition != null && condition.isBlank()) {
            throw new IllegalArgumentException("condition must not be blank when provided");
        }
    }
}
