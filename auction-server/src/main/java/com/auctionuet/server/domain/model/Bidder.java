package com.auctionuet.server.domain.model;

public class Bidder extends User {
    public Bidder(String id, String username) {
        super(id, username, com.auctionuet.server.domain.enums.UserRole.BIDDER);
    }

    @Override
    public boolean hasPermission(String action) { return false; }

    @Override
    public String getDisplayInfo() { return "Bidder: " + getUsername(); }
}
