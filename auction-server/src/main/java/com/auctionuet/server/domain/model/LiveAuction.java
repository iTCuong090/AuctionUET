package com.auctionuet.server.domain.model;

import com.auctionuet.protocol.enums.AuctionStatus;
import com.auctionuet.protocol.enums.BidType;
import com.auctionuet.server.exception.*;

import java.time.Duration;
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
import java.util.function.Supplier;


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
    private LocalDateTime currentLeaderSince;
    private LocalDateTime endTime;

    // Biến run time - only
    private final ReentrantLock bidLock;
    private final List<AuctionObserver> observers;
    private final Supplier<LocalDateTime> nowSupplier;

    // Anti Snipping
    private int antiSnipingWindowSeconds;      // X giây
    private int antiSnipingExtensionSeconds;   // Y giây
    private final PriorityQueue<AutoBidConfig> autoBidQueue = new PriorityQueue<>();
    private final Set<String> depositedBidders = ConcurrentHashMap.newKeySet();
    private long pendingAutoBidSequence;
    private PendingAutoBid pendingAutoBid;
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
        this(id, itemId, sellerId, endTime, status, currentHighestBid, currentWinnerId,
                antiSnipingWindowSeconds, antiSnipingExtensionSeconds, startingPrice, null);
    }

    public LiveAuction(String id, String itemId, String sellerId,
                       LocalDateTime endTime, AuctionStatus status,
                       double currentHighestBid, String currentWinnerId,
                       int antiSnipingWindowSeconds, int antiSnipingExtensionSeconds,
                       double startingPrice, LocalDateTime currentLeaderSince) {
        this(id, itemId, sellerId, endTime, status, currentHighestBid, currentWinnerId,
                antiSnipingWindowSeconds, antiSnipingExtensionSeconds, startingPrice,
                currentLeaderSince, LocalDateTime::now);
    }

    public LiveAuction(String id, String itemId, String sellerId,
                       LocalDateTime endTime, AuctionStatus status,
                       double currentHighestBid, String currentWinnerId,
                       int antiSnipingWindowSeconds, int antiSnipingExtensionSeconds,
                       double startingPrice, LocalDateTime currentLeaderSince,
                       Supplier<LocalDateTime> nowSupplier) {
        this.id = id;
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.startingPrice = startingPrice;
        this.nowSupplier = nowSupplier != null ? nowSupplier : LocalDateTime::now;
        this.endTime = endTime;
        this.status = status;
        this.currentHighestBid = currentHighestBid;
        this.currentWinnerId = currentWinnerId;
        this.currentLeaderSince = currentLeaderSince != null ? currentLeaderSince : now();

        this.bidHistory = new ArrayList<>();
        this.bidLock = new ReentrantLock();
        this.observers = new CopyOnWriteArrayList<>(); // thread-safe list

        this.antiSnipingWindowSeconds = antiSnipingWindowSeconds;
        this.antiSnipingExtensionSeconds = antiSnipingExtensionSeconds;

    }

    public record BidPlacementResult(BidRecord manualBid, PendingAutoBid pendingAutoBid) {}

    public record PendingAutoBid(
            long sequenceId,
            String autoBidderId,
            String manualBidderId,
            double manualAmount,
            LocalDateTime createdAt) {}

    public record AutoBidResolution(BidRecord autoBid, boolean autoBidDeactivated) {}

    public record AutoBidStateSnapshot(
            boolean hasConfig,
            boolean active,
            double maxBid,
            double increment,
            String currentWinnerId,
            boolean pendingAgainstCurrentLeader) {}


    public BidRecord placeBid(User bidder, double amount, Consumer<BidRecord> onAutoBidPlaced)
            throws InvalidBidException, AuctionClosedException {
        return placeBid(bidder, amount).manualBid();
    }

    public BidPlacementResult placeBid(User bidder, double amount)
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

            AutoBidConfig pendingAutoBidConfig = getPendingAutoBidConfigForManualBid(bidder.getId());
            AutoBidConfig previousLeaderAutoBid = pendingAutoBidConfig != null
                    ? pendingAutoBidConfig
                    : getActiveAutoBidConfig(currentWinnerId);

            // 2. Update state
            long sequenceId = ++pendingAutoBidSequence;
            pendingAutoBid = null;
            BidRecord record = new BidRecord(
                    bidder.getId(), bidder.getUsername(), amount, now(), BidType.MANUAL);
            this.currentHighestBid = amount;
            this.currentWinnerId = bidder.getId();
            this.currentLeaderSince = record.getTimestamp();
            this.bidHistory.add(record);

            // Anti-Sniping check — THÊM MỚI
            boolean extended = extendIfSniping();

            // Notify observers
            notifyObservers(record);

            PendingAutoBid pending = createPendingAutoBid(previousLeaderAutoBid, bidder.getId(), amount, sequenceId);

            return new BidPlacementResult(record, pending);
        } finally {
            bidLock.unlock();
        }
    }

    // === ANTI-SNIPING ===
    public boolean extendIfSniping() {
        LocalDateTime now = now();
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
        addAutoBid(config, null, Duration.ZERO);
    }

    public void addAutoBid(AutoBidConfig config, Consumer<BidRecord> onAutoBidPlaced) {
        addAutoBid(config, onAutoBidPlaced, Duration.ZERO);
    }

    public void addAutoBid(
            AutoBidConfig config,
            Consumer<BidRecord> onAutoBidPlaced,
            Duration requiredLeaderDuration) {
        bidLock.lock();
        try {
            validateAutoBidCanBeEnabledNoLock(config.getBidderId(), requiredLeaderDuration);
            // Remove existing config for same bidder (update scenario)
            autoBidQueue.removeIf(c -> c.getBidderId().equals(config.getBidderId()));
            autoBidQueue.add(config);
            // Also store in map for quick lookup
            autoBids.put(config.getBidderId(), config);
        } finally {
            bidLock.unlock();
        }
    }

    public boolean removeAutoBid(String bidderId) {
        bidLock.lock();
        try {
            autoBidQueue.removeIf(c -> c.getBidderId().equals(bidderId));
            autoBids.remove(bidderId);
            if (pendingAutoBid != null && pendingAutoBid.autoBidderId().equals(bidderId)) {
                pendingAutoBid = null;
                pendingAutoBidSequence++;
                return true;
            }
            return false;
        } finally {
            bidLock.unlock();
        }
    }

    public AutoBidResolution resolvePendingAutoBid(long sequenceId, Consumer<BidRecord> onAutoBidPlaced) {
        bidLock.lock();
        try {
            if (pendingAutoBid == null || pendingAutoBid.sequenceId() != sequenceId) {
                return new AutoBidResolution(null, false);
            }

            PendingAutoBid pending = pendingAutoBid;
            pendingAutoBid = null;

            if (status != AuctionStatus.RUNNING
                    || !pending.manualBidderId().equals(currentWinnerId)
                    || Double.compare(pending.manualAmount(), currentHighestBid) != 0) {
                return new AutoBidResolution(null, false);
            }

            AutoBidConfig autoBid = getActiveAutoBidConfig(pending.autoBidderId());
            if (autoBid == null) {
                return new AutoBidResolution(null, false);
            }

            if (currentHighestBid > autoBid.getMaxBid()) {
                deactivateAutoBid(autoBid);
                return new AutoBidResolution(null, true);
            }

            BidRecord autoRecord = createAutoBidRecord(autoBid);
            this.currentHighestBid = autoRecord.getAmount();
            this.currentWinnerId = autoBid.getBidderId();
            this.currentLeaderSince = autoRecord.getTimestamp();
            this.bidHistory.add(autoRecord);

            if (onAutoBidPlaced != null) {
                onAutoBidPlaced.accept(autoRecord);
            }

            extendIfSniping();
            notifyObservers(autoRecord);

            return new AutoBidResolution(autoRecord, false);
        } finally {
            bidLock.unlock();
        }
    }

    public AutoBidStateSnapshot getAutoBidStateSnapshot(String bidderId) {
        bidLock.lock();
        try {
            AutoBidConfig config = autoBids.get(bidderId);
            if (config == null) {
                return new AutoBidStateSnapshot(false, false, 0.0, 0.0, currentWinnerId, false);
            }
            boolean pendingAgainstCurrentLeader = pendingAutoBid != null
                    && pendingAutoBid.manualBidderId().equals(currentWinnerId)
                    && pendingAutoBid.manualBidderId().equals(bidderId);
            return new AutoBidStateSnapshot(
                    true,
                    config.isActive(),
                    config.getMaxBid(),
                    config.getIncrement(),
                    currentWinnerId,
                    pendingAgainstCurrentLeader);
        } finally {
            bidLock.unlock();
        }
    }

    public void validateAutoBidCanBeEnabled(String bidderId, Duration requiredLeaderDuration) {
        bidLock.lock();
        try {
            validateAutoBidCanBeEnabledNoLock(bidderId, requiredLeaderDuration);
        } finally {
            bidLock.unlock();
        }
    }

    private void validateAutoBidCanBeEnabledNoLock(String bidderId, Duration requiredLeaderDuration) {
        if (!bidderId.equals(currentWinnerId)) {
            throw new IllegalArgumentException("chỉ người dẫn đầu mới được bật Autobid");
        }
        if (pendingAutoBid != null && pendingAutoBid.manualBidderId().equals(bidderId)) {
            throw new IllegalArgumentException("Vui lòng chờ Auto-Bid hiện tại phản ứng xong");
        }
        Duration leaderDuration = Duration.between(currentLeaderSince, now());
        if (leaderDuration.compareTo(requiredLeaderDuration) < 0) {
            throw new IllegalArgumentException(buildLeaderStabilityMessage(requiredLeaderDuration));
        }
    }

    private PendingAutoBid createPendingAutoBid(
            AutoBidConfig previousLeaderAutoBid,
            String manualBidderId,
            double manualAmount,
            long sequenceId) {
        if (previousLeaderAutoBid == null
                || !previousLeaderAutoBid.isActive()
                || previousLeaderAutoBid.getBidderId().equals(manualBidderId)) {
            return null;
        }
        pendingAutoBid = new PendingAutoBid(
                sequenceId,
                previousLeaderAutoBid.getBidderId(),
                manualBidderId,
                manualAmount,
                now());
        return pendingAutoBid;
    }

    private BidRecord createAutoBidRecord(AutoBidConfig previousLeaderAutoBid) {
        double newBidAmount = Math.min(
                previousLeaderAutoBid.getMaxBid(),
                currentHighestBid + previousLeaderAutoBid.getIncrement());
        return new BidRecord(
                previousLeaderAutoBid.getBidderId(),
                previousLeaderAutoBid.getBidderName(),
                newBidAmount,
                now(),
                BidType.AUTO);
    }

    private String buildLeaderStabilityMessage(Duration requiredLeaderDuration) {
        long millis = Math.max(1, requiredLeaderDuration.toMillis());
        long seconds = Math.max(1, (millis + 999) / 1000);
        return "Bạn cần dẫn đầu ít nhất " + seconds + " giây để bật Auto-Bid";
    }

    private LocalDateTime now() {
        return nowSupplier.get();
    }

    private AutoBidConfig getActiveAutoBidConfig(String bidderId) {
        if (bidderId == null) {
            return null;
        }
        AutoBidConfig config = autoBids.get(bidderId);
        return config != null && config.isActive() ? config : null;
    }

    private AutoBidConfig getPendingAutoBidConfigForManualBid(String manualBidderId) {
        if (pendingAutoBid == null || pendingAutoBid.autoBidderId().equals(manualBidderId)) {
            return null;
        }
        return getActiveAutoBidConfig(pendingAutoBid.autoBidderId());
    }

    private void deactivateAutoBid(AutoBidConfig config) {
        config.markInactive();
        autoBidQueue.remove(config);
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
    public LocalDateTime getCurrentLeaderSince() {
        return currentLeaderSince;
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
