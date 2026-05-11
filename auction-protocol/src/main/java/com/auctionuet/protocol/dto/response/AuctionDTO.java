package com.auctionuet.protocol.dto.response;

import com.auctionuet.protocol.enums.AuctionStatus;

import java.time.LocalDateTime;

public class AuctionDTO {
    private final String id;
    private final ItemDTO item;
    private final String sellerUsername;
    private final String title;
    private final String description;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final AuctionStatus status; // GiÃ¡ÂºÂ£ Ã„â€˜Ã¡Â»â€¹nh lÃƒÂ  mÃ¡Â»â„¢t Enum (OPEN, CLOSED, v.v.)
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
    }

    // --- Getters ---

    public String getId() {
        return id;
    }

    public ItemDTO getItem() {
        return item;
    }

    public String getSellerUsername() {
        return sellerUsername;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public double getCurrentHighestBid() {
        return currentHighestBid;
    }

    public String getCurrentWinnerUsername() {
        return currentWinnerUsername;
    }
}

