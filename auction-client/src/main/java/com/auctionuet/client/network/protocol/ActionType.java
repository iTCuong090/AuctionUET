package com.auctionuet.client.network.protocol;

public enum ActionType {
    // Đồ cũ của tuần trước
    LOGIN,
    REGISTER,
    LOGOUT,
    PING,

    // ĐỒ MỚI CỦA TUẦN NÀY (Nhét thêm vào đây)

    // Dành cho Sản phẩm (Item)
    CREATE_ITEM,
    GET_MY_ITEMS,

    // Dành cho Đấu giá (Auction)
    CREATE_AUCTION,
    START_AUCTION,
    GET_AUCTIONS,
    GET_AUCTION_DETAIL
}