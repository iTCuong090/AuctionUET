package com.auctionuet.protocol.dto.push;

import com.auctionuet.protocol.dto.response.bid.BidDTO;
import com.auctionuet.protocol.dto.response.user.UserDTO;

import java.time.LocalDateTime;

public class PushEvents {
    public static class AuctionStartedPush {
        private String auctionId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String message;

        public AuctionStartedPush(String auctionId, LocalDateTime startTime, LocalDateTime endTime, String message) {
            this.auctionId = auctionId;
            this.startTime = startTime;
            this.endTime = endTime;
            this.message = message;
        }

        public String getAuctionId() { return auctionId; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public String getMessage() { return message; }
    }

    public static class BidUpdatePush {
        private String auctionId;
        private BidDTO bid;

        public BidUpdatePush(String auctionId, BidDTO bid) {
            this.auctionId = auctionId;
            this.bid = bid;
        }

        public String getAuctionId() { return auctionId; }
        public BidDTO getBid() { return bid; }
    }

    public static class AuctionEndedPush {
        private String auctionId;
        private UserDTO winner;
        private double finalPrice;
        private String message;

        public AuctionEndedPush(String auctionId, UserDTO winner, double finalPrice, String message) {
            this.auctionId = auctionId;
            this.winner = winner;
            this.finalPrice = finalPrice;
            this.message = message;
        }

        public String getAuctionId() { return auctionId; }
        public UserDTO getWinner() { return winner; }
        public double getFinalPrice() { return finalPrice; }
        public String getMessage() { return message; }
    }

    public static class AuctionExtendedPush {
        private String auctionId;
        private LocalDateTime newEndTime;

        public AuctionExtendedPush(String auctionId, LocalDateTime newEndTime) {
            this.auctionId = auctionId;
            this.newEndTime = newEndTime;
        }

        public String getAuctionId() { return auctionId; }
        public LocalDateTime getNewEndTime() { return newEndTime; }
    }

    public static class AuctionCanceledPush {
        private String auctionId;
        private String reason;
        private LocalDateTime canceledAt;
        private String message;

        public AuctionCanceledPush(String auctionId, String reason, LocalDateTime canceledAt, String message) {
            this.auctionId = auctionId;
            this.reason = reason;
            this.canceledAt = canceledAt;
            this.message = message;
        }

        public String getAuctionId() { return auctionId; }
        public String getReason() { return reason; }
        public LocalDateTime getCanceledAt() { return canceledAt; }
        public String getMessage() { return message; }
    }
}
