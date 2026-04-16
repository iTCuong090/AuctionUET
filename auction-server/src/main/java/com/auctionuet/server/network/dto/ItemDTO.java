package com.auctionuet.server.network.dto;

import com.auctionuet.server.domain.enums.ItemType;
import java.util.Collections;
import java.util.Map;

public class ItemDTO {
    private final String id;
    private final String name;
    private final String description;
    private final double startingPrice;
    private final ItemType type; // Giả định bạn đã có enum hoặc class ItemType
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
    }

    // --- Getters ---

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public ItemType getType() {
        return type;
    }

    public String getSellerUsername() {
        return sellerUsername;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getCondition() {
        return condition;
    }

    public Map<String, Object> getExtraFields() {
        return extraFields;
    }
}

