package com.auctionuet.server.domain.service;

import com.auctionuet.server.domain.manager.AuctionManager;
import com.auctionuet.server.domain.model.AutoBidConfig;
import com.auctionuet.server.domain.model.BidRecord;
import com.auctionuet.server.domain.model.LiveAuction;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.exception.AuctionClosedException;
import com.auctionuet.server.exception.InvalidBidException;
import com.auctionuet.server.persistence.dao.AuctionDAO;
import com.auctionuet.server.persistence.dao.BidDAO;
import com.auctionuet.server.persistence.schema.AuctionSchema;
import com.auctionuet.server.persistence.schema.BidSchema;
import com.auctionuet.server.persistence.schema.ItemSchema;
import com.auctionuet.server.util.IdGenerator;

import java.time.LocalDateTime;
import java.util.List;

public class BidService {
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
        LiveAuction liveAuction = auctionManager.getAuction(auctionId);
        if (liveAuction == null) {
            throw new IllegalArgumentException("Auction not found or not running");
        }

        ItemSchema itemSchema = itemService.getItemById(liveAuction.getItemId());
        if (itemSchema == null) {
            throw new IllegalArgumentException("Item not found");
        }
        double depositAmount = itemSchema.getStartingPrice() * 0.10;

        boolean hasDeposited = liveAuction.hasDeposited(bidder.getId());

        if (!hasDeposited) {
            walletService.freezeDeposit(bidder.getId(), depositAmount);
            liveAuction.markDeposited(bidder.getId());
        }

        String previousWinnerId = liveAuction.getCurrentWinnerId();

        BidRecord record = liveAuction.placeBid(bidder, amount, autoRecord -> {
            BidSchema autoBidSchema = new BidSchema(
                    IdGenerator.generate(), LocalDateTime.now(), LocalDateTime.now(),
                    auctionId, autoRecord.getBidderId(), autoRecord.getAmount(), autoRecord.getTimestamp()
            );
            bidDAO.save(autoBidSchema);
        });

        AuctionSchema auctionSchema = auctionDAO.findById(auctionId);
        if (auctionSchema != null) {
            auctionSchema.setHighestBid(liveAuction.getCurrentHighestBid());
            auctionSchema.setWinnerId(liveAuction.getCurrentWinnerId());
            auctionSchema.setUpdatedAt(LocalDateTime.now());

            // Sync endTime from LiveAuction (anti-sniping may have extended it)
            auctionSchema.setEndTime(liveAuction.getEndTime());

            auctionDAO.update(auctionSchema);
        }

        if (previousWinnerId != null && !previousWinnerId.equals(bidder.getId())) {
            walletService.unfreezeDeposit(previousWinnerId, depositAmount);
        }

        BidSchema bidSchema = new BidSchema(
                IdGenerator.generate(), LocalDateTime.now(), LocalDateTime.now(),
                auctionId, bidder.getId(), amount, LocalDateTime.now()
        );
        bidDAO.save(bidSchema);

        return record;
    }

    public List<BidSchema> getBidHistory(String auctionId) {
        return bidDAO.findByAuctionId(auctionId);
    }

    public synchronized void setAutoBid(User bidder, String auctionId, double maxBid, double increment) {
        LiveAuction liveAuction = auctionManager.getAuction(auctionId);
        if (liveAuction == null) {
            throw new IllegalArgumentException("Auction not found");
        }

        if (maxBid <= liveAuction.getCurrentHighestBid() || increment <= 0) {
            throw new IllegalArgumentException("Invalid autobid parameters");
        }

        ItemSchema itemSchema = itemService.getItemById(liveAuction.getItemId());
        if (itemSchema == null) {
            throw new IllegalArgumentException("Item not found");
        }
        double depositAmount = itemSchema.getStartingPrice() * 0.10;

        boolean hasDeposited = liveAuction.hasDeposited(bidder.getId());

        if (!hasDeposited) {
            walletService.freezeDeposit(bidder.getId(), depositAmount);
            liveAuction.markDeposited(bidder.getId());
        }

        AutoBidConfig config = new AutoBidConfig(bidder.getId(), bidder.getUsername(), maxBid, increment);
        liveAuction.addAutoBid(config);
    }

    public void cancelAutoBid(User bidder, String auctionId) {
        LiveAuction liveAuction = auctionManager.getAuction(auctionId);
        if (liveAuction == null) {
            throw new IllegalArgumentException("Auction not found");
        }
        liveAuction.removeAutoBid(bidder.getId());
    }
}
