package com.auctionuet.server.domain.model;
import com.auctionuet.server.domain.enums.UserRole;

public class Bidder extends User {
    public Bidder(String id, String name) {
        super(id, name, UserRole.BIDDER);
    }

    @Override
    public boolean hasPermission(String action) {
        return switch (action) {
            case "PLACE_BID" -> true;
            case "VIEW_AUCTION" -> true;
            case "VIEW_BID_HISTORY" -> true;
            case "GET_PROFILE" -> true;
            case "UPDATE_PROFILE" -> true;
            case "CREATE_AUCTION" -> false;
            case "MANAGE_USERS" -> false;
            default -> false;
        };
    }

    @Override
    public String getDisplayInfo() {
        return "Bidder: " + getUsername();
    }
}
