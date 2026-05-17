package com.auctionuet.protocol.dto.request.auction;

import com.auctionuet.protocol.dto.ValidatableDTO;

import java.time.LocalDateTime;

public class CreateAuctionRequestDTO implements ValidatableDTO {
    private String itemId;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer antiSnipingWindowSeconds = 60;
    private Integer antiSnipingExtensionSeconds = 120;

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public Integer getAntiSnipingWindowSeconds() { return antiSnipingWindowSeconds; }
    public void setAntiSnipingWindowSeconds(Integer antiSnipingWindowSeconds) { this.antiSnipingWindowSeconds = antiSnipingWindowSeconds; }
    public Integer getAntiSnipingExtensionSeconds() { return antiSnipingExtensionSeconds; }
    public void setAntiSnipingExtensionSeconds(Integer antiSnipingExtensionSeconds) { this.antiSnipingExtensionSeconds = antiSnipingExtensionSeconds; }

    @Override
    public void validate() {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId must not be blank");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (startTime == null) {
            throw new IllegalArgumentException("startTime must not be null");
        }
        if (endTime == null) {
            throw new IllegalArgumentException("endTime must not be null");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
        if (antiSnipingWindowSeconds == null) {
            antiSnipingWindowSeconds = 60;
        } else if (antiSnipingWindowSeconds <= 0) {
            throw new IllegalArgumentException("antiSnipingWindowSeconds must be greater than 0 when provided");
        }
        if (antiSnipingExtensionSeconds == null) {
            antiSnipingExtensionSeconds = 120;
        } else if (antiSnipingExtensionSeconds <= 0) {
            throw new IllegalArgumentException("antiSnipingExtensionSeconds must be greater than 0 when provided");
        }
    }
}
