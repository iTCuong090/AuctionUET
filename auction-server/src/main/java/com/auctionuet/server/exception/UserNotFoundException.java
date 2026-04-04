package com.auctionuet.server.exception;

public class UserNotFoundException extends AuctionException {
    public UserNotFoundException(String message) {
        super(message);
    }
}

