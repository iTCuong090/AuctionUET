package com.auctionuet.server.domain.model;

import com.auctionuet.server.domain.enums.UserRole;

public class Admin extends User {
    public Admin(String id,String name) {
        super(id,name, UserRole.ADMIN);
    }

    @Override
    public boolean hasPermission(String action) {
        return switch (action) {
            case "VIEW_AUCTION" -> true;
            case "GET_PROFILE" -> true;
            case "UPDATE_PROFILE" -> true;
            case "VIEW_BID_HISTORY" -> true;
            case "MANAGE_USERS" -> true;
            case "PLACE_BID" -> false;
            case "CREATE_AUCTION" -> false;
            default -> false;  // mặc định: không cho phép
        };
    }

    @Override
    public String getDisplayInfo() {
        return "Admin: " + getUsername();
    }
}

