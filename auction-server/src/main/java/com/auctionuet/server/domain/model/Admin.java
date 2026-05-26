package com.auctionuet.server.domain.model;

import com.auctionuet.protocol.enums.UserRole;
import com.auctionuet.protocol.enums.Permission;

public class Admin extends User {
    public Admin(String id, String name) {
        super(id, name, UserRole.ADMIN);
    }

    @Override
    public boolean hasPermission(Permission action) {
        return switch (action) {
            case MANAGE_USERS, GET_ALL_USERS, DELETE_USER, UPDATE_ROLE, UPDATE_USER_STATUS,
                 CANCEL_AUCTION_AS_ADMIN,
                 VIEW_ITEM_APPROVAL, MANAGE_ITEM_APPROVAL,
                 VIEW_FINANCIAL_AUDIT,
                 VIEW_SYSTEM_MONITOR,
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
