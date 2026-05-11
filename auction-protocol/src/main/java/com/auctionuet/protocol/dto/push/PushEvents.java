package com.auctionuet.protocol.dto.push;

import java.time.LocalDateTime;

public class PushEvents {
    public static class BidUpdatePush {
        private String auctionId;
        private String bidderUsername;
        private double amount;
        private String timestamp;

        public BidUpdatePush(String auctionId, String bidderUsername, double amount, LocalDateTime timestamp) {
            this.auctionId = auctionId;
            this.bidderUsername = bidderUsername;
            this.amount = amount;
            this.timestamp = timestamp != null ? timestamp.toString() : null;
        }

        // Getters
        public String getAuctionId() { return auctionId; }
        public String getBidderUsername() { return bidderUsername; }
        public double getAmount() { return amount; }
        public String getTimestamp() { return timestamp; }
    }

    public static class AuctionEndedPush {
        private String auctionId;
        private String winner;
        private double finalPrice;
        private String message;

        public AuctionEndedPush(String auctionId, String winner, double finalPrice, String message) {
            this.auctionId = auctionId;
            this.winner = winner;
            this.finalPrice = finalPrice;
            this.message = message;
        }

        // Getters
        public String getAuctionId() { return auctionId; }
        public String getWinner() { return winner; }
        public double getFinalPrice() { return finalPrice; }
        public String getMessage() { return message; }
    }

    public static class AuctionExtendedPush {
        private String auctionId;
        private String newEndTime;

        public AuctionExtendedPush(String auctionId, LocalDateTime newEndTime) {
            this.auctionId = auctionId;
            this.newEndTime = newEndTime != null ? newEndTime.toString() : null;
        }

        public String getAuctionId() { return auctionId; }
        public String getNewEndTime() { return newEndTime; }
    }
}
