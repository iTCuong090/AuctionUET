package com.auctionuet.server.domain.manager;

import com.auctionuet.server.domain.enums.AuctionStatus;
import com.auctionuet.server.domain.model.LiveAuction;
import com.auctionuet.server.mapper.AuctionMapper;
import com.auctionuet.server.persistence.schema.AuctionSchema;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionManager {

    // Singleton
    private AuctionManager() {}
    private static final AuctionManager INSTANCE = new AuctionManager();
    public static AuctionManager getInstance() {
        return INSTANCE;
    }

    private final Map<String, LiveAuction> liveAuctions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public LiveAuction loadAuction(AuctionSchema schema) {
        LiveAuction auction = AuctionMapper.toDomain(schema);
        liveAuctions.put(auction.getId(), auction);

        // Schedule auto-end
        long delayMs = Duration.between(LocalDateTime.now(), schema.getEndTime()).toMillis();
        if (delayMs > 0) {
            scheduler.schedule(() -> endAuction(auction.getId()), delayMs, TimeUnit.MILLISECONDS);
        }

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
}
