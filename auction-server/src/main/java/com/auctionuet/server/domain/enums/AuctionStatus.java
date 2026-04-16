package com.auctionuet.server.domain.enums;

public enum AuctionStatus {
    OPEN,       // Phiên đã tạo, chưa bắt đầu. Có thể sửa/xóa item
    RUNNING,    // Đang diễn ra, nhận bid từ Bidder
    FINISHED,   // Hết thời gian, đã xác định người thắng
    PAID,       // Người thắng đã thanh toán (tùy chọn, mở rộng sau)
    CANCELED    // Bị hủy bởi Seller hoặc không ai đặt giá
}

