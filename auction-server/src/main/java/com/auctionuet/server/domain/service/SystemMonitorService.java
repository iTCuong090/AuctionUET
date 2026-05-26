package com.auctionuet.server.domain.service;

import com.auctionuet.protocol.dto.response.admin.SystemMonitorDTO;
import com.auctionuet.server.domain.model.SystemMonitorObserver;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntSupplier;

public class SystemMonitorService {
    private final IntSupplier activeConnectionCountSupplier;
    private final IntSupplier liveAuctionCountSupplier;
    private final Set<SystemMonitorObserver> observers = ConcurrentHashMap.newKeySet();
    private final AtomicLong sequence = new AtomicLong();

    public SystemMonitorService(
            IntSupplier activeConnectionCountSupplier,
            IntSupplier liveAuctionCountSupplier) {
        this.activeConnectionCountSupplier = activeConnectionCountSupplier;
        this.liveAuctionCountSupplier = liveAuctionCountSupplier;
    }

    public SystemMonitorDTO subscribe(SystemMonitorObserver observer) {
        if (observer == null) {
            throw new IllegalArgumentException("Observer giám sát không hợp lệ");
        }
        observers.add(observer);
        return snapshot(sequence.get());
    }

    public void unsubscribe(SystemMonitorObserver observer) {
        if (observer != null) {
            observers.remove(observer);
        }
    }

    public void notifyMetricsChanged() {
        SystemMonitorDTO snapshot = snapshot(sequence.incrementAndGet());
        for (SystemMonitorObserver observer : observers) {
            observer.onSystemMonitorUpdated(snapshot);
        }
    }

    public SystemMonitorDTO getSnapshot() {
        return snapshot(sequence.get());
    }

    private SystemMonitorDTO snapshot(long snapshotSequence) {
        return new SystemMonitorDTO(
                activeConnectionCountSupplier.getAsInt(),
                liveAuctionCountSupplier.getAsInt(),
                snapshotSequence,
                LocalDateTime.now());
    }
}
