package com.auctionuet.server.domain.model;

import java.time.LocalDateTime;

public class AutoBidConfig {
    private String bidderId;
    private double maxBid;
    private double increment;

    public AutoBidConfig(String bidderId, double maxBid, double increment) {
        this.bidderId = bidderId;
        this.maxBid = maxBid;
        this.increment = increment;
    }

    public String getBidderId() {
        return bidderId;
    }

    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
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

    public void setIncrement(double increment) {
        this.increment = increment;
    }
}
