package com.auctionuet.server.domain.service;

import com.auctionuet.protocol.dto.response.bid.AutoBidConfigDTO;
import com.auctionuet.protocol.dto.response.bid.BidDTO;
import com.auctionuet.protocol.enums.AutoBidStatus;
import com.auctionuet.protocol.enums.BidType;
import com.auctionuet.server.domain.manager.AuctionManager;
import com.auctionuet.server.domain.model.AutoBidConfig;
import com.auctionuet.server.domain.model.BidRecord;
import com.auctionuet.server.domain.model.LiveAuction;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.persistence.dao.AuctionDAO;
import com.auctionuet.server.persistence.dao.BidDAO;
import com.auctionuet.server.persistence.schema.AuctionDepositRefSchema;
import com.auctionuet.server.persistence.schema.AuctionSchema;
import com.auctionuet.server.persistence.schema.BidSchema;
import com.auctionuet.server.persistence.schema.ItemSchema;
import com.auctionuet.server.util.IdGenerator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public class BidService {
    private static final double ESCROW_DEPOSIT_RATE = 0.10;
    public static final Duration AUTO_BID_RESPONSE_DELAY = Duration.ofSeconds(3);
    public static final Duration AUTO_BID_LEADER_STABILITY = Duration.ofSeconds(3);

    private final AuctionManager auctionManager;
    private final WalletService walletService;
    private final BidDAO bidDAO;
    private final ItemService itemService;
    private final AuctionDAO auctionDAO;
    private final UserService userService;
    private final Duration autoBidResponseDelay;
    private final Duration autoBidLeaderStability;

    public BidService(AuctionManager auctionManager, WalletService walletService,
            BidDAO bidDAO, ItemService itemService, AuctionDAO auctionDAO, UserService userService) {
        this(
                auctionManager,
                walletService,
                bidDAO,
                itemService,
                auctionDAO,
                userService,
                AUTO_BID_RESPONSE_DELAY,
                AUTO_BID_LEADER_STABILITY);
    }

    public BidService(AuctionManager auctionManager, WalletService walletService,
            BidDAO bidDAO, ItemService itemService, AuctionDAO auctionDAO, UserService userService,
            Duration autoBidResponseDelay, Duration autoBidLeaderStability) {
        this.auctionManager = auctionManager;
        this.walletService = walletService;
        this.bidDAO = bidDAO;
        this.itemService = itemService;
        this.auctionDAO = auctionDAO;
        this.userService = userService;
        this.autoBidResponseDelay = autoBidResponseDelay;
        this.autoBidLeaderStability = autoBidLeaderStability;
    }

    public synchronized BidDTO placeBid(User bidder, String auctionId, double amount) throws Exception {
        LiveAuction liveAuction = requireLiveAuction(auctionId, "Auction not found or not running");
        ItemSchema itemSchema = requireItemSchema(liveAuction.getItemId());
        double depositAmount = calculateDepositAmount(itemSchema);
        DepositHoldResult deposit = ensureDepositFrozen(auctionId, liveAuction, bidder, depositAmount);

        LiveAuction.BidPlacementResult result;
        try {
            result = liveAuction.placeBid(bidder, amount);
        } catch (Exception e) {
            rollbackDepositIfNeeded(auctionId, liveAuction, bidder, depositAmount, deposit);
            throw e;
        }

        syncAuctionState(auctionId, liveAuction);

        BidRecord record = result.manualBid();
        BidSchema bidSchema = toBidSchema(auctionId, record);
        bidDAO.save(bidSchema);
        schedulePendingAutoBidResponse(auctionId, result.pendingAutoBid());

        return toBidDTO(record, auctionId);
    }

    public List<BidSchema> getBidHistory(String auctionId) {
        return bidDAO.findByAuctionId(auctionId);
    }

    public List<BidDTO> getBidHistoryDTO(String auctionId) {
        return getBidHistory(auctionId).stream()
                .map(this::toBidDTO)
                .toList();
    }

    public BidDTO getCurrentHighestBidDTO(AuctionSchema auction) {
        if (auction == null || auction.getWinnerId() == null || auction.getHighestBid() <= 0) {
            return null;
        }

        return getBidHistory(auction.getId()).stream()
                .filter(bid -> auction.getWinnerId().equals(bid.getBidderId()))
                .filter(bid -> Double.compare(auction.getHighestBid(), bid.getAmount()) == 0)
                .max(Comparator.comparing(BidSchema::getTimestamp))
                .map(this::toBidDTO)
                .orElseGet(() -> new BidDTO(
                        auction.getId(),
                        userService.getUserDTOById(auction.getWinnerId()),
                        auction.getHighestBid(),
                        auction.getUpdatedAt() != null ? auction.getUpdatedAt() : LocalDateTime.now(),
                        BidType.MANUAL));
    }

    public synchronized void setAutoBid(User bidder, String auctionId, double maxBid, double increment) {
        LiveAuction liveAuction = requireLiveAuction(auctionId, "Auction not found");
        liveAuction.validateAutoBidCanBeEnabled(bidder.getId(), autoBidLeaderStability);
        validateAutoBidParams(maxBid, increment, liveAuction.getCurrentPrice());
        ItemSchema itemSchema = requireItemSchema(liveAuction.getItemId());
        double depositAmount = calculateDepositAmount(itemSchema);
        DepositHoldResult deposit = ensureDepositFrozen(auctionId, liveAuction, bidder, depositAmount);

        AutoBidConfig config = new AutoBidConfig(bidder.getId(), bidder.getUsername(), maxBid, increment);
        try {
            liveAuction.addAutoBid(config, null, autoBidLeaderStability);
            syncAuctionState(auctionId, liveAuction);
        } catch (RuntimeException e) {
            liveAuction.removeAutoBid(bidder.getId());
            rollbackDepositIfNeeded(auctionId, liveAuction, bidder, depositAmount, deposit);
            throw e;
        }
    }

    public void cancelAutoBid(User bidder, String auctionId) {
        LiveAuction liveAuction = requireLiveAuction(auctionId, "Auction not found");
        boolean canceledPendingResponse = liveAuction.removeAutoBid(bidder.getId());
        if (canceledPendingResponse) {
            auctionManager.cancelAutoBidResponse(auctionId);
        }
    }

    public synchronized void resolvePendingAutoBid(String auctionId, long sequenceId) {
        LiveAuction liveAuction = auctionManager.getAuction(auctionId);
        if (liveAuction == null) {
            return;
        }

        LiveAuction.AutoBidResolution resolution = liveAuction.resolvePendingAutoBid(
                sequenceId,
                autoRecord -> saveAutoBidRecord(auctionId, autoRecord));
        if (resolution.autoBid() != null) {
            syncAuctionState(auctionId, liveAuction);
        }
    }

    public AutoBidConfigDTO getAutoBidConfigDTO(User bidder, String auctionId) {
        LiveAuction liveAuction = auctionManager.getAuction(auctionId);
        if (liveAuction == null) {
            return null;
        }

        LiveAuction.AutoBidStateSnapshot snapshot = liveAuction.getAutoBidStateSnapshot(bidder.getId());
        if (!snapshot.hasConfig()) {
            return null;
        }

        AutoBidStatus status;
        double protectedUntil = 0.0;
        if (!snapshot.active()) {
            status = AutoBidStatus.INEFFECTIVE;
        } else if (bidder.getId().equals(snapshot.currentWinnerId()) && !snapshot.pendingAgainstCurrentLeader()) {
            status = AutoBidStatus.PROTECTING;
            protectedUntil = snapshot.maxBid();
        } else {
            status = AutoBidStatus.WAITING;
            protectedUntil = snapshot.maxBid();
        }

        return new AutoBidConfigDTO(
                auctionId,
                userService.toDTO(bidder),
                snapshot.maxBid(),
                snapshot.increment(),
                status,
                protectedUntil);
    }

    public BidDTO toBidDTO(BidRecord record, String auctionId) {
        return new BidDTO(
                auctionId,
                userService.getUserDTOById(record.getBidderId()),
                record.getAmount(),
                record.getTimestamp(),
                record.getBidType());
    }

    public BidDTO toBidDTO(BidSchema schema) {
        return new BidDTO(
                schema.getAuctionId(),
                userService.getUserDTOById(schema.getBidderId()),
                schema.getAmount(),
                schema.getTimestamp(),
                schema.getBidType());
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

    private DepositHoldResult ensureDepositFrozen(String auctionId, LiveAuction liveAuction, User bidder, double depositAmount) {
        AuctionSchema auctionSchema = auctionDAO.findById(auctionId);
        String bidderId = bidder.getId();

        if (auctionSchema != null && auctionSchema.hasDepositedBidder(bidderId)) {
            if (auctionSchema.getDepositRef(bidderId) == null) {
                persistDepositParticipation(auctionSchema, bidder, depositAmount, null);
            }
            liveAuction.markDeposited(bidderId);
            return DepositHoldResult.existing();
        }

        if (liveAuction.hasDeposited(bidderId)) {
            persistDepositParticipation(auctionSchema, bidder, depositAmount, null);
            return DepositHoldResult.existing();
        }

        String transactionId = walletService.holdAuctionDeposit(
                bidderId,
                depositAmount,
                auctionId,
                "Giu tien coc phien dau gia " + auctionId);
        liveAuction.markDeposited(bidderId);
        persistDepositParticipation(auctionSchema, bidder, depositAmount, transactionId);
        return DepositHoldResult.held(transactionId);
    }

    private void persistDepositParticipation(
            AuctionSchema auctionSchema,
            User bidder,
            double depositAmount,
            String transactionId) {
        if (auctionSchema != null && !auctionSchema.hasDepositedBidder(bidder.getId())) {
            String effectiveTransactionId = transactionId != null
                    ? transactionId
                    : walletService.recordLegacyAuctionDeposit(
                            bidder.getId(),
                            depositAmount,
                            auctionSchema.getId(),
                            "Khoi phuc coc tu runtime khi dat gia");
            auctionSchema.addDepositRef(new AuctionDepositRefSchema(
                    bidder.getId(),
                    bidder.getUsername(),
                    depositAmount,
                    effectiveTransactionId,
                    LocalDateTime.now()));
            auctionDAO.update(auctionSchema);
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

    private void rollbackDepositIfNeeded(
            String auctionId,
            LiveAuction liveAuction,
            User bidder,
            double depositAmount,
            DepositHoldResult deposit) {
        if (deposit != null && deposit.heldNow()) {
            walletService.refundAuctionDeposit(
                    bidder.getId(),
                    depositAmount,
                    auctionId,
                    deposit.transactionId(),
                    "Hoan tien coc do dat gia khong hop le");
            liveAuction.unmarkDeposited(bidder.getId());
            removeDepositParticipation(auctionId, bidder.getId());
        }
    }

    private void removeDepositParticipation(String auctionId, String bidderId) {
        AuctionSchema auctionSchema = auctionDAO.findById(auctionId);
        if (auctionSchema != null && auctionSchema.removeDepositedBidder(bidderId)) {
            auctionDAO.update(auctionSchema);
        }
    }

    private void saveAutoBidRecord(String auctionId, BidRecord autoRecord) {
        BidSchema autoBidSchema = toBidSchema(auctionId, autoRecord);
        bidDAO.save(autoBidSchema);
    }

    private void schedulePendingAutoBidResponse(String auctionId, LiveAuction.PendingAutoBid pendingAutoBid) {
        if (pendingAutoBid == null) {
            auctionManager.cancelAutoBidResponse(auctionId);
            return;
        }

        long delayMs = Math.max(0, autoBidResponseDelay.toMillis());
        long sequenceId = pendingAutoBid.sequenceId();
        auctionManager.scheduleAutoBidResponse(
                auctionId,
                delayMs,
                () -> resolvePendingAutoBid(auctionId, sequenceId));
    }

    private BidSchema toBidSchema(String auctionId, BidRecord record) {
        LocalDateTime now = LocalDateTime.now();
        return new BidSchema(
                IdGenerator.generate(),
                now,
                now,
                auctionId,
                record.getBidderId(),
                record.getAmount(),
                record.getTimestamp(),
                record.getBidType());
    }

    private record DepositHoldResult(boolean heldNow, String transactionId) {
        static DepositHoldResult existing() {
            return new DepositHoldResult(false, null);
        }

        static DepositHoldResult held(String transactionId) {
            return new DepositHoldResult(true, transactionId);
        }
    }
}
