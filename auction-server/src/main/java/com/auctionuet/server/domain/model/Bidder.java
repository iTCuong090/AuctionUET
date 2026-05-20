package com.auctionuet.server.domain.model;
import com.auctionuet.protocol.enums.UserRole;
import com.auctionuet.protocol.enums.Permission;

public class Bidder extends User {
    public Bidder(String id, String username) {
        super(id, username, UserRole.BIDDER);
    }

    @Override
    public boolean hasPermission(Permission action) {
        return switch (action) {
            case PLACE_BID, VIEW_AUCTION, VIEW_BID_HISTORY,
                 GET_PROFILE, UPDATE_PROFILE, SUBSCRIBE, UNSUBSCRIBE,
                 DEPOSIT, WITHDRAW, GET_WALLET, GET_MY_TRANSACTIONS, PAY_AUCTION,    // Wallet + Payment
                 SET_AUTO_BID, CANCEL_AUTO_BID                   // Auto-bid
                    -> true;
            default -> false;
        };
    }

    @Override
    public String getDisplayInfo() {
        return "Bidder: " + getUsername();
    }
}
