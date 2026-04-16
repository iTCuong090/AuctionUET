package com.auctionuet.server.domain.manager;

import com.auctionuet.server.persistence.schema.AuctionSchema;

public class AuctionManager {

    private static final AuctionManager INSTANCE = new AuctionManager();

    private AuctionManager() {}

    public static AuctionManager getInstance() {
        return INSTANCE;
    }

    public void loadAuction(AuctionSchema schema) {
        // Mock implementation
    }

    public void endAuction(String auctionId) {
        // Mock implementation
    }
}
