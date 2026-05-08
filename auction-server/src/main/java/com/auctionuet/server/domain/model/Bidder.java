package com.auctionuet.server.domain.model;
import com.auctionuet.server.domain.enums.UserRole;
import com.auctionuet.server.domain.enums.Permission;

public class Bidder extends User {
    public Bidder(String id, String username) {
        super(id, username, UserRole.BIDDER);
    }

    @Override
    public boolean hasPermission(Permission action) {
        return switch (action) {
            case PLACE_BID, VIEW_AUCTION, VIEW_BID_HISTORY,
                 GET_PROFILE, UPDATE_PROFILE, SUBSCRIBE, UNSUBSCRIBE
                    -> true;
            default -> false;
        };
    }

    @Override
    public String getDisplayInfo() {
        return "Bidder: " + getUsername();
    }
}
