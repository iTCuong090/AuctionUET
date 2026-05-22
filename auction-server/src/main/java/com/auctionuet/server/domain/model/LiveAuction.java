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
            validateAutoBidConfig(config);
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

    private void validateAutoBidConfig(AutoBidConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Invalid autobid parameters");
        }
        double currentPrice = getCurrentPrice();
        if (config.getIncrement() <= 0 || config.getMaxBid() <= currentPrice) {
            throw new IllegalArgumentException(
                    "Invalid autobid parameters: maxBid must be greater than current price and increment must be positive");
        }
        if (!config.getBidderId().equals(currentWinnerId)
                && config.getMaxBid() < currentPrice + config.getIncrement()) {
            throw new IllegalArgumentException(
                    "Invalid autobid parameters: maxBid must be at least current price plus increment");
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
     * Xử lý auto-bid theo kiểu proxy:
     * người có trần cao nhất thắng, hòa trần thì người bật sớm hơn thắng.
     * Giá thắng được đẩy tới trần cao thứ hai cộng increment của người thắng.
     */
    public void resolveAutoBids(Consumer<BidRecord> onAutoBidPlaced) {
        // KHÔNG CẦN lock vì hàm này luôn được gọi khi đã giữ bidLock.
        deactivateAutoBidsBelowCurrentPrice();

        AutoBidConfig winningConfig = findBestAutoBidConfig();
        if (winningConfig == null) {
            return;
        }

        double newBidAmount = calculateProxyAutoBidAmount(winningConfig);
        if (!canApplyAutoBid(winningConfig, newBidAmount)) {
            deactivateExhaustedAutoBids();
            return;
        }

        BidRecord autoRecord = new BidRecord(
                winningConfig.getBidderId(),
                winningConfig.getBidderName(),
                newBidAmount,
                LocalDateTime.now(),
                BidType.AUTO
        );
        this.currentHighestBid = newBidAmount;
        this.currentWinnerId = winningConfig.getBidderId();
        this.bidHistory.add(autoRecord);

        // Gửi callback để lưu vào DB.
        if (onAutoBidPlaced != null) {
            onAutoBidPlaced.accept(autoRecord);
        }

        // Anti-sniping cho auto-bid.
        extendIfSniping();

        deactivateExhaustedAutoBids();
        notifyObservers(autoRecord);
    }

    private AutoBidConfig findBestAutoBidConfig() {
        return autoBidQueue.stream()
                .filter(AutoBidConfig::isActive)
                .min(AutoBidConfig::comparePriority)
                .orElse(null);
    }

    private AutoBidConfig findBestCompetingAutoBidConfig(String bidderId) {
        return autoBidQueue.stream()
                .filter(AutoBidConfig::isActive)
                .filter(config -> !config.getBidderId().equals(bidderId))
                .min(AutoBidConfig::comparePriority)
                .orElse(null);
    }

    private double calculateProxyAutoBidAmount(AutoBidConfig winningConfig) {
        AutoBidConfig competingConfig = findBestCompetingAutoBidConfig(winningConfig.getBidderId());
        if (winningConfig.getBidderId().equals(currentWinnerId)) {
            if (competingConfig == null) {
                return getCurrentPrice();
            }
            return Math.min(
                    winningConfig.getMaxBid(),
                    competingConfig.getMaxBid() + winningConfig.getIncrement());
        }

        double priceToBeat = getCurrentPrice();
        if (competingConfig != null) {
            priceToBeat = Math.max(priceToBeat, competingConfig.getMaxBid());
            return Math.min(winningConfig.getMaxBid(), priceToBeat + winningConfig.getIncrement());
        }

        double nextBidAmount = priceToBeat + winningConfig.getIncrement();
        if (nextBidAmount <= winningConfig.getMaxBid()) {
            return nextBidAmount;
        }
        return currentWinnerId == null ? getCurrentPrice() : winningConfig.getMaxBid();
    }

    private boolean canApplyAutoBid(AutoBidConfig winningConfig, double newBidAmount) {
        double currentPrice = getCurrentPrice();
        if (newBidAmount > currentPrice) {
            return true;
        }
        if (Double.compare(newBidAmount, currentPrice) != 0) {
            return false;
        }
        return canClaimCurrentPriceTie(winningConfig);
    }

    private boolean canClaimCurrentPriceTie(AutoBidConfig winningConfig) {
        if (currentWinnerId == null
                || winningConfig.getBidderId().equals(currentWinnerId)
                || Double.compare(winningConfig.getMaxBid(), getCurrentPrice()) != 0) {
            return false;
        }

        BidRecord currentWinningBid = findCurrentWinningBidRecord();
        return currentWinningBid != null
                && !winningConfig.getRegisteredAt().isAfter(currentWinningBid.getTimestamp());
    }

    private BidRecord findCurrentWinningBidRecord() {
        BidRecord latestWinningBid = null;
        for (BidRecord record : bidHistory) {
            if (!record.getBidderId().equals(currentWinnerId)
                    || Double.compare(record.getAmount(), getCurrentPrice()) != 0) {
                continue;
            }
            if (latestWinningBid == null
                    || record.getTimestamp().isAfter(latestWinningBid.getTimestamp())) {
                latestWinningBid = record;
            }
        }
        return latestWinningBid;
    }

    private void deactivateAutoBidsBelowCurrentPrice() {
        deactivateAutoBids(false);
    }

    private void deactivateExhaustedAutoBids() {
        deactivateAutoBids(true);
    }

    private void deactivateAutoBids(boolean includeCurrentPriceTies) {
        List<AutoBidConfig> exhaustedConfigs = autoBidQueue.stream()
                .filter(AutoBidConfig::isActive)
                .filter(config -> !config.getBidderId().equals(currentWinnerId))
                .filter(config -> includeCurrentPriceTies
                        ? config.getMaxBid() <= getCurrentPrice()
                        : config.getMaxBid() < getCurrentPrice())
                .toList();

        for (AutoBidConfig config : exhaustedConfigs) {
            config.markInactive();
            autoBidQueue.remove(config);
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
