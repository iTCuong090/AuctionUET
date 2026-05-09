package com.auctionuet.server.domain.model;

import java.time.LocalDateTime;

/**
 * Observer interface cho LiveAuction.
 * Dùng để thông báo các sự kiện đấu giá (bid mới, phiên kết thúc) đến các listener.
 */
public interface AuctionObserver {

    /**
     * Được gọi khi có một bid mới được đặt thành công.
     * @param record thông tin bid mới
     */
    void onBidPlaced(BidRecord record);

    /**
     * Được gọi khi phiên đấu giá kết thúc.
     * @param auctionId ID phiên đấu giá
     * @param winnerId ID người thắng (có thể null nếu không ai đặt giá)
     * @param finalPrice giá cuối cùng
     */
    void onAuctionEnded(String auctionId, String winnerId, double finalPrice);

    /**
     * Được gọi khi phiên đấu giá được gia hạn thêm thời gian.
     * @param auctionId ID phiên đấu giá
     * @param newEndTime thời gian kết thúc mới
     */
    void onAuctionExtended(String auctionId, LocalDateTime newEndTime);
}
