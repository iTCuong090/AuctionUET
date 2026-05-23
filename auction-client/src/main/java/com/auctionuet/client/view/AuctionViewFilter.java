package com.auctionuet.client.view;

import com.auctionuet.protocol.dto.response.auction.AuctionDTO;
import com.auctionuet.protocol.enums.AuctionStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AuctionViewFilter {

    private AuctionViewFilter() {
    }

    static List<AuctionDTO> representativeAuctions(List<AuctionDTO> auctions) {
        Map<String, AuctionDTO> byItemId = new LinkedHashMap<>();
        if (auctions == null) {
            return new ArrayList<>();
        }

        for (AuctionDTO auction : auctions) {
            if (auction == null) {
                continue;
            }

            String key = auction.getItem() != null ? auction.getItem().getId() : auction.getId();
            AuctionDTO current = byItemId.get(key);
            if (current == null || isBetterRepresentative(auction, current)) {
                byItemId.put(key, auction);
            }
        }
        return new ArrayList<>(byItemId.values());
    }

    static boolean isBlockingStatus(AuctionStatus status) {
        return status == AuctionStatus.OPEN
                || status == AuctionStatus.RUNNING
                || status == AuctionStatus.WAITING_PAYMENT;
    }

    static AuctionDTO representativeAuction(List<AuctionDTO> auctions) {
        AuctionDTO selected = null;
        if (auctions == null) {
            return null;
        }
        for (AuctionDTO auction : auctions) {
            if (auction == null) {
                continue;
            }
            if (selected == null || isBetterRepresentative(auction, selected)) {
                selected = auction;
            }
        }
        return selected;
    }

    private static boolean isBetterRepresentative(AuctionDTO candidate, AuctionDTO current) {
        boolean candidateActive = isBlockingStatus(candidate.getStatus());
        boolean currentActive = isBlockingStatus(current.getStatus());
        if (candidateActive != currentActive) {
            return candidateActive;
        }

        if (candidateActive) {
            int candidateRank = activeStatusRank(candidate.getStatus());
            int currentRank = activeStatusRank(current.getStatus());
            if (candidateRank != currentRank) {
                return candidateRank > currentRank;
            }
        }

        return representativeTime(candidate).isAfter(representativeTime(current));
    }

    private static int activeStatusRank(AuctionStatus status) {
        if (status == AuctionStatus.WAITING_PAYMENT) {
            return 3;
        }
        if (status == AuctionStatus.RUNNING) {
            return 2;
        }
        if (status == AuctionStatus.OPEN) {
            return 1;
        }
        return 0;
    }

    private static LocalDateTime representativeTime(AuctionDTO auction) {
        if (auction.getPaidAt() != null) {
            return auction.getPaidAt();
        }
        if (auction.getPaymentDeadlineAt() != null) {
            return auction.getPaymentDeadlineAt();
        }
        if (auction.getEndTime() != null) {
            return auction.getEndTime();
        }
        if (auction.getStartTime() != null) {
            return auction.getStartTime();
        }
        return LocalDateTime.MIN;
    }
}
