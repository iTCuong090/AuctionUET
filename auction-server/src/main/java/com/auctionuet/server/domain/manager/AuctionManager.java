package com.auctionuet.server.domain.manager;

import com.auctionuet.protocol.enums.AuctionStatus;
import com.auctionuet.server.domain.model.LiveAuction;
import com.auctionuet.server.persistence.schema.AuctionSchema;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AuctionManager implements com.auctionuet.server.domain.model.AuctionObserver {

    // Singleton
    private AuctionManager() {}
    private static final AuctionManager INSTANCE = new AuctionManager();
    public static AuctionManager getInstance() {
        return INSTANCE;
    }

    private final Map<String, LiveAuction> liveAuctions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private java.util.function.Consumer<String> endAuctionCallback;

    public void setEndAuctionCallback(java.util.function.Consumer<String> callback) {
        this.endAuctionCallback = callback;
    }

    private void scheduleAuctionEnd(String auctionId, long delayMs) {
        ScheduledFuture<?> task = scheduledTasks.get(auctionId);
        if (task != null) task.cancel(false);

        if (delayMs > 0) {
            scheduledTasks.put(auctionId, scheduler.schedule(() -> {
                if (endAuctionCallback != null) {
                    endAuctionCallback.accept(auctionId);
                }
            }, delayMs, TimeUnit.MILLISECONDS));
        } else {
            if (endAuctionCallback != null) {
                endAuctionCallback.accept(auctionId);
            }
        }
    }

    public LiveAuction loadAuction(AuctionSchema schema) {
        LiveAuction auction = new LiveAuction(
                schema.getId(),
                schema.getItemId(),
                schema.getSellerId(),
                schema.getEndTime(),
                schema.getStatus(),
                schema.getHighestBid(),
                schema.getWinnerId(),
                schema.getAntiSnipingWindowSeconds(),
                schema.getAntiSnipingExtensionSeconds()
        );
        liveAuctions.put(auction.getId(), auction);
        auction.addObserver(this); // Đăng ký để lắng nghe sự kiện gia hạn

        // Schedule auto-end
        long delayMs = Duration.between(LocalDateTime.now(), schema.getEndTime()).toMillis();
        scheduleAuctionEnd(auction.getId(), delayMs);

        return auction;
    }

    public LiveAuction getAuction(String auctionId) {
        return liveAuctions.get(auctionId);
    }

    public void endAuction(String auctionId) {
        LiveAuction auction = liveAuctions.remove(auctionId);
        if (auction != null) {
            auction.setStatus(AuctionStatus.FINISHED);
            auction.notifyAuctionEnded();
            // Cường's AuctionService sẽ ghi kết quả vào DB
        }
    }

    public void extendAuction(String auctionId, LocalDateTime newEndTime) {
        LiveAuction auction = liveAuctions.get(auctionId);
        if (auction != null) {
            auction.setEndTime(newEndTime);
            long delayMs = Duration.between(LocalDateTime.now(), newEndTime).toMillis();
            scheduleAuctionEnd(auctionId, delayMs);
        }
    }

    // --- AuctionObserver methods ---
    @Override
    public void onBidPlaced(String auctionId, com.auctionuet.server.domain.model.BidRecord record) {
        // Do nothing here
    }

    @Override
    public void onAuctionEnded(String auctionId, String winnerId, double finalPrice) {
        // Do nothing here
    }

    @Override
    public void onAuctionExtended(String auctionId, LocalDateTime newEndTime) {
        extendAuction(auctionId, newEndTime);
    }
}
