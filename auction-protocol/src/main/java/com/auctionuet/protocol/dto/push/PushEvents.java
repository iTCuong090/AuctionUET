package com.auctionuet.protocol.dto.push;

import com.auctionuet.protocol.dto.response.bid.BidDTO;
import com.auctionuet.protocol.dto.response.user.UserDTO;

import java.time.LocalDateTime;

public class PushEvents {
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
}
