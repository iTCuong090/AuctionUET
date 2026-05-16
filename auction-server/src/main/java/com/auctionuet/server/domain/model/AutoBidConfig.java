package com.auctionuet.server.domain.model;

import java.time.LocalDateTime;

public class AutoBidConfig implements Comparable<AutoBidConfig> {
    private final String bidderId;
    private final String bidderName;
    private double maxBid;
    private final double increment;
    private final LocalDateTime registeredAt;

    public AutoBidConfig(String bidderId, String bidderName,
                         double maxBid, double increment) {
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.maxBid = maxBid;
        this.increment = increment;
        this.registeredAt = LocalDateTime.now();
    }

    @Override
    public int compareTo(AutoBidConfig other) {
        // maxBid cao hơn → ưu tiên trước (nằm đầu queue)
        int cmp = Double.compare(other.maxBid, this.maxBid);
        if (cmp != 0) return cmp;
        // Nếu bằng → đăng ký sớm hơn ưu tiên
        return this.registeredAt.compareTo(other.registeredAt);
    }

    // Getters
    public String getBidderId() {
        return bidderId;
    }
    public String getBidderName() {
        return bidderName;
    }
    public double getMaxBid() {
        return maxBid;
    }
    public void setMaxBid(double maxBid) {
        this.maxBid = maxBid;
    }
    public double getIncrement() {
        return increment;
    }
    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }
}
