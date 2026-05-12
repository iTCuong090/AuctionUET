package com.auctionuet.protocol.dto.response.auction;

import com.auctionuet.protocol.dto.ValidatableDTO;
import com.auctionuet.protocol.dto.response.item.ItemDTO;
import com.auctionuet.protocol.enums.AuctionStatus;

import java.time.LocalDateTime;

public class AuctionDTO implements ValidatableDTO {
    private final String id;
    private final ItemDTO item;
    private final String sellerUsername;
    private final String title;
    private final String description;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final AuctionStatus status;
    private final double currentHighestBid;
    private final String currentWinnerUsername;

    public AuctionDTO(
            String id,
            ItemDTO item,
            String sellerUsername,
            String title,
            String description,
            LocalDateTime startTime,
            LocalDateTime endTime,
            AuctionStatus status,
            double currentHighestBid,
            String currentWinnerUsername) {
        this.id = id;
        this.item = item;
        this.sellerUsername = sellerUsername;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.currentHighestBid = currentHighestBid;
        this.currentWinnerUsername = currentWinnerUsername;
        validate();
    }

    @Override
    public void validate() {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (item == null) {
            throw new IllegalArgumentException("item must not be null");
        }
        if (sellerUsername == null || sellerUsername.isBlank()) {
            throw new IllegalArgumentException("sellerUsername must not be blank");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("startTime and endTime must not be null");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (currentHighestBid < 0) {
            throw new IllegalArgumentException("currentHighestBid must be greater than or equal to 0");
        }
    }

    public String getId() { return id; }
    public ItemDTO getItem() { return item; }
    public String getSellerUsername() { return sellerUsername; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public AuctionStatus getStatus() { return status; }
    public double getCurrentHighestBid() { return currentHighestBid; }
    public String getCurrentWinnerUsername() { return currentWinnerUsername; }
}
