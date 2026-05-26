package com.auctionuet.protocol.dto.response.admin;

import com.auctionuet.protocol.dto.ValidatableDTO;

import java.time.LocalDateTime;

public class SystemMonitorDTO implements ValidatableDTO {
    private final int activeConnectionCount;
    private final int liveAuctionCount;
    private final long sequence;
    private final LocalDateTime capturedAt;

    public SystemMonitorDTO(
            int activeConnectionCount,
            int liveAuctionCount,
            long sequence,
            LocalDateTime capturedAt) {
        this.activeConnectionCount = activeConnectionCount;
        this.liveAuctionCount = liveAuctionCount;
        this.sequence = sequence;
        this.capturedAt = capturedAt;
        validate();
    }

    @Override
    public void validate() {
        if (activeConnectionCount < 0) {
            throw new IllegalArgumentException("activeConnectionCount must be greater than or equal to 0");
        }
        if (liveAuctionCount < 0) {
            throw new IllegalArgumentException("liveAuctionCount must be greater than or equal to 0");
        }
        if (sequence < 0) {
            throw new IllegalArgumentException("sequence must be greater than or equal to 0");
        }
        if (capturedAt == null) {
            throw new IllegalArgumentException("capturedAt must not be null");
        }
    }

    public int getActiveConnectionCount() {
        return activeConnectionCount;
    }

    public int getLiveAuctionCount() {
        return liveAuctionCount;
    }

    public long getSequence() {
        return sequence;
    }

    public LocalDateTime getCapturedAt() {
        return capturedAt;
    }
}
