package com.auctionuet.server.domain.manager;

import com.auctionuet.protocol.enums.AuctionStatus;
import com.auctionuet.server.domain.model.AuctionObserver;
import com.auctionuet.server.domain.model.BidRecord;
import com.auctionuet.server.domain.model.LiveAuction;
import com.auctionuet.server.persistence.schema.AuctionSchema;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AuctionManager implements AuctionObserver {
    public static final int PAYMENT_DEADLINE_MINUTES = 5;
    public static final int RECONCILE_INTERVAL_SECONDS = 30;

    private AuctionManager() {}

    private static final AuctionManager INSTANCE = new AuctionManager();

    public static AuctionManager getInstance() {
        return INSTANCE;
    }

    private final Map<String, LiveAuction> liveAuctions = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> startTasks = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> endTasks = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> paymentDeadlineTasks = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> autoBidResponseTasks = new ConcurrentHashMap<>();
    private final Map<String, Set<AuctionObserver>> observers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private ScheduledFuture<?> reconcileTask;
    private java.util.function.Consumer<String> startAuctionCallback;
    private java.util.function.Consumer<String> endAuctionCallback;
    private java.util.function.Consumer<String> paymentDeadlineCallback;
    private Runnable reconcileCallback;
    private Runnable liveAuctionCountChangeCallback;

    public void setStartAuctionCallback(java.util.function.Consumer<String> callback) {
        this.startAuctionCallback = callback;
    }

    public void setEndAuctionCallback(java.util.function.Consumer<String> callback) {
        this.endAuctionCallback = callback;
    }

    public void setPaymentDeadlineCallback(java.util.function.Consumer<String> callback) {
        this.paymentDeadlineCallback = callback;
    }

    public void setReconcileCallback(Runnable callback) {
        this.reconcileCallback = callback;
    }

    public void setLiveAuctionCountChangeCallback(Runnable callback) {
        this.liveAuctionCountChangeCallback = callback;
    }

    public int getLiveAuctionCount() {
        return liveAuctions.size();
    }

    public void scheduleAuctionStart(String auctionId, LocalDateTime startTime) {
        long delayMs = Duration.between(LocalDateTime.now(), startTime).toMillis();
        scheduleTask(startTasks, auctionId, delayMs, () -> {
            if (startAuctionCallback != null) {
                startAuctionCallback.accept(auctionId);
            }
        });
    }

    public void scheduleAuctionEnd(String auctionId, LocalDateTime endTime) {
        long delayMs = Duration.between(LocalDateTime.now(), endTime).toMillis();
        scheduleTask(endTasks, auctionId, delayMs, () -> {
            if (endAuctionCallback != null) {
                endAuctionCallback.accept(auctionId);
            }
        });
    }

    public void schedulePaymentDeadline(String auctionId, LocalDateTime deadline) {
        long delayMs = Duration.between(LocalDateTime.now(), deadline).toMillis();
        scheduleTask(paymentDeadlineTasks, auctionId, delayMs, () -> {
            if (paymentDeadlineCallback != null) {
                paymentDeadlineCallback.accept(auctionId);
            }
        });
    }

    public void scheduleAutoBidResponse(String auctionId, long delayMs, Runnable taskBody) {
        scheduleTask(autoBidResponseTasks, auctionId, delayMs, taskBody);
    }

    public void cancelAuctionStart(String auctionId) {
        cancelTask(startTasks, auctionId);
    }

    public void cancelAuctionEnd(String auctionId) {
        cancelTask(endTasks, auctionId);
    }

    public void cancelPaymentDeadline(String auctionId) {
        cancelTask(paymentDeadlineTasks, auctionId);
    }

    public void cancelAutoBidResponse(String auctionId) {
        cancelTask(autoBidResponseTasks, auctionId);
    }

    public void cancelAllTasks(String auctionId) {
        cancelAuctionStart(auctionId);
        cancelAuctionEnd(auctionId);
        cancelPaymentDeadline(auctionId);
        cancelAutoBidResponse(auctionId);
    }

    public synchronized void startPeriodicReconcile() {
        if (reconcileTask != null && !reconcileTask.isCancelled() && !reconcileTask.isDone()) {
            return;
        }
        reconcileTask = scheduler.scheduleWithFixedDelay(() -> {
            if (reconcileCallback != null) {
                reconcileCallback.run();
            }
        }, RECONCILE_INTERVAL_SECONDS, RECONCILE_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public LiveAuction loadAuction(AuctionSchema schema) {
        return loadAuction(schema, schema.getHighestBid());
    }

    public LiveAuction loadAuction(AuctionSchema schema, double startingPrice) {
        return loadAuction(schema, startingPrice, null);
    }

    public LiveAuction loadAuction(AuctionSchema schema, double startingPrice, LocalDateTime currentLeaderSince) {
        LiveAuction auction = toDomain(schema, startingPrice, currentLeaderSince);
        auction.markDeposited(schema.getDepositedBidderIds());
        LiveAuction previous = liveAuctions.put(auction.getId(), auction);
        auction.addObserver(this);
        scheduleAuctionEnd(auction.getId(), schema.getEndTime());
        if (previous == null) {
            notifyLiveAuctionCountChanged();
        }
        return auction;
    }

    public LiveAuction getAuction(String auctionId) {
        return liveAuctions.get(auctionId);
    }

    public void markAuctionStarted(AuctionSchema schema) {
        cancelAuctionStart(schema.getId());
        notifyAuctionStarted(schema.getId(), schema.getStartTime(), schema.getEndTime());
    }

    public void endAuction(String auctionId) {
        cancelAuctionEnd(auctionId);
        cancelAutoBidResponse(auctionId);
        LiveAuction auction = liveAuctions.remove(auctionId);
        if (auction != null) {
            auction.setStatus(AuctionStatus.FINISHED);
            auction.notifyAuctionEnded();
            notifyLiveAuctionCountChanged();
        }
    }

    public void removeLiveAuction(String auctionId) {
        cancelAuctionEnd(auctionId);
        cancelAutoBidResponse(auctionId);
        if (liveAuctions.remove(auctionId) != null) {
            notifyLiveAuctionCountChanged();
        }
    }

    public void notifyAuctionCanceled(String auctionId, String reason, LocalDateTime canceledAt) {
        for (AuctionObserver observer : observers.getOrDefault(auctionId, Set.of())) {
            observer.onAuctionCanceled(auctionId, reason, canceledAt);
        }
    }

    public void extendAuction(String auctionId, LocalDateTime newEndTime) {
        LiveAuction auction = liveAuctions.get(auctionId);
        if (auction != null) {
            auction.setEndTime(newEndTime);
        }
        scheduleAuctionEnd(auctionId, newEndTime);
    }

    public void addObserver(String auctionId, AuctionObserver observer) {
        if (auctionId == null || observer == null) {
            return;
        }
        observers.computeIfAbsent(auctionId, id -> ConcurrentHashMap.newKeySet()).add(observer);
    }

    public void removeObserver(String auctionId, AuctionObserver observer) {
        if (auctionId == null || observer == null) {
            return;
        }
        Set<AuctionObserver> auctionObservers = observers.get(auctionId);
        if (auctionObservers != null) {
            auctionObservers.remove(observer);
            if (auctionObservers.isEmpty()) {
                observers.remove(auctionId);
            }
        }
    }

    @Override
    public void onBidPlaced(String auctionId, BidRecord record) {
        for (AuctionObserver observer : observers.getOrDefault(auctionId, Set.of())) {
            observer.onBidPlaced(auctionId, record);
        }
    }

    @Override
    public void onAuctionEnded(String auctionId, String winnerId, double finalPrice) {
        for (AuctionObserver observer : observers.getOrDefault(auctionId, Set.of())) {
            observer.onAuctionEnded(auctionId, winnerId, finalPrice);
        }
    }

    @Override
    public void onAuctionExtended(String auctionId, LocalDateTime newEndTime) {
        extendAuction(auctionId, newEndTime);
        for (AuctionObserver observer : observers.getOrDefault(auctionId, Set.of())) {
            observer.onAuctionExtended(auctionId, newEndTime);
        }
    }

    private void notifyAuctionStarted(String auctionId, LocalDateTime startTime, LocalDateTime endTime) {
        for (AuctionObserver observer : observers.getOrDefault(auctionId, Set.of())) {
            observer.onAuctionStarted(auctionId, startTime, endTime);
        }
    }

    private void scheduleTask(
            Map<String, ScheduledFuture<?>> tasks,
            String auctionId,
            long delayMs,
            Runnable taskBody) {
        cancelTask(tasks, auctionId);
        Runnable wrapped = () -> {
            tasks.remove(auctionId);
            taskBody.run();
        };
        if (delayMs <= 0) {
            wrapped.run();
            return;
        }
        tasks.put(auctionId, scheduler.schedule(wrapped, delayMs, TimeUnit.MILLISECONDS));
    }

    private void cancelTask(Map<String, ScheduledFuture<?>> tasks, String auctionId) {
        ScheduledFuture<?> task = tasks.remove(auctionId);
        if (task != null) {
            task.cancel(false);
        }
    }

    private void notifyLiveAuctionCountChanged() {
        if (liveAuctionCountChangeCallback != null) {
            liveAuctionCountChangeCallback.run();
        }
    }

    private LiveAuction toDomain(AuctionSchema schema, double startingPrice, LocalDateTime currentLeaderSince) {
        return new LiveAuction(
                schema.getId(),
                schema.getItemId(),
                schema.getSellerId(),
                schema.getEndTime(),
                schema.getStatus(),
                schema.getHighestBid(),
                schema.getWinnerId(),
                schema.getAntiSnipingWindowSeconds(),
                schema.getAntiSnipingExtensionSeconds(),
                startingPrice,
                currentLeaderSince
        );
    }
}
