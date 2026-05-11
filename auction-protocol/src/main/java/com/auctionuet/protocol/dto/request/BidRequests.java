package com.auctionuet.protocol.dto.request;

public class BidRequests {
    public static class PlaceBidReq {
        private String auctionId;
        private double amount;

        public String getAuctionId() { return auctionId; }
        public void setAuctionId(String auctionId) { this.auctionId = auctionId; }
        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }
    }

    public static class AuctionIdReq {
        private String auctionId;

        public String getAuctionId() { return auctionId; }
        public void setAuctionId(String auctionId) { this.auctionId = auctionId; }
    }

    public static class SetAutoBidReq {
        private String auctionId;
        private double maxBid;
        private double increment;

        public String getAuctionId() { return auctionId; }
        public void setAuctionId(String auctionId) { this.auctionId = auctionId; }
        public double getMaxBid() { return maxBid; }
        public void setMaxBid(double maxBid) { this.maxBid = maxBid; }
        public double getIncrement() { return increment; }
        public void setIncrement(double increment) { this.increment = increment; }
    }
}

