package com.auctionuet.server.domain.model;

import com.auctionuet.server.domain.enums.ItemType;

public abstract class Item {
    private final String id;            // ID sản phẩm (liên kết với ItemSchema.id)
    private final String name;          // Tên sản phẩm
    private final String description;   // Mô tả
    private final double startingPrice; // Giá khởi điểm
    private final ItemType type;        // Loại sản phẩm
    private final String sellerId;      // ID người bán
    private final String imageUrl;      // URL ảnh
    private final String condition;     // Tình trạng

    // Constructor
    public Item(String id, String name, String description, double startingPrice,
                ItemType type, String sellerId, String imageUrl, String condition) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.type = type;
        this.sellerId = sellerId;
        this.imageUrl = imageUrl;
        this.condition = condition;
    }

    // 2. Các phương thức Getter
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

    public String getSellerId() {
        return sellerId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getCondition() {
        return condition;
    }
}

