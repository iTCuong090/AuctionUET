package com.auctionuet.client.model;

// Import đúng đường dẫn
import com.auctionuet.client.network.protocol.ItemType;
import java.util.Map;

public class ItemDTO {
    private String id;
    private String name;
    private String description;
    private double startingPrice;
    private ItemType type;
    private String sellerUsername;
    private String imageUrl;
    private String condition;
    private Map<String, Object> extraFields;

    public ItemDTO() {}

    public ItemDTO(String id, String name, String description, double startingPrice,
                   ItemType type, String sellerUsername, String imageUrl,
                   String condition, Map<String, Object> extraFields) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.type = type;
        this.sellerUsername = sellerUsername;
        this.imageUrl = imageUrl;
        this.condition = condition;
        this.extraFields = extraFields;
    }

    // --- Getters & Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

    public ItemType getType() { return type; }
    public void setType(ItemType type) { this.type = type; }

    public String getSellerUsername() { return sellerUsername; }
    public void setSellerUsername(String sellerUsername) { this.sellerUsername = sellerUsername; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public Map<String, Object> getExtraFields() { return extraFields; }
    public void setExtraFields(Map<String, Object> extraFields) { this.extraFields = extraFields; }

    // Giúp hiển thị tên trên ComboBox cực đẹp
    @Override
    public String toString() {
        return name + " (" + startingPrice + ")";
    }
}