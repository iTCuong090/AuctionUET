package com.auctionuet.protocol;

public enum ActionType {
    //Utils
    PING,
    //Auth(tuáº§n 3)
    LOGIN, REGISTER,LOGOUT,
    // User Profile (tuáº§n 4 )
    GET_PROFILE, UPDATE_PROFILE,

    // Item Management (tuáº§n 4 )
    CREATE_ITEM, UPDATE_ITEM, DELETE_ITEM, GET_MY_ITEMS,

    // Auction Management (tuáº§n 4)
    CREATE_AUCTION, START_AUCTION, GET_AUCTIONS, GET_AUCTION_DETAIL,

    // Bidding (tuáº§n 5)
    PLACE_BID, GET_BID_HISTORY, SUBSCRIBE, UNSUBSCRIBE,SET_AUTO_BID, CANCEL_AUTO_BID, CHECK_AUTO_BID,

    // Admin (tuáº§n 5)
    GET_ALL_USERS, DELETE_USER, UPDATE_ROLE,

    // Wallet (tuáº§n 5)
    DEPOSIT, WITHDRAW, GET_WALLET,

    // Payment (tuáº§n 5)
    PAY_AUCTION
}


