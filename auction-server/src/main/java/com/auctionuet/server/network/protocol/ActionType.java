package com.auctionuet.server.network.protocol;

public enum ActionType {
    //Utils
    PING,
    //Auth(tuần 3)
    LOGIN, REGISTER,LOGOUT,
    // User Profile (tuần 4 )
    GET_PROFILE, UPDATE_PROFILE,

    // Item Management (tuần 4 )
    CREATE_ITEM, UPDATE_ITEM, DELETE_ITEM, GET_MY_ITEMS,

    // Auction Management (tuần 4)
    CREATE_AUCTION, START_AUCTION, GET_AUCTIONS, GET_AUCTION_DETAIL,

    // Bidding (tuần 5)
    PLACE_BID, GET_BID_HISTORY, SUBSCRIBE, UNSUBSCRIBE,SET_AUTO_BID, CANCEL_AUTO_BID,

    // Admin (tuần 5)
    GET_ALL_USERS, DELETE_USER, UPDATE_ROLE,

    // Wallet (tuần 5)
    DEPOSIT, WITHDRAW, GET_WALLET,

    // Payment (tuần 5)
    PAY_AUCTION
}

