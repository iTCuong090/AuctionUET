package com.auctionuet.server.domain.model;

import java.time.LocalDateTime;

public class AutoBidConfig implements Comparable<AutoBidConfig> {
    private final String bidderId;
    private final String bidderName;
    private double maxBid;
    private final double increment;
    private final LocalDateTime registeredAt;
    private boolean active = true;

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
        return comparePriority(other);
    }

    public int comparePriority(AutoBidConfig other) {
        // maxBid cao hơn được ưu tiên trước.
        int maxBidComparison = Double.compare(other.maxBid, this.maxBid);
        if (maxBidComparison != 0) {
            return maxBidComparison;
        }
        // Nếu maxBid bằng nhau thì người bật auto-bid sớm hơn được ưu tiên.
        return this.registeredAt.compareTo(other.registeredAt);
    }

    public void markInactive() {
        this.active = false;
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
    public boolean isActive() {
        return active;
    }
}
