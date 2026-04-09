package com.auctionuet.server.exception;

public class UserNotFoundException extends AuctionException {
    public UserNotFoundException(String username) {
        super("User không tồn tại: " + username);
    }
}

