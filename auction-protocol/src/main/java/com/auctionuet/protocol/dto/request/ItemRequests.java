package com.auctionuet.protocol.dto.request;

import com.auctionuet.protocol.enums.ItemType;
import java.util.Map;

public class ItemRequests {

    public static class CreateItemReq {
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

        public void validate() {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Tên vật phẩm không được để trống");
            }
            if (startingPrice <= 0) {
                throw new IllegalArgumentException("Giá khởi điểm phải lớn hơn 0");
            }
            if (type == null) {
                throw new IllegalArgumentException("Loại vật phẩm không được để trống");
            }
        }
    }

    public static class UpdateItemReq {
        private String id;
        private String name;
        private String description;
        private double startingPrice;
        private String imageUrl;
        private String condition;
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
        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }
        public Map<String, Object> getExtraFields() { return extraFields; }
        public void setExtraFields(Map<String, Object> extraFields) { this.extraFields = extraFields; }

        public void validate() {
            if (id == null || id.isEmpty()) {
                throw new IllegalArgumentException("ID vật phẩm không được để trống để cập nhật");
            }
            if (startingPrice < 0) {
                throw new IllegalArgumentException("Giá khởi điểm không được âm");
            }
        }
    }
}
