package com.auctionuet.server.domain.model;

import com.auctionuet.server.domain.enums.UserRole;
import com.auctionuet.server.domain.enums.Permission;

public class Admin extends User {
    public Admin(String id,String name) {
        super(id,name, UserRole.ADMIN);
    }

    @Override
    public boolean hasPermission(Permission action) {
        return switch (action) {
            case MANAGE_USERS, GET_ALL_USERS, DELETE_USER, UPDATE_ROLE,
                 VIEW_AUCTION, VIEW_BID_HISTORY,
                 GET_PROFILE, UPDATE_PROFILE
                    -> true;
            default -> false;
        };
    }

    @Override
    public String getDisplayInfo() {
        return "Admin: " + getUsername();
    }
}
