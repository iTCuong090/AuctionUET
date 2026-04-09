package com.auctionuet.server.domain.model;

import com.auctionuet.server.domain.enums.UserRole;

public class Seller extends User{
    public Seller(String id, String name) {super(id,name, UserRole.SELLER);
    }

    @Override
    public boolean hasPermission(String action) {
        return switch (action) {
            case "VIEW_AUCTION" -> true;
            case "CREATE_AUCTION" -> true;
            case "GET_PROFILE" -> true;
            case "UPDATE_PROFILE" -> true;
            case "VIEW_BID_HISTORY" -> true;
            case "PLACE_BID" -> false;
            case "MANAGE_USERS" -> false;
            default -> false;  // mặc định: không cho phép
        };
    }

    @Override
    public String getDisplayInfo() {
        return "Seller: " + getUsername();
    }
}

