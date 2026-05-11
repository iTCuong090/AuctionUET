package com.auctionuet.protocol.dto.request;

import java.time.LocalDateTime;

public class AuctionRequests {
    public static class CreateAuctionReq {
        private String itemId;
        private String title;
        private String description;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer antiSnipingWindowSeconds;
        private Integer antiSnipingExtensionSeconds;

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
    }

    public static class AuctionIdReq {
        private String auctionId;

        public String getAuctionId() { return auctionId; }
        public void setAuctionId(String auctionId) { this.auctionId = auctionId; }
    }
}

