package com.auctionuet.server.domain.model;

import com.auctionuet.server.domain.enums.UserRole;

public class Seller extends User{
    public Seller(String id, String name) {super(id,name, UserRole.SELLER);
    }

    @Override
    public boolean hasPermission(String action) {
        return switch (action) {
            case "CREATE_ITEM", "UPDATE_ITEM", "DELETE_ITEM", "GET_MY_ITEMS",
                 "CREATE_AUCTION", "START_AUCTION",
                 "VIEW_AUCTION", "VIEW_BID_HISTORY",
                 "GET_PROFILE", "UPDATE_PROFILE"
                    -> true;
            default -> false;
        };
    }

    @Override
    public String getDisplayInfo() {
        return "Seller: " + getUsername();
    }
}

