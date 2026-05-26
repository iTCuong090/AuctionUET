package com.auctionuet.server.domain.manager;

import com.auctionuet.protocol.enums.AuctionStatus;
import com.auctionuet.server.persistence.schema.AuctionSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuctionManagerSystemMonitorTest {
    private static final String AUCTION_ID = "monitor-test-auction";
    private final AuctionManager auctionManager = AuctionManager.getInstance();

    @AfterEach
    void cleanup() {
        auctionManager.setLiveAuctionCountChangeCallback(null);
        auctionManager.removeLiveAuction(AUCTION_ID);
    }

    @Test
    void testCallbackOnlyRunsWhenLiveAuctionCountChanges() {
        auctionManager.removeLiveAuction(AUCTION_ID);
        int initialCount = auctionManager.getLiveAuctionCount();
        AtomicInteger updates = new AtomicInteger();
        auctionManager.setLiveAuctionCountChangeCallback(updates::incrementAndGet);
        AuctionSchema schema = runningAuction();

        auctionManager.loadAuction(schema, 100.0);
        assertEquals(initialCount + 1, auctionManager.getLiveAuctionCount());
        assertEquals(1, updates.get());

        auctionManager.loadAuction(schema, 100.0);
        assertEquals(initialCount + 1, auctionManager.getLiveAuctionCount());
        assertEquals(1, updates.get());

        auctionManager.removeLiveAuction(AUCTION_ID);
        assertEquals(initialCount, auctionManager.getLiveAuctionCount());
        assertEquals(2, updates.get());
    }

    private AuctionSchema runningAuction() {
        LocalDateTime now = LocalDateTime.now();
        return new AuctionSchema(
                AUCTION_ID,
                now,
                now,
                "item-monitor",
                "seller-monitor",
                "Phiên giám sát",
                "Dữ liệu kiểm thử",
                now.minusMinutes(1),
                now.plusMinutes(5),
                AuctionStatus.RUNNING,
                0,
                null,
                60,
                120);
    }
}
