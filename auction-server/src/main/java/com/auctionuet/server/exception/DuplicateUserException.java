package com.auctionuet.server.exception;

public class DuplicateUserException extends AuctionException {
    public DuplicateUserException(String username) {
        super("Username đã tồn tại: " + username);
    }
}

