package com.auctionuet.server.domain.service;

import com.auctionuet.server.domain.manager.AuctionManager;
import com.auctionuet.server.domain.model.AutoBidConfig;
import com.auctionuet.server.domain.model.BidRecord;
import com.auctionuet.server.domain.model.LiveAuction;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.persistence.dao.AuctionDAO;
import com.auctionuet.server.persistence.dao.BidDAO;
import com.auctionuet.server.persistence.schema.AuctionSchema;
import com.auctionuet.server.persistence.schema.BidSchema;
import com.auctionuet.server.persistence.schema.ItemSchema;
import com.auctionuet.server.util.IdGenerator;

import java.time.LocalDateTime;
import java.util.List;

public class BidService {
    /**
     * Tỉ lệ tiền cọc bắt buộc (10% giá khởi điểm) để đảm bảo trách nhiệm tham gia đấu giá.
     */
    private static final double ESCROW_DEPOSIT_RATE = 0.10;
    private final AuctionManager auctionManager;
    private final WalletService walletService;
    private final BidDAO bidDAO;
    private final ItemService itemService;
    private final AuctionDAO auctionDAO;

    public BidService(AuctionManager auctionManager, WalletService walletService,
            BidDAO bidDAO, ItemService itemService, AuctionDAO auctionDAO) {
        this.auctionManager = auctionManager;
        this.walletService = walletService;
        this.bidDAO = bidDAO;
        this.itemService = itemService;
        this.auctionDAO = auctionDAO;
    }

    public synchronized BidRecord placeBid(User bidder, String auctionId, double amount) throws Exception {
        LiveAuction liveAuction = requireLiveAuction(auctionId, "Auction not found or not running");
        ItemSchema itemSchema = requireItemSchema(liveAuction.getItemId());
        double depositAmount = calculateDepositAmount(itemSchema);
        ensureDepositFrozen(liveAuction, bidder, depositAmount);

        String previousWinnerId = liveAuction.getCurrentWinnerId();

        BidRecord record = liveAuction.placeBid(bidder, amount, autoRecord -> {
            BidSchema autoBidSchema = toBidSchema(
                    auctionId,
                    autoRecord.getBidderId(),
                    autoRecord.getAmount(),
                    autoRecord.getTimestamp());
            bidDAO.save(autoBidSchema);
        });

        syncAuctionState(auctionId, liveAuction);
        unfreezePreviousWinnerDeposit(previousWinnerId, bidder.getId(), depositAmount);

        BidSchema bidSchema = toBidSchema(auctionId, bidder.getId(), amount, LocalDateTime.now());
        bidDAO.save(bidSchema);

        return record;
    }

    public List<BidSchema> getBidHistory(String auctionId) {
        return bidDAO.findByAuctionId(auctionId);
    }

    public synchronized void setAutoBid(User bidder, String auctionId, double maxBid, double increment) {
        LiveAuction liveAuction = requireLiveAuction(auctionId, "Auction not found");
        validateAutoBidParams(maxBid, increment, liveAuction.getCurrentHighestBid());
        ItemSchema itemSchema = requireItemSchema(liveAuction.getItemId());
        double depositAmount = calculateDepositAmount(itemSchema);
        ensureDepositFrozen(liveAuction, bidder, depositAmount);

        AutoBidConfig config = new AutoBidConfig(bidder.getId(), bidder.getUsername(), maxBid, increment);
        liveAuction.addAutoBid(config);
    }

    public void cancelAutoBid(User bidder, String auctionId) {
        LiveAuction liveAuction = requireLiveAuction(auctionId, "Auction not found");
        liveAuction.removeAutoBid(bidder.getId());
    }

    private LiveAuction requireLiveAuction(String auctionId, String errorMessage) {
        LiveAuction liveAuction = auctionManager.getAuction(auctionId);
        if (liveAuction == null) {
            throw new IllegalArgumentException(errorMessage);
        }
        return liveAuction;
    }

    private ItemSchema requireItemSchema(String itemId) {
        ItemSchema itemSchema = itemService.getItemSchemaById(itemId);
        if (itemSchema == null) {
            throw new IllegalArgumentException("Item not found");
        }
        return itemSchema;
    }

    private void validateAutoBidParams(double maxBid, double increment, double currentHighestBid) {
        if (maxBid <= currentHighestBid || increment <= 0) {
            throw new IllegalArgumentException("Invalid autobid parameters");
        }
    }

    private double calculateDepositAmount(ItemSchema itemSchema) {
        return itemSchema.getStartingPrice() * ESCROW_DEPOSIT_RATE;
    }

    private void ensureDepositFrozen(LiveAuction liveAuction, User bidder, double depositAmount) {
        if (!liveAuction.hasDeposited(bidder.getId())) {
            walletService.freezeDeposit(bidder.getId(), depositAmount);
            liveAuction.markDeposited(bidder.getId());
        }
    }

    private void syncAuctionState(String auctionId, LiveAuction liveAuction) {
        AuctionSchema auctionSchema = auctionDAO.findById(auctionId);
        if (auctionSchema == null) {
            return;
        }
        auctionSchema.setHighestBid(liveAuction.getCurrentHighestBid());
        auctionSchema.setWinnerId(liveAuction.getCurrentWinnerId());
        auctionSchema.setUpdatedAt(LocalDateTime.now());
        auctionSchema.setEndTime(liveAuction.getEndTime());
        auctionDAO.update(auctionSchema);
    }

    private void unfreezePreviousWinnerDeposit(String previousWinnerId, String currentBidderId, double depositAmount) {
        if (previousWinnerId != null && !previousWinnerId.equals(currentBidderId)) {
            walletService.unfreezeDeposit(previousWinnerId, depositAmount);
        }
    }

    private BidSchema toBidSchema(String auctionId, String bidderId, double amount, LocalDateTime bidTime) {
        LocalDateTime now = LocalDateTime.now();
        return new BidSchema(
                IdGenerator.generate(),
                now,
                now,
                auctionId,
                bidderId,
                amount,
                bidTime);
    }
}
