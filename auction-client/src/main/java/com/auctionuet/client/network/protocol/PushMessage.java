package com.auctionuet.client.network.protocol;

public class PushMessage {
    private String type;           // "PUSH"
    private String pushType;       // "BID_UPDATE", "AUCTION_EXTENDED", "AUCTION_ENDED"
    private String auctionId;
    private String bidderUsername;
    private double amount;
    private String winnerId;
    private String newEndTime;
    private double finalPrice;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPushType() { return pushType; }
    public void setPushType(String pushType) { this.pushType = pushType; }

    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }

    public String getBidderUsername() { return bidderUsername; }
    public void setBidderUsername(String bidderUsername) { this.bidderUsername = bidderUsername; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getWinnerId() { return winnerId; }
    public void setWinnerId(String winnerId) { this.winnerId = winnerId; }

    public String getNewEndTime() { return newEndTime; }
    public void setNewEndTime(String newEndTime) { this.newEndTime = newEndTime; }

    public double getFinalPrice() { return finalPrice; }
    public void setFinalPrice(double finalPrice) { this.finalPrice = finalPrice; }
}
