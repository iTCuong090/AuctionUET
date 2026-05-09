package com.auctionuet.server.domain.model;

import java.time.LocalDateTime;

public class AutoBidConfig implements Comparable<AutoBidConfig> {
    private final String bidderId;
    private final String bidderUsername;
    private double maxBid;
    private final double increment;
    private final LocalDateTime registeredAt;

    public AutoBidConfig(String bidderId, String bidderUsername,
                         double maxBid, double increment) {
        this.bidderId = bidderId;
        this.bidderUsername = bidderUsername;
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
}
