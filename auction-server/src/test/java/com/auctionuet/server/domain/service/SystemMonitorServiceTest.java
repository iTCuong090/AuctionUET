package com.auctionuet.server.domain.service;

import com.auctionuet.protocol.dto.response.admin.SystemMonitorDTO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SystemMonitorServiceTest {
    @Test
    void testSnapshotNotificationSequenceAndUnsubscribe() {
        AtomicInteger connections = new AtomicInteger(1);
        AtomicInteger auctions = new AtomicInteger(2);
        SystemMonitorService service = new SystemMonitorService(connections::get, auctions::get);
        List<SystemMonitorDTO> updates = new ArrayList<>();

        SystemMonitorDTO initial = service.subscribe(updates::add);
        assertEquals(1, initial.getActiveConnectionCount());
        assertEquals(2, initial.getLiveAuctionCount());
        assertEquals(0, initial.getSequence());
        assertNotNull(initial.getCapturedAt());

        connections.incrementAndGet();
        service.notifyMetricsChanged();
        auctions.decrementAndGet();
        service.notifyMetricsChanged();

        assertEquals(2, updates.size());
        assertEquals(2, updates.get(0).getActiveConnectionCount());
        assertEquals(2, updates.get(0).getLiveAuctionCount());
        assertEquals(1, updates.get(0).getSequence());
        assertEquals(1, updates.get(1).getLiveAuctionCount());
        assertEquals(2, updates.get(1).getSequence());
    }

    @Test
    void testUnsubscribedObserverStopsReceivingUpdates() {
        SystemMonitorService service = new SystemMonitorService(() -> 1, () -> 0);
        List<SystemMonitorDTO> updates = new ArrayList<>();
        com.auctionuet.server.domain.model.SystemMonitorObserver observer = updates::add;

        service.subscribe(observer);
        service.notifyMetricsChanged();
        service.unsubscribe(observer);
        service.notifyMetricsChanged();

        assertEquals(1, updates.size());
        assertEquals(1, updates.get(0).getSequence());
    }
}
