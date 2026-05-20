package com.auctionuet.server.persistence.schema;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.auctionuet.protocol.enums.AuctionStatus;

public class AuctionSchema extends BaseSchema {
    private String itemId;
    private String sellerId;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private double highestBid;
    private String winnerId;
    private List<String> depositedBidderIds = new ArrayList<>();
    private List<AuctionDepositRefSchema> depositRefs = new ArrayList<>();
    private LocalDateTime paymentDeadlineAt;
    private int antiSnipingWindowSeconds = 60;     // Mặc định 60 giây
    private int antiSnipingExtensionSeconds = 120; // Mặc định gia hạn 2 phút

    public AuctionSchema() {
    }

    public AuctionSchema(String id, LocalDateTime createdAt, LocalDateTime updatedAt,
                         String itemId, String sellerId, String title, String description,
                         LocalDateTime startTime, LocalDateTime endTime,
                         AuctionStatus status, double highestBid, String winnerId,
                         int antiSnipingWindowSeconds, int antiSnipingExtensionSeconds) {
        super(id, createdAt, updatedAt);
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.highestBid = highestBid;
        this.winnerId = winnerId;
        this.antiSnipingWindowSeconds = antiSnipingWindowSeconds;
        this.antiSnipingExtensionSeconds = antiSnipingExtensionSeconds;
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    
    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }
    
    public double getHighestBid() { return highestBid; }
    public void setHighestBid(double highestBid) { this.highestBid = highestBid; }
    
    public String getWinnerId() { return winnerId; }
    public void setWinnerId(String winnerId) { this.winnerId = winnerId; }

    public List<String> getDepositedBidderIds() {
        ensureDepositRefs();
        List<String> result = new ArrayList<>();
        for (AuctionDepositRefSchema ref : depositRefs) {
            if (ref.getBidderId() != null && !result.contains(ref.getBidderId())) {
                result.add(ref.getBidderId());
            }
        }
        ensureDepositedBidderIds();
        for (String bidderId : depositedBidderIds) {
            if (bidderId != null && !result.contains(bidderId)) {
                result.add(bidderId);
            }
        }
        return result;
    }

    public void setDepositedBidderIds(List<String> depositedBidderIds) {
        this.depositedBidderIds = depositedBidderIds != null
                ? new ArrayList<>(depositedBidderIds)
                : new ArrayList<>();
    }

    public boolean hasDepositedBidder(String bidderId) {
        return getDepositRef(bidderId) != null || (bidderId != null && getLegacyDepositedBidderIds().contains(bidderId));
    }

    public boolean addDepositedBidder(String bidderId) {
        if (bidderId == null || bidderId.isBlank() || hasDepositedBidder(bidderId)) {
            return false;
        }
        ensureDepositRefs();
        return depositRefs.add(new AuctionDepositRefSchema(
                bidderId,
                bidderId,
                0,
                null,
                LocalDateTime.now()));
    }

    public boolean addDepositRef(AuctionDepositRefSchema ref) {
        if (ref == null || ref.getBidderId() == null || ref.getBidderId().isBlank()) {
            return false;
        }
        if (getDepositRef(ref.getBidderId()) != null) {
            return false;
        }
        ensureDepositRefs();
        ensureDepositedBidderIds();
        depositedBidderIds.remove(ref.getBidderId());
        return depositRefs.add(ref);
    }

    public boolean removeDepositedBidder(String bidderId) {
        boolean removed = false;
        ensureDepositRefs();
        removed |= depositRefs.removeIf(ref -> bidderId != null && bidderId.equals(ref.getBidderId()));
        ensureDepositedBidderIds();
        removed |= depositedBidderIds.remove(bidderId);
        return removed;
    }

    public void clearDepositedBidders() {
        ensureDepositRefs();
        depositRefs.clear();
        ensureDepositedBidderIds();
        depositedBidderIds.clear();
    }

    public List<AuctionDepositRefSchema> getDepositRefs() {
        ensureDepositRefs();
        return new ArrayList<>(depositRefs);
    }

    public void setDepositRefs(List<AuctionDepositRefSchema> depositRefs) {
        this.depositRefs = depositRefs != null ? new ArrayList<>(depositRefs) : new ArrayList<>();
    }

    public AuctionDepositRefSchema getDepositRef(String bidderId) {
        if (bidderId == null) {
            return null;
        }
        ensureDepositRefs();
        for (AuctionDepositRefSchema ref : depositRefs) {
            if (bidderId.equals(ref.getBidderId())) {
                return ref;
            }
        }
        return null;
    }

    public List<String> getLegacyDepositedBidderIds() {
        ensureDepositedBidderIds();
        return new ArrayList<>(depositedBidderIds);
    }

    private void ensureDepositedBidderIds() {
        if (depositedBidderIds == null) {
            depositedBidderIds = new ArrayList<>();
        }
    }

    private void ensureDepositRefs() {
        if (depositRefs == null) {
            depositRefs = new ArrayList<>();
        }
    }

    public LocalDateTime getPaymentDeadlineAt() { return paymentDeadlineAt; }
    public void setPaymentDeadlineAt(LocalDateTime paymentDeadlineAt) { this.paymentDeadlineAt = paymentDeadlineAt; }

    public int getAntiSnipingWindowSeconds() { return antiSnipingWindowSeconds; }
    public void setAntiSnipingWindowSeconds(int antiSnipingWindowSeconds) { this.antiSnipingWindowSeconds = antiSnipingWindowSeconds; }

    public int getAntiSnipingExtensionSeconds() { return antiSnipingExtensionSeconds; }
    public void setAntiSnipingExtensionSeconds(int antiSnipingExtensionSeconds) { this.antiSnipingExtensionSeconds = antiSnipingExtensionSeconds; }
}
