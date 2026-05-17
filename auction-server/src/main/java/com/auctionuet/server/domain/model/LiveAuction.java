package com.auctionuet.server.domain.model;

import com.auctionuet.protocol.enums.AuctionStatus;
import com.auctionuet.protocol.enums.BidType;
import com.auctionuet.server.exception.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;


public class LiveAuction {
    // Các biến Final
    private final String id;
    private final String itemId;
    private final String sellerId;
    private final double startingPrice;
    private final List<BidRecord> bidHistory;
    private final Map<String, AutoBidConfig> autoBids = new ConcurrentHashMap<>();

    // Các biến ko final
    private AuctionStatus status;
    private double currentHighestBid;
    private String currentWinnerId;
    private LocalDateTime endTime;

    // Biến run time - only
    private final ReentrantLock bidLock;
    private final List<AuctionObserver> observers;

    // Anti Snipping
    private int antiSnipingWindowSeconds;      // X giây
    private int antiSnipingExtensionSeconds;   // Y giây
    private final PriorityQueue<AutoBidConfig> autoBidQueue = new PriorityQueue<>();
    private final Set<String> depositedBidders = ConcurrentHashMap.newKeySet();
    // Track ai đã cọc rồi để BidService không freeze 2 lần


    // Constructor
    public LiveAuction(String id, String itemId, String sellerId,
                       LocalDateTime endTime, AuctionStatus status,
                       double currentHighestBid, String currentWinnerId,
                       int antiSnipingWindowSeconds, int antiSnipingExtensionSeconds) {
        this(id, itemId, sellerId, endTime, status, currentHighestBid, currentWinnerId,
                antiSnipingWindowSeconds, antiSnipingExtensionSeconds, currentHighestBid);
    }

    public LiveAuction(String id, String itemId, String sellerId,
                       LocalDateTime endTime, AuctionStatus status,
                       double currentHighestBid, String currentWinnerId,
                       int antiSnipingWindowSeconds, int antiSnipingExtensionSeconds,
                       double startingPrice) {
        this.id = id;
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.startingPrice = startingPrice;
        this.endTime = endTime;
        this.status = status;
        this.currentHighestBid = currentHighestBid;
        this.currentWinnerId = currentWinnerId;

        this.bidHistory = new ArrayList<>();
        this.bidLock = new ReentrantLock();
        this.observers = new CopyOnWriteArrayList<>(); // thread-safe list

        this.antiSnipingWindowSeconds = antiSnipingWindowSeconds;
        this.antiSnipingExtensionSeconds = antiSnipingExtensionSeconds;

    }


    public BidRecord placeBid(User bidder, double amount, Consumer<BidRecord> onAutoBidPlaced)
            throws InvalidBidException, AuctionClosedException {

        bidLock.lock();
        try {
            // 1. Validate
            if (status != AuctionStatus.RUNNING) {
                throw new AuctionClosedException("Phiên đấu giá đã kết thúc");
            }
            double currentPrice = getCurrentPrice();
            if (amount <= currentPrice) {
                throw new InvalidBidException(
                        "Giá phải lớn hơn giá hiện tại: " + currentPrice);
            }
            if (bidder.getId().equals(sellerId)) {
                throw new InvalidBidException("Người bán không được tự đấu giá");
            }

            // 2. Update state
            BidRecord record = new BidRecord(
                    bidder.getId(), bidder.getUsername(), amount, LocalDateTime.now(), BidType.MANUAL);
            this.currentHighestBid = amount;
            this.currentWinnerId = bidder.getId();
            this.bidHistory.add(record);

            // Anti-Sniping check — THÊM MỚI
            boolean extended = extendIfSniping();

            // Notify observers
            notifyObservers(record);

            // Trigger Auto-Bid resolution — THÊM MỚI
            resolveAutoBids(onAutoBidPlaced);

            return record;
        } finally {
            bidLock.unlock();
        }
    }

    // === ANTI-SNIPING ===
    public boolean extendIfSniping() {
        LocalDateTime now = LocalDateTime.now();
        long secondsLeft = java.time.Duration.between(now, endTime).getSeconds();

        if (secondsLeft <= antiSnipingWindowSeconds && secondsLeft > 0) {
            // Bid trong X giây cuối → gia hạn thêm Y giây
            this.endTime = this.endTime.plusSeconds(antiSnipingExtensionSeconds);
            // Notify observers về việc gia hạn
            for (AuctionObserver obs : observers) {
                obs.onAuctionExtended(id, endTime);
            }
            return true;
        }
        return false;
    }

    // === AUTO-BID ===
    public void addAutoBid(AutoBidConfig config) {
        addAutoBid(config, null);
    }

    public void addAutoBid(AutoBidConfig config, Consumer<BidRecord> onAutoBidPlaced) {
        bidLock.lock();
        try {
            // Remove existing config for same bidder (update scenario)
            autoBidQueue.removeIf(c -> c.getBidderId().equals(config.getBidderId()));
            autoBidQueue.add(config);
            // Also store in map for quick lookup
            autoBids.put(config.getBidderId(), config);
            resolveAutoBids(onAutoBidPlaced);
        } finally {
            bidLock.unlock();
        }
    }

    public void removeAutoBid(String bidderId) {
        bidLock.lock();
        try {
            autoBidQueue.removeIf(c -> c.getBidderId().equals(bidderId));
            autoBids.remove(bidderId);
        } finally {
            bidLock.unlock();
        }
    }

    /**
     * Xử lý Auto-Bid PriorityQueue.
     * Gọi SAU mỗi lần placeBid thành công.
     *
     * Logic:
     * 1. Lấy auto-bid config có maxBid cao nhất (đầu PriorityQueue)
     * 2. Nếu config này KHÔNG phải người đang dẫn đầu
     *    VÀ currentHighestBid + increment <= maxBid
     *    → Tự động đặt giá = currentHighestBid + increment
     * 3. Lặp lại cho đến khi:
     *    - Không còn auto-bid nào đủ điều kiện, HOẶC
     *    - Người dẫn đầu chính là top auto-bidder
     */

    public void resolveAutoBids(Consumer<BidRecord> onAutoBidPlaced) {
        // KHÔNG CẦN lock vì hàm này luôn được gọi trong placeBid() đã lock sẵn

        boolean keepResolving = true;
        int maxIterations = 100; // Safety: tránh infinite loop
        int iteration = 0;

        while (keepResolving && iteration < maxIterations) {
            iteration++;
            keepResolving = false;

            // Tìm auto-bid tốt nhất (không phải current winner)
            AutoBidConfig bestConfig = autoBidQueue.stream()
                    .filter(config -> !config.getBidderId().equals(currentWinnerId))
                    .min(AutoBidConfig::compareTo)
                    .orElse(null);

            if (bestConfig == null) break; // Không ai auto-bid

            double newBidAmount = getCurrentPrice() + bestConfig.getIncrement();

            if (newBidAmount <= bestConfig.getMaxBid()) {
                // Auto-bid!
                BidRecord autoRecord = new BidRecord(
                        bestConfig.getBidderId(),
                        bestConfig.getBidderName(),
                        newBidAmount,
                        LocalDateTime.now(),
                        BidType.AUTO
                );
                this.currentHighestBid = newBidAmount;
                this.currentWinnerId = bestConfig.getBidderId();
                this.bidHistory.add(autoRecord);
                
                // Gửi callback để lưu vào DB
                if (onAutoBidPlaced != null) {
                    onAutoBidPlaced.accept(autoRecord);
                }

                // Anti-sniping cho auto-bid
                extendIfSniping();

                // Notify
                notifyObservers(autoRecord);

                keepResolving = true; // Có thể còn auto-bid khác phản hồi
            } else {
                // maxBid không đủ → remove config
                autoBidQueue.remove(bestConfig);
                autoBids.remove(bestConfig.getBidderId());
            }
        }
    }

    // === DEPOSIT TRACKING ===
    public boolean hasDeposited(String bidderId) {
        return depositedBidders.contains(bidderId);
    }
    public void markDeposited(String bidderId) {
        if (bidderId != null && !bidderId.isBlank()) {
            depositedBidders.add(bidderId);
        }
    }
    public void markDeposited(Collection<String> bidderIds) {
        if (bidderIds == null) {
            return;
        }
        for (String bidderId : bidderIds) {
            markDeposited(bidderId);
        }
    }
    public void unmarkDeposited(String bidderId) {
        depositedBidders.remove(bidderId);
    }
    public List<String> getDepositedBidderIds() {
        return new ArrayList<>(depositedBidders);
    }




    public void addObserver(AuctionObserver observer) {
        observers.add(observer);
    }
    public void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }
    public void notifyAuctionEnded() {
        for (AuctionObserver obs : observers) {
            obs.onAuctionEnded(id, currentWinnerId, currentHighestBid);
        }
    }
    private void notifyObservers(BidRecord record) {
        for (AuctionObserver obs : observers) {
            obs.onBidPlaced(this.id, record);
        }
    }




    // Getters, Setters
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
    public double getCurrentPrice() {
        return currentHighestBid > 0 ? currentHighestBid : startingPrice;
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
    public int getAntiSnipingWindowSeconds() { return antiSnipingWindowSeconds; }
    public int getAntiSnipingExtensionSeconds() { return antiSnipingExtensionSeconds; }

    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public AutoBidConfig getAutoBidConfig(String bidderId) {
        return autoBids.get(bidderId);
    }

    public Map<String, AutoBidConfig> getAutoBids() {
        return autoBids;
    }
}
