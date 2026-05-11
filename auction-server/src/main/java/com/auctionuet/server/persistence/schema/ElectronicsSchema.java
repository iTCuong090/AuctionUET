package com.auctionuet.server.persistence.schema;

import com.auctionuet.protocol.enums.ItemType;
import java.time.LocalDateTime;

public class ElectronicsSchema extends ItemSchema {
    private String brand;
    private int warrantyMonths;

    protected ElectronicsSchema() {
    }

    public ElectronicsSchema(String id, LocalDateTime createdAt, LocalDateTime updatedAt,
                             String name, String description, double startingPrice,
                             ItemType type, String sellerId, String imageUrl,
                             String condition, int auctionCount,
                             String brand, int warrantyMonths) {
        super(id, createdAt, updatedAt, name, description, startingPrice, type, sellerId, imageUrl, condition, auctionCount);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    
    public int getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; }
}
