package com.auctionuet.server.domain.service;

import com.auctionuet.protocol.dto.response.auction.AuctionDTO;
import com.auctionuet.protocol.dto.response.bid.BidDTO;
import com.auctionuet.protocol.dto.response.item.ItemDTO;
import com.auctionuet.protocol.dto.response.user.UserDTO;
import com.auctionuet.protocol.enums.AuctionStatus;
import com.auctionuet.server.domain.manager.AuctionManager;
import com.auctionuet.server.domain.model.LiveAuction;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.exception.AuctionException;
import com.auctionuet.server.persistence.dao.AuctionDAO;
import com.auctionuet.server.persistence.schema.AuctionDepositRefSchema;
import com.auctionuet.server.persistence.schema.AuctionSchema;
import com.auctionuet.server.persistence.schema.BidSchema;
import com.auctionuet.server.persistence.schema.ItemSchema;
import com.auctionuet.server.persistence.schema.UserSchema;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AuctionService {

    private static final int CREATE_AUCTION_START_GRACE_SECONDS = 10;

    private final ItemService itemService;
    private final BidService bidService;
    private final UserService userService;
    private final AuctionDAO auctionDAO;
    private final AuctionManager auctionManager;
    private final WalletService walletService;

    public AuctionService(ItemService itemService, BidService bidService, UserService userService,
            AuctionDAO auctionDAO, WalletService walletService) {
        this.itemService = itemService;
        this.bidService = bidService;
        this.userService = userService;
        this.auctionDAO = auctionDAO;
        this.walletService = walletService;
        this.auctionManager = AuctionManager.getInstance();
        this.auctionManager.setStartAuctionCallback(this::onAuctionStartDue);
        this.auctionManager.setEndAuctionCallback(this::endAuction);
        this.auctionManager.setPaymentDeadlineCallback(this::expirePaymentDeadline);
        this.auctionManager.setReconcileCallback(this::reconcileAuctionsFromDatabase);
    }

    public synchronized AuctionDTO createAuction(User seller, String itemId, LocalDateTime startTime,
            LocalDateTime endTime, String title, String description,
            int antiSnipingWindowSeconds, int antiSnipingExtensionSeconds) throws AuctionException {
        requireCreateAuctionPermission(seller);
        ItemSchema item = requireItem(itemId);
        if (item.isArchived()) {
            throw new AuctionException("Item da bi go khoi danh sach dau gia");
        }
        requireItemOwner(seller, item);
        validateAuctionTimeRange(startTime, endTime);
        startTime = normalizeCreateAuctionStartTime(startTime);
        requireItemAvailableForNewAuction(itemId);

        AuctionSchema schema = new AuctionSchema(
                UUID.randomUUID().toString(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                itemId,
                seller.getId(),
                title,
                description,
                startTime,
                endTime,
                AuctionStatus.OPEN,
                0,
                null,
                antiSnipingWindowSeconds,
                antiSnipingExtensionSeconds);

        auctionDAO.save(schema);
        auctionManager.scheduleAuctionStart(schema.getId(), schema.getStartTime());

        item.setAuctionCount(item.getAuctionCount() + 1);
        itemService.updateItem(item);

        ItemDTO itemDTO = itemService.getItemById(itemId);
        return toAuctionDTO(schema, itemDTO);
    }

    public synchronized void startAuction(User seller, String auctionId) throws AuctionException {
        AuctionSchema schema = requireAuction(auctionId);
        requireAuctionOwner(seller, schema);
        requireAuctionStatus(schema, AuctionStatus.OPEN, "Auction hien khong o trang thai OPEN");
        startAuctionNow(schema);
    }

    public void loadRunningAuctions() {
        reconcileAuctionsFromDatabase();
    }

    public synchronized void reconcileAuctionsFromDatabase() {
        LocalDateTime now = LocalDateTime.now();
        reconcileDuplicateActiveAuctions();
        for (AuctionSchema schema : auctionDAO.findAll()) {
            if (schema.getStatus() == AuctionStatus.PAID || schema.getStatus() == AuctionStatus.CANCELED) {
                auctionManager.cancelAllTasks(schema.getId());
                auctionManager.removeLiveAuction(schema.getId());
                continue;
            }

            if (schema.getStatus() == AuctionStatus.WAITING_PAYMENT) {
                reconcilePaymentDeadline(schema);
                continue;
            }

            if (schema.getStatus() == AuctionStatus.OPEN) {
                if (hasInvalidAuctionTime(schema) || !schema.getEndTime().isAfter(now)) {
                    cancelStaleAuction(schema, "Huy phien dau gia qua han khi dong bo du lieu");
                    continue;
                }
                if (!schema.getStartTime().isAfter(now)) {
                    startAuctionNow(schema);
                } else {
                    auctionManager.scheduleAuctionStart(schema.getId(), schema.getStartTime());
                }
                continue;
            }

            if (schema.getStatus() == AuctionStatus.RUNNING) {
                if (hasInvalidAuctionTime(schema)) {
                    cancelStaleAuction(schema, "Huy phien dau gia co thoi gian khong hop le");
                } else if (!schema.getEndTime().isAfter(now)) {
                    endAuction(schema.getId());
                } else {
                    hydrateRunningAuction(schema);
                }
                continue;
            }

            cancelStaleAuction(schema, "Huy phien dau gia co trang thai khong hop le");
        }
    }

    public synchronized void onAuctionStartDue(String auctionId) {
        AuctionSchema schema = auctionDAO.findById(auctionId);
        if (schema == null || schema.getStatus() != AuctionStatus.OPEN) {
            return;
        }
        if (hasInvalidAuctionTime(schema) || !schema.getEndTime().isAfter(LocalDateTime.now())) {
            cancelStaleAuction(schema, "Huy phien dau gia qua han khi den lich bat dau");
            return;
        }
        startAuctionNow(schema);
    }

    public synchronized void endAuction(String auctionId) throws AuctionException {
        AuctionSchema schema = auctionDAO.findById(auctionId);
        if (schema == null) {
            auctionManager.removeLiveAuction(auctionId);
            return;
        }
        if (schema.getStatus() == AuctionStatus.PAID || schema.getStatus() == AuctionStatus.CANCELED) {
            auctionManager.removeLiveAuction(auctionId);
            return;
        }
        if (schema.getStatus() == AuctionStatus.WAITING_PAYMENT) {
            auctionManager.removeLiveAuction(auctionId);
            reconcilePaymentDeadline(schema);
            return;
        }

        LiveAuction liveAuction = auctionManager.getAuction(auctionId);
        ItemSchema item = itemService.getItemSchemaById(schema.getItemId());
        double depositAmount = item != null ? calculateDepositAmount(item) : 0;

        if (schema.getWinnerId() != null) {
            refundDepositsExcept(schema, liveAuction, schema.getWinnerId(), depositAmount,
                    "Hoan tien coc cho nguoi khong thang dau gia");
            schema.setStatus(AuctionStatus.WAITING_PAYMENT);
            schema.setPaymentDeadlineAt(LocalDateTime.now().plusMinutes(AuctionManager.PAYMENT_DEADLINE_MINUTES));
            schema.setPaidAt(null);
            auctionDAO.update(schema);
            auctionManager.schedulePaymentDeadline(schema.getId(), schema.getPaymentDeadlineAt());
        } else {
            refundDepositsExcept(schema, liveAuction, null, depositAmount,
                    "Hoan tien coc do phien dau gia khong co nguoi thang");
            schema.setStatus(AuctionStatus.CANCELED);
            schema.setPaymentDeadlineAt(null);
            schema.setPaidAt(null);
            auctionDAO.update(schema);
        }
        auctionManager.endAuction(auctionId);
    }

    public synchronized void expirePaymentDeadline(String auctionId) {
        AuctionSchema schema = auctionDAO.findById(auctionId);
        if (schema == null || schema.getStatus() != AuctionStatus.WAITING_PAYMENT) {
            auctionManager.cancelPaymentDeadline(auctionId);
            return;
        }

        ItemSchema item = itemService.getItemSchemaById(schema.getItemId());
        double depositAmount = item != null ? calculateDepositAmount(item) : 0;
        AuctionDepositRefSchema winnerDeposit = schema.getDepositRef(schema.getWinnerId());
        double amount = amountOfDeposit(winnerDeposit, depositAmount);
        String relatedTransactionId = winnerDeposit != null ? winnerDeposit.getDepositTransactionId() : null;

        if (schema.getWinnerId() != null) {
            walletService.forfeitAuctionDeposit(
                    schema.getWinnerId(),
                    amount,
                    auctionId,
                    relatedTransactionId,
                    "Tich thu tien coc do qua han thanh toan");
            clearDepositParticipation(schema, auctionManager.getAuction(auctionId), schema.getWinnerId());
        }

        schema.setStatus(AuctionStatus.CANCELED);
        schema.setPaymentDeadlineAt(null);
        schema.setPaidAt(null);
        auctionDAO.update(schema);
        auctionManager.cancelPaymentDeadline(auctionId);
        auctionManager.removeLiveAuction(auctionId);
    }

    public synchronized void payAuction(User winner, String auctionId) throws AuctionException {
        AuctionSchema schema = requireAuction(auctionId);
        requireAuctionStatus(schema, AuctionStatus.WAITING_PAYMENT, "Auction khong o trang thai cho thanh toan");
        requireWinner(winner, schema);
        if (schema.getPaymentDeadlineAt() != null && !schema.getPaymentDeadlineAt().isAfter(LocalDateTime.now())) {
            expirePaymentDeadline(auctionId);
            throw new AuctionException("Da qua han thanh toan");
        }
        ItemSchema itemSchema = requireItem(schema.getItemId());

        double totalPrice = schema.getHighestBid();
        double depositAmount = calculateDepositAmount(itemSchema);
        AuctionDepositRefSchema winnerDeposit = schema.getDepositRef(winner.getId());
        double retainedDepositAmount = amountOfDeposit(winnerDeposit, depositAmount);
        double remaining = totalPrice - retainedDepositAmount;
        String relatedTransactionId = winnerDeposit != null ? winnerDeposit.getDepositTransactionId() : null;

        try {
            if (remaining > 0) {
                walletService.payAuctionRemaining(winner.getId(), remaining, auctionId,
                        "Thanh toan phan con lai cua phien dau gia");
            }
            walletService.forfeitAuctionDeposit(winner.getId(), retainedDepositAmount, auctionId, relatedTransactionId,
                    "Ap tien coc vao thanh toan phien dau gia");
            walletService.payoutSeller(schema.getSellerId(), totalPrice, auctionId,
                    "Nhan tien tu nguoi thang dau gia");
            itemService.transferOwnership(schema.getItemId(), winner.getId());
            clearDepositParticipation(schema, null, winner.getId());

            schema.setStatus(AuctionStatus.PAID);
            schema.setPaymentDeadlineAt(null);
            schema.setPaidAt(LocalDateTime.now());
            auctionDAO.update(schema);
            auctionManager.cancelPaymentDeadline(auctionId);
        } catch (IllegalArgumentException e) {
            throw new AuctionException("Thanh toan that bai: " + e.getMessage());
        }
    }

    public List<AuctionDTO> getAuctions() {
        return toAuctionDTOList(auctionDAO.findAll());
    }

    public List<AuctionDTO> getAuctions(String currentUserId) {
        return toAuctionDTOList(auctionDAO.findAll(), currentUserId);
    }

    public List<AuctionDTO> getAuctionsByStatus(AuctionStatus status) {
        return toAuctionDTOList(auctionDAO.findByStatus(status));
    }

    public List<AuctionDTO> getPendingPaymentsForWinner(String winnerId) {
        return auctionDAO.findByStatus(AuctionStatus.WAITING_PAYMENT).stream()
                .filter(schema -> winnerId.equals(schema.getWinnerId()))
                .map(schema -> toAuctionDTO(schema, winnerId))
                .filter(dto -> dto != null)
                .toList();
    }

    public AuctionDTO getAuctionById(String id) {
        return getAuctionById(id, null);
    }

    public AuctionDTO getAuctionById(String id, String currentUserId) {
        AuctionSchema schema = auctionDAO.findById(id);
        if (schema == null) {
            throw new AuctionException("Auction khong ton tai");
        }
        return toAuctionDTO(schema, currentUserId);
    }

    public List<AuctionDTO> getAuctionsBySeller(String sellerId) {
        return toAuctionDTOList(auctionDAO.findBySellerId(sellerId));
    }

    public List<AuctionDTO> getItemAuctionHistory(User user, String itemId) throws AuctionException {
        ItemSchema item = requireItem(itemId);
        if (!user.getId().equals(item.getSellerId())) {
            throw new AuctionException("Ban khong phai chu so huu cua item nay");
        }
        return toAuctionDTOList(auctionDAO.findByItemId(itemId));
    }

    private synchronized void startAuctionNow(AuctionSchema schema) {
        ItemSchema item = itemService.getItemSchemaById(schema.getItemId());
        if (item == null) {
            cancelStaleAuction(schema, "Huy phien dau gia do item khong ton tai");
            return;
        }

        AuctionSchema activeKeeper = selectActiveKeeperForItem(schema.getItemId());
        if (activeKeeper != null && !schema.getId().equals(activeKeeper.getId())) {
            cancelStaleAuction(schema, "Hủy phiên đấu giá trùng vật phẩm");
            return;
        }
        cancelCompetingActiveAuctions(schema);

        auctionManager.cancelAuctionStart(schema.getId());
        schema.setStatus(AuctionStatus.RUNNING);
        schema.setPaymentDeadlineAt(null);
        schema.setPaidAt(null);
        auctionDAO.update(schema);
        auctionManager.loadAuction(schema, item.getStartingPrice(), resolveCurrentLeaderSince(schema));
        auctionManager.markAuctionStarted(schema);
    }

    private void hydrateRunningAuction(AuctionSchema schema) {
        ItemSchema item = itemService.getItemSchemaById(schema.getItemId());
        if (item == null) {
            cancelStaleAuction(schema, "Huy phien dau gia do item khong ton tai");
            return;
        }

        double depositAmount = calculateDepositAmount(item);
        if (backfillDepositsFromBidHistory(schema, depositAmount)) {
            auctionDAO.update(schema);
        }

        LiveAuction liveAuction = auctionManager.getAuction(schema.getId());
        if (liveAuction != null) {
            liveAuction.markDeposited(schema.getDepositedBidderIds());
            LocalDateTime endTime = laterOf(liveAuction.getEndTime(), schema.getEndTime());
            liveAuction.setEndTime(endTime);
            auctionManager.scheduleAuctionEnd(schema.getId(), endTime);
            return;
        }

        auctionManager.loadAuction(schema, item.getStartingPrice(), resolveCurrentLeaderSince(schema));
    }

    private LocalDateTime laterOf(LocalDateTime first, LocalDateTime second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.isAfter(second) ? first : second;
    }

    private LocalDateTime resolveCurrentLeaderSince(AuctionSchema schema) {
        if (schema.getWinnerId() != null) {
            return bidService.getBidHistory(schema.getId()).stream()
                    .filter(bid -> schema.getWinnerId().equals(bid.getBidderId()))
                    .filter(bid -> Double.compare(schema.getHighestBid(), bid.getAmount()) == 0)
                    .max(Comparator.comparing(BidSchema::getTimestamp))
                    .map(BidSchema::getTimestamp)
                    .orElseGet(() -> fallbackLeaderSince(schema));
        }
        return fallbackLeaderSince(schema);
    }

    private LocalDateTime fallbackLeaderSince(AuctionSchema schema) {
        return schema.getUpdatedAt() != null ? schema.getUpdatedAt() : LocalDateTime.now();
    }

    private void reconcilePaymentDeadline(AuctionSchema schema) {
        LocalDateTime deadline = schema.getPaymentDeadlineAt();
        if (deadline == null) {
            LocalDateTime base = schema.getUpdatedAt() != null ? schema.getUpdatedAt() : LocalDateTime.now();
            deadline = base.plusMinutes(AuctionManager.PAYMENT_DEADLINE_MINUTES);
            schema.setPaymentDeadlineAt(deadline);
            auctionDAO.update(schema);
        }

        if (!deadline.isAfter(LocalDateTime.now())) {
            expirePaymentDeadline(schema.getId());
        } else {
            auctionManager.schedulePaymentDeadline(schema.getId(), deadline);
        }
    }

    private void cancelStaleAuction(AuctionSchema schema, String description) {
        if (schema == null) {
            return;
        }
        LiveAuction liveAuction = auctionManager.getAuction(schema.getId());
        ItemSchema item = itemService.getItemSchemaById(schema.getItemId());
        double depositAmount = item != null ? calculateDepositAmount(item) : 0;
        refundDepositsExcept(schema, liveAuction, null, depositAmount, description);
        schema.setStatus(AuctionStatus.CANCELED);
        schema.setPaymentDeadlineAt(null);
        schema.setPaidAt(null);
        auctionDAO.update(schema);
        auctionManager.cancelAllTasks(schema.getId());
        auctionManager.removeLiveAuction(schema.getId());
    }

    private void requireItemAvailableForNewAuction(String itemId) throws AuctionException {
        AuctionSchema blockingAuction = findBlockingAuctionForItem(itemId);
        if (blockingAuction != null) {
            throw new AuctionException("Vật phẩm này đã có phiên đấu giá đang hoạt động");
        }
    }

    private AuctionSchema findBlockingAuctionForItem(String itemId) {
        for (AuctionSchema auction : auctionDAO.findByItemId(itemId)) {
            if (isBlockingAuctionStatus(auction.getStatus())) {
                return auction;
            }
        }
        return null;
    }

    private void reconcileDuplicateActiveAuctions() {
        Map<String, List<AuctionSchema>> auctionsByItemId = new LinkedHashMap<>();
        for (AuctionSchema auction : auctionDAO.findAll()) {
            if (auction.getItemId() == null || !isBlockingAuctionStatus(auction.getStatus())) {
                continue;
            }
            auctionsByItemId.computeIfAbsent(auction.getItemId(), key -> new ArrayList<>()).add(auction);
        }

        for (List<AuctionSchema> itemAuctions : auctionsByItemId.values()) {
            if (itemAuctions.size() <= 1) {
                continue;
            }

            AuctionSchema keeper = selectActiveKeeper(itemAuctions);
            for (AuctionSchema auction : itemAuctions) {
                if (!auction.getId().equals(keeper.getId())) {
                    cancelStaleAuction(auction, "Hủy phiên đấu giá trùng vật phẩm khi đồng bộ dữ liệu");
                }
            }
        }
    }

    private void cancelCompetingActiveAuctions(AuctionSchema keeper) {
        for (AuctionSchema auction : auctionDAO.findByItemId(keeper.getItemId())) {
            if (!auction.getId().equals(keeper.getId()) && isBlockingAuctionStatus(auction.getStatus())) {
                cancelStaleAuction(auction, "Hủy phiên đấu giá trùng vật phẩm");
            }
        }
    }

    private AuctionSchema selectActiveKeeperForItem(String itemId) {
        List<AuctionSchema> activeAuctions = auctionDAO.findByItemId(itemId).stream()
                .filter(auction -> isBlockingAuctionStatus(auction.getStatus()))
                .toList();
        return selectActiveKeeper(activeAuctions);
    }

    private AuctionSchema selectActiveKeeper(List<AuctionSchema> activeAuctions) {
        AuctionSchema selected = null;
        for (AuctionSchema auction : activeAuctions) {
            if (selected == null || isBetterActiveAuction(auction, selected)) {
                selected = auction;
            }
        }
        return selected;
    }

    private boolean isBetterActiveAuction(AuctionSchema candidate, AuctionSchema current) {
        int candidateRank = activeStatusRank(candidate.getStatus());
        int currentRank = activeStatusRank(current.getStatus());
        if (candidateRank != currentRank) {
            return candidateRank > currentRank;
        }
        return schemaTieBreakTime(candidate).isAfter(schemaTieBreakTime(current));
    }

    private int activeStatusRank(AuctionStatus status) {
        if (status == AuctionStatus.WAITING_PAYMENT) {
            return 3;
        }
        if (status == AuctionStatus.RUNNING) {
            return 2;
        }
        if (status == AuctionStatus.OPEN) {
            return 1;
        }
        return 0;
    }

    private LocalDateTime schemaTieBreakTime(AuctionSchema schema) {
        if (schema.getCreatedAt() != null) {
            return schema.getCreatedAt();
        }
        if (schema.getStartTime() != null) {
            return schema.getStartTime();
        }
        if (schema.getUpdatedAt() != null) {
            return schema.getUpdatedAt();
        }
        return LocalDateTime.MIN;
    }

    private boolean isBlockingAuctionStatus(AuctionStatus status) {
        return status == AuctionStatus.OPEN
                || status == AuctionStatus.RUNNING
                || status == AuctionStatus.WAITING_PAYMENT;
    }

    private List<AuctionDTO> toAuctionDTOList(List<AuctionSchema> schemas) {
        return toAuctionDTOList(schemas, null);
    }

    private List<AuctionDTO> toAuctionDTOList(List<AuctionSchema> schemas, String currentUserId) {
        List<AuctionDTO> auctionDTOList = new ArrayList<>();
        for (AuctionSchema schema : schemas) {
            AuctionDTO dto = toAuctionDTO(schema, currentUserId);
            if (dto != null) {
                auctionDTOList.add(dto);
            }
        }
        return auctionDTOList;
    }

    private AuctionDTO toAuctionDTO(AuctionSchema schema) {
        return toAuctionDTO(schema, (String) null);
    }

    private AuctionDTO toAuctionDTO(AuctionSchema schema, String currentUserId) {
        ItemDTO itemDTO = itemService.getItemById(schema.getItemId());
        if (itemDTO == null) {
            return null;
        }
        return toAuctionDTO(schema, itemDTO, currentUserId);
    }

    private AuctionDTO toAuctionDTO(AuctionSchema schema, ItemDTO itemDTO) {
        return toAuctionDTO(schema, itemDTO, null);
    }

    private AuctionDTO toAuctionDTO(AuctionSchema schema, ItemDTO itemDTO, String currentUserId) {
        UserDTO seller = userService.getUserDTOById(schema.getSellerId());
        UserDTO winner = schema.getWinnerId() != null ? userService.getUserDTOById(schema.getWinnerId()) : null;
        BidDTO currentHighestBid = bidService.getCurrentHighestBidDTO(schema);
        double currentPrice = currentHighestBid != null ? currentHighestBid.getAmount() : itemDTO.getStartingPrice();
        double depositAmount = itemDTO.getStartingPrice() * 0.10;
        boolean currentUserDeposited = hasAuctionDeposit(schema, currentUserId);

        return new AuctionDTO(
                schema.getId(),
                itemDTO,
                seller,
                winner,
                currentHighestBid,
                currentPrice,
                schema.getTitle(),
                schema.getDescription(),
                schema.getStartTime(),
                schema.getEndTime(),
                schema.getPaymentDeadlineAt(),
                paidAtOf(schema),
                schema.getStatus(),
                schema.getAntiSnipingWindowSeconds(),
                schema.getAntiSnipingExtensionSeconds(),
                depositAmount,
                currentUserDeposited);
    }

    private LocalDateTime paidAtOf(AuctionSchema schema) {
        if (schema.getPaidAt() != null) {
            return schema.getPaidAt();
        }
        if (schema.getStatus() == AuctionStatus.PAID) {
            return schema.getUpdatedAt();
        }
        return null;
    }

    private boolean hasAuctionDeposit(AuctionSchema schema, String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }

        if (schema.hasDepositedBidder(userId)) {
            if (schema.getDepositRef(userId) == null) {
                persistDepositParticipation(schema, auctionManager.getAuction(schema.getId()), userId);
            }
            return true;
        }

        LiveAuction liveAuction = auctionManager.getAuction(schema.getId());
        if (liveAuction != null && liveAuction.hasDeposited(userId)) {
            persistDepositParticipation(schema, liveAuction, userId);
            return true;
        }

        if (schema.getStatus() == AuctionStatus.WAITING_PAYMENT && userId.equals(schema.getWinnerId())) {
            return true;
        }

        if (canBackfillDepositForBidder(schema, userId)) {
            persistDepositParticipation(schema, liveAuction, userId);
            return true;
        }

        return false;
    }

    private void refundDepositsExcept(
            AuctionSchema schema,
            LiveAuction liveAuction,
            String retainedBidderId,
            double defaultDepositAmount,
            String description) {
        if (schema == null) {
            return;
        }

        Set<String> handledBidderIds = new HashSet<>();
        for (AuctionDepositRefSchema ref : schema.getDepositRefs()) {
            String bidderId = ref.getBidderId();
            if (retainedBidderId != null && retainedBidderId.equals(bidderId)) {
                continue;
            }
            double amount = amountOfDeposit(ref, defaultDepositAmount);
            walletService.refundAuctionDeposit(
                    bidderId,
                    amount,
                    schema.getId(),
                    ref.getDepositTransactionId(),
                    description);
            clearDepositParticipation(schema, liveAuction, bidderId);
            handledBidderIds.add(bidderId);
        }

        for (String bidderId : schema.getLegacyDepositedBidderIds()) {
            if (handledBidderIds.contains(bidderId)
                    || (retainedBidderId != null && retainedBidderId.equals(bidderId))) {
                continue;
            }
            walletService.refundAuctionDeposit(bidderId, defaultDepositAmount, schema.getId(), null, description);
            clearDepositParticipation(schema, liveAuction, bidderId);
        }
    }

    private void clearDepositParticipation(AuctionSchema schema, LiveAuction liveAuction, String bidderId) {
        if (schema != null) {
            schema.removeDepositedBidder(bidderId);
        }
        if (liveAuction != null) {
            liveAuction.unmarkDeposited(bidderId);
        }
    }

    private void persistDepositParticipation(AuctionSchema schema, LiveAuction liveAuction, String bidderId) {
        if (schema != null && schema.getDepositRef(bidderId) == null) {
            ItemSchema item = itemService.getItemSchemaById(schema.getItemId());
            double amount = item != null ? calculateDepositAmount(item) : 0;
            if (amount > 0) {
                String transactionId = walletService.recordLegacyAuctionDeposit(
                        bidderId,
                        amount,
                        schema.getId(),
                        "Khoi phuc coc tu du lieu runtime hoac bid history");
                schema.addDepositRef(createDepositRef(bidderId, amount, transactionId));
                auctionDAO.update(schema);
            }
        }
        if (liveAuction != null) {
            liveAuction.markDeposited(bidderId);
        }
    }

    private boolean canBackfillDepositForBidder(AuctionSchema schema, String bidderId) {
        if (schema.getStatus() != AuctionStatus.RUNNING) {
            return false;
        }

        boolean hasBidHistory = bidService.getBidHistory(schema.getId()).stream()
                .anyMatch(bid -> bidderId.equals(bid.getBidderId()));
        if (!hasBidHistory) {
            return false;
        }

        ItemSchema item = itemService.getItemSchemaById(schema.getItemId());
        if (item == null) {
            return false;
        }

        try {
            UserSchema bidder = userService.getUserSchemaById(bidderId);
            return bidder.getFrozenBalance() >= calculateDepositAmount(item);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean backfillDepositsFromBidHistory(AuctionSchema schema, double depositAmount) {
        if (schema == null || depositAmount <= 0) {
            return false;
        }

        boolean changed = false;
        Set<String> bidderIds = new HashSet<>();
        for (BidSchema bid : bidService.getBidHistory(schema.getId())) {
            bidderIds.add(bid.getBidderId());
        }

        for (String bidderId : bidderIds) {
            if (schema.getDepositRef(bidderId) != null) {
                continue;
            }

            try {
                UserSchema bidder = userService.getUserSchemaById(bidderId);
                if (bidder.getFrozenBalance() >= depositAmount) {
                    String transactionId = walletService.recordLegacyAuctionDeposit(
                            bidderId,
                            depositAmount,
                            schema.getId(),
                            "Khoi phuc coc tu bid history khi load server");
                    changed |= schema.addDepositRef(createDepositRef(bidderId, depositAmount, transactionId));
                }
            } catch (RuntimeException e) {
                // Bo qua bid history cu khong con user hop le.
            }
        }
        return changed;
    }

    private AuctionDepositRefSchema createDepositRef(String bidderId, double amount, String transactionId) {
        UserSchema bidder = userService.getUserSchemaById(bidderId);
        return new AuctionDepositRefSchema(
                bidderId,
                bidder.getUsername(),
                amount,
                transactionId,
                LocalDateTime.now());
    }

    private boolean hasInvalidAuctionTime(AuctionSchema schema) {
        return schema.getStartTime() == null
                || schema.getEndTime() == null
                || !schema.getEndTime().isAfter(schema.getStartTime());
    }

    private double amountOfDeposit(AuctionDepositRefSchema ref, double defaultAmount) {
        if (ref != null && ref.getAmount() > 0) {
            return ref.getAmount();
        }
        return defaultAmount;
    }

    private double calculateDepositAmount(ItemSchema itemSchema) {
        return itemSchema.getStartingPrice() * 0.10;
    }

    private void requireCreateAuctionPermission(User seller) throws AuctionException {
        if (!seller.hasPermission(com.auctionuet.protocol.enums.Permission.CREATE_AUCTION)) {
            throw new AuctionException("Khong co quyen CREATE_AUCTION");
        }
    }

    private ItemSchema requireItem(String itemId) throws AuctionException {
        ItemSchema item = itemService.getItemSchemaById(itemId);
        if (item == null) {
            throw new AuctionException("Item khong ton tai");
        }
        return item;
    }

    private AuctionSchema requireAuction(String auctionId) throws AuctionException {
        AuctionSchema schema = auctionDAO.findById(auctionId);
        if (schema == null) {
            throw new AuctionException("Auction khong ton tai");
        }
        return schema;
    }

    private void requireItemOwner(User seller, ItemSchema item) throws AuctionException {
        if (!seller.getId().equals(item.getSellerId())) {
            throw new AuctionException("Ban khong phai chu so huu cua item nay");
        }
    }

    private void requireAuctionOwner(User seller, AuctionSchema auction) throws AuctionException {
        if (!auction.getSellerId().equals(seller.getId())) {
            throw new AuctionException("Ban khong phai chu so huu cua auction nay");
        }
    }

    private void requireWinner(User winner, AuctionSchema auction) throws AuctionException {
        if (!winner.getId().equals(auction.getWinnerId())) {
            throw new AuctionException("Ban khong phai nguoi chien thang");
        }
    }

    private void requireAuctionStatus(
            AuctionSchema auction,
            AuctionStatus expectedStatus,
            String message) throws AuctionException {
        if (auction.getStatus() != expectedStatus) {
            throw new AuctionException(message);
        }
    }

    private void validateAuctionTimeRange(LocalDateTime startTime, LocalDateTime endTime) throws AuctionException {
        if (!endTime.isAfter(startTime)) {
            throw new AuctionException("Thoi gian ket thuc phai sau thoi gian bat dau");
        }
    }

    private LocalDateTime normalizeCreateAuctionStartTime(LocalDateTime startTime) throws AuctionException {
        LocalDateTime now = LocalDateTime.now();
        if (startTime.isBefore(now.minusSeconds(CREATE_AUCTION_START_GRACE_SECONDS))) {
            throw new AuctionException("Thoi gian bat dau phai o trong tuong lai");
        }
        return startTime.isBefore(now) ? now : startTime;
    }
}
