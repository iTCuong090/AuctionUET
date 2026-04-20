package com.auctionuet.server.persistence.schema;

import com.auctionuet.server.domain.enums.ItemType;
import java.time.LocalDateTime;

public class VehicleSchema extends ItemSchema {
    private String make;
    private String model;
    private int mileage;
    private int vehicleYear;

    protected VehicleSchema() {
    }

    public VehicleSchema(String id, LocalDateTime createdAt, LocalDateTime updatedAt,
                         String name, String description, double startingPrice,
                         ItemType type, String sellerId, String imageUrl,
                         String condition, int auctionCount,
                         String make, String model, int mileage, int vehicleYear) {
        super(id, createdAt, updatedAt, name, description, startingPrice, type, sellerId, imageUrl, condition, auctionCount);
        this.make = make;
        this.model = model;
        this.mileage = mileage;
        this.vehicleYear = vehicleYear;
    }

    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }
    
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    
    public int getMileage() { return mileage; }
    public void setMileage(int mileage) { this.mileage = mileage; }
    
    public int getVehicleYear() { return vehicleYear; }
    public void setVehicleYear(int vehicleYear) { this.vehicleYear = vehicleYear; }
}
