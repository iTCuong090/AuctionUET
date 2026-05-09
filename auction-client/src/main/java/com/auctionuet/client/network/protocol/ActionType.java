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
    GET_AUCTION_DETAIL,

    // MỚI tuần 5 — Bidding
    PLACE_BID,
    GET_BID_HISTORY,
    SET_AUTO_BID,
    CANCEL_AUTO_BID,

    // MỚI tuần 5 — Bidding (Realtime Push)
    SUBSCRIBE,
    UNSUBSCRIBE,

    // MỚI tuần 5 — Wallet
    DEPOSIT,
    WITHDRAW,
    GET_WALLET,

    // MỚI tuần 5 — Payment
    PAY_AUCTION
}