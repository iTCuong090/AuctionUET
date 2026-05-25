package com.auctionuet.protocol.dto.response.item;

import com.auctionuet.protocol.dto.ValidatableDTO;
import com.auctionuet.protocol.dto.response.user.UserDTO;
import com.auctionuet.protocol.enums.ItemCondition;
import com.auctionuet.protocol.enums.ItemApprovalStatus;
import com.auctionuet.protocol.enums.ItemType;

import java.util.Collections;
import java.util.Map;
import java.time.LocalDateTime;

public class ItemDTO implements ValidatableDTO {
    private final String id;
    private final String name;
    private final String description;
    private final double startingPrice;
    private final ItemType type;
    private final UserDTO seller;
    private final String imageUrl;
    private final ItemCondition condition;
    private final Map<String, Object> extraFields;
    private final ItemApprovalStatus approvalStatus;
    private final LocalDateTime createdAt;

    public ItemDTO(
            String id,
            String name,
            String description,
            double startingPrice,
            ItemType type,
            UserDTO seller,
            String imageUrl,
            ItemCondition condition,
            Map<String, Object> extraFields) {
        this(id, name, description, startingPrice, type, seller, imageUrl, condition, extraFields,
                ItemApprovalStatus.APPROVED, null);
    }

    public ItemDTO(
            String id,
            String name,
            String description,
            double startingPrice,
            ItemType type,
            UserDTO seller,
            String imageUrl,
            ItemCondition condition,
            Map<String, Object> extraFields,
            ItemApprovalStatus approvalStatus) {
        this(id, name, description, startingPrice, type, seller, imageUrl, condition, extraFields,
                approvalStatus, null);
    }

    public ItemDTO(
            String id,
            String name,
            String description,
            double startingPrice,
            ItemType type,
            UserDTO seller,
            String imageUrl,
            ItemCondition condition,
            Map<String, Object> extraFields,
            ItemApprovalStatus approvalStatus,
            LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.type = type;
        this.seller = seller;
        this.imageUrl = imageUrl;
        this.condition = condition;
        this.extraFields = extraFields != null ? Collections.unmodifiableMap(extraFields) : null;
        this.approvalStatus = approvalStatus != null ? approvalStatus : ItemApprovalStatus.APPROVED;
        this.createdAt = createdAt;
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
        if (seller == null) {
            throw new IllegalArgumentException("seller must not be null");
        }
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getStartingPrice() { return startingPrice; }
    public ItemType getType() { return type; }
    public UserDTO getSeller() { return seller; }
    public String getImageUrl() { return imageUrl; }
    public ItemCondition getCondition() { return condition; }
    public Map<String, Object> getExtraFields() { return extraFields; }
    public ItemApprovalStatus getApprovalStatus() {
        return approvalStatus != null ? approvalStatus : ItemApprovalStatus.APPROVED;
    }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return name + " (" + startingPrice + ")";
    }
}
