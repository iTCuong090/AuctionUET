package com.auctionuet.server.exception;

public class DuplicateUserException extends AuctionException {
    public DuplicateUserException(String message) {
        super(message);
    }
}

