package com.auctionuet.protocol.dto.response.item;

import com.auctionuet.protocol.dto.ValidatableDTO;
import com.auctionuet.protocol.enums.ItemType;

import java.util.Collections;
import java.util.Map;

public class ItemDTO implements ValidatableDTO {
    private final String id;
    private final String name;
    private final String description;
    private final double startingPrice;
    private final ItemType type;
    private final String sellerUsername;
    private final String imageUrl;
    private final String condition;
    private final Map<String, Object> extraFields;

    public ItemDTO(
            String id,
            String name,
            String description,
            double startingPrice,
            ItemType type,
            String sellerUsername,
            String imageUrl,
            String condition,
            Map<String, Object> extraFields) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.type = type;
        this.sellerUsername = sellerUsername;
        this.imageUrl = imageUrl;
        this.condition = condition;
        this.extraFields = extraFields != null ? Collections.unmodifiableMap(extraFields) : null;
        validate();
    }

    @Override
    public void validate() {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (startingPrice < 0) {
            throw new IllegalArgumentException("startingPrice must be greater than or equal to 0");
        }
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (extraFields == null) {
            throw new IllegalArgumentException("extraFields must not be null");
        }
        type.normalizeAndValidateExtraFields(extraFields);
        if (sellerUsername == null || sellerUsername.isBlank()) {
            throw new IllegalArgumentException("sellerUsername must not be blank");
        }
        if (condition != null && condition.isBlank()) {
            throw new IllegalArgumentException("condition must not be blank when provided");
        }
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getStartingPrice() { return startingPrice; }
    public ItemType getType() { return type; }
    public String getSellerUsername() { return sellerUsername; }
    public String getImageUrl() { return imageUrl; }
    public String getCondition() { return condition; }
    public Map<String, Object> getExtraFields() { return extraFields; }

    @Override
    public String toString() {
        return name + " (" + startingPrice + ")";
    }
}
