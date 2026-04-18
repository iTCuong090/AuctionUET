package com.auctionuet.server.domain.model;

import com.auctionuet.server.domain.enums.AuctionStatus;
import com.auctionuet.server.exception.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;


public class LiveAuction {
    // Các biến Final
    private final String id;
    private final String itemId;
    private final String sellerId;
    private final LocalDateTime endTime;
    private final List<BidRecord> bidHistory;

    // Các biến ko final
    private AuctionStatus status;
    private double currentHighestBid;
    private String currentWinnerId;

    // Biến run time - only
    private final ReentrantLock bidLock;
    private final List<AuctionObserver> observers;

    // Constructor
    public LiveAuction(String id, String itemId, String sellerId,
                       LocalDateTime endTime, AuctionStatus status,
                       double currentHighestBid, String currentWinnerId) {
        this.id = id;
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.endTime = endTime;
        this.status = status;
        this.currentHighestBid = currentHighestBid;
        this.currentWinnerId = currentWinnerId;

        this.bidHistory = new ArrayList<>();
        this.bidLock = new ReentrantLock();
        this.observers = new CopyOnWriteArrayList<>(); // thread-safe list
    }


    public BidRecord placeBid(User bidder, double amount)
            throws InvalidBidException, AuctionClosedException {

        bidLock.lock();
        try {
            // 1. Validate
            if (status != AuctionStatus.RUNNING) {
                throw new AuctionClosedException("Phiên đấu giá đã kết thúc");
            }
            if (amount <= currentHighestBid) {
                throw new InvalidBidException(
                        "Giá phải lớn hơn giá hiện tại: " + currentHighestBid);
            }
            if (bidder.getId().equals(sellerId)) {
                throw new InvalidBidException("Người bán không được tự đấu giá");
            }

            // 2. Update state
            BidRecord record = new BidRecord(
                    bidder.getId(), bidder.getUsername(), amount, LocalDateTime.now());
            this.currentHighestBid = amount;
            this.currentWinnerId = bidder.getId();
            this.bidHistory.add(record);

            // 3. Notify observers
            notifyObservers(record);

            return record;
        } finally {
            bidLock.unlock();
        }
    }

    public void addObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(BidRecord record) {
        for (AuctionObserver obs : observers) {
            obs.onBidPlaced(record);
        }
    }

    public void notifyAuctionEnded() {
        for (AuctionObserver obs : observers) {
            obs.onAuctionEnded(id, currentWinnerId, currentHighestBid);
        }
    }

    public String getId() {
        return id;
    }
    public String getItemId() {
        return itemId;
    }
    public String getSellerId() {
        return sellerId;
    }
    public LocalDateTime getEndTime() {
        return endTime;
    }
    public AuctionStatus getStatus() {
        return status;
    }
    public double getCurrentHighestBid() {
        return currentHighestBid;
    }
    public String getCurrentWinnerId() {
        return currentWinnerId;
    }
    public List<BidRecord> getBidHistory() {
        return new ArrayList<>(bidHistory);
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }
}