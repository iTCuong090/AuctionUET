package com.auctionuet.server.persistence.schema;

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
    private String condition;
    private int auctionCount;
    private Map<String, Object> extraFields;
    // Legacy fields for backward compatibility with old stored JSON.
    private String brand;
    private Integer warrantyMonths;
    private String artist;
    private Integer year;
    private String medium;
    private String make;
    private String model;
    private Integer mileage;
    private Integer vehicleYear;

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
            String condition,
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
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public int getAuctionCount() { return auctionCount; }
    public void setAuctionCount(int auctionCount) { this.auctionCount = auctionCount; }
    public Map<String, Object> getExtraFields() { return extraFields; }
    public void setExtraFields(Map<String, Object> extraFields) {
        this.extraFields = extraFields != null ? new LinkedHashMap<>(extraFields) : new LinkedHashMap<>();
    }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public Integer getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(Integer warrantyMonths) { this.warrantyMonths = warrantyMonths; }
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public String getMedium() { return medium; }
    public void setMedium(String medium) { this.medium = medium; }
    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Integer getMileage() { return mileage; }
    public void setMileage(Integer mileage) { this.mileage = mileage; }
    public Integer getVehicleYear() { return vehicleYear; }
    public void setVehicleYear(Integer vehicleYear) { this.vehicleYear = vehicleYear; }
}
