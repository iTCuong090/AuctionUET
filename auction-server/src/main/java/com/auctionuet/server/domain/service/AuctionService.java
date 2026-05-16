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
import com.auctionuet.server.persistence.schema.AuctionSchema;
import com.auctionuet.server.persistence.schema.BidSchema;
import com.auctionuet.server.persistence.schema.ItemSchema;
import com.auctionuet.server.persistence.schema.UserSchema;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionService {

    private final ItemService itemService;
    private final BidService bidService;
    private final UserService userService;
    private final AuctionDAO auctionDAO;
    private final AuctionManager auctionManager;
    private final WalletService walletService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public AuctionService(ItemService itemService, BidService bidService, UserService userService,
            AuctionDAO auctionDAO, WalletService walletService) {
        this.itemService = itemService;
        this.bidService = bidService;
        this.userService = userService;
        this.auctionDAO = auctionDAO;
        this.walletService = walletService;
        this.auctionManager = AuctionManager.getInstance();
        this.auctionManager.setEndAuctionCallback(this::endAuction);
    }

    public AuctionDTO createAuction(User seller, String itemId, LocalDateTime startTime,
            LocalDateTime endTime, String title, String description,
            int antiSnipingWindowSeconds, int antiSnipingExtensionSeconds) throws AuctionException {
        requireCreateAuctionPermission(seller);
        ItemSchema item = requireItem(itemId);
        requireItemOwner(seller, item);
        validateAuctionTimeRange(startTime, endTime);
        requireFutureStartTime(startTime);

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

        item.setAuctionCount(item.getAuctionCount() + 1);
        itemService.updateItem(item);

        ItemDTO itemDTO = itemService.getItemById(itemId);
        return toAuctionDTO(schema, itemDTO);
    }

    public void startAuction(User seller, String auctionId) throws AuctionException {
        AuctionSchema schema = requireAuction(auctionId);
        requireAuctionOwner(seller, schema);
        requireAuctionStatus(schema, AuctionStatus.OPEN, "Auction hien khong o trang thai OPEN");
        ItemSchema item = requireItem(schema.getItemId());

        schema.setStatus(AuctionStatus.RUNNING);
        auctionDAO.update(schema);
        auctionManager.loadAuction(schema, item.getStartingPrice());
    }

    public void loadRunningAuctions() {
        for (AuctionSchema schema : auctionDAO.findByStatus(AuctionStatus.RUNNING)) {
            ItemSchema item = itemService.getItemSchemaById(schema.getItemId());
            if (item == null) {
                continue;
            }

            double depositAmount = calculateDepositAmount(item);
            if (backfillDepositsFromBidHistory(schema, depositAmount)) {
                auctionDAO.update(schema);
            }
            auctionManager.loadAuction(schema, item.getStartingPrice());
        }
    }

    public void endAuction(String auctionId) throws AuctionException {
        AuctionSchema schema = auctionDAO.findById(auctionId);
        if (schema != null) {
            LiveAuction liveAuction = auctionManager.getAuction(auctionId);
            ItemSchema item = itemService.getItemSchemaById(schema.getItemId());
            double depositAmount = item != null ? calculateDepositAmount(item) : 0;

            if (schema.getWinnerId() != null) {
                refundDepositsExcept(schema, liveAuction, schema.getWinnerId(), depositAmount);
                schema.setStatus(AuctionStatus.WAITING_PAYMENT);
                auctionDAO.update(schema);

                scheduler.schedule(() -> {
                    AuctionSchema currentSchema = auctionDAO.findById(auctionId);
                    if (currentSchema != null && currentSchema.getStatus() == AuctionStatus.WAITING_PAYMENT) {
                        try {
                            walletService.forfeitDeposit(currentSchema.getWinnerId(), depositAmount);
                            clearDepositParticipation(currentSchema, null, currentSchema.getWinnerId());
                            currentSchema.setStatus(AuctionStatus.CANCELED);
                            auctionDAO.update(currentSchema);
                        } catch (Exception e) {
                            // Ignore scheduler payment-forfeit errors.
                        }
                    }
                }, 24, TimeUnit.HOURS);
            } else {
                refundDepositsExcept(schema, liveAuction, null, depositAmount);
                schema.setStatus(AuctionStatus.CANCELED);
                auctionDAO.update(schema);
            }
        }
        auctionManager.endAuction(auctionId);
    }

    public void payAuction(User winner, String auctionId) throws AuctionException {
        AuctionSchema schema = requireAuction(auctionId);
        requireAuctionStatus(schema, AuctionStatus.WAITING_PAYMENT, "Auction khong o trang thai cho thanh toan");
        requireWinner(winner, schema);
        ItemSchema itemSchema = requireItem(schema.getItemId());

        double totalPrice = schema.getHighestBid();
        double depositAmount = calculateDepositAmount(itemSchema);
        double remaining = totalPrice - depositAmount;

        try {
            walletService.withdraw(winner.getId(), remaining);
            walletService.forfeitDeposit(winner.getId(), depositAmount);
            walletService.deposit(schema.getSellerId(), totalPrice);
            clearDepositParticipation(schema, null, winner.getId());

            schema.setStatus(AuctionStatus.PAID);
            auctionDAO.update(schema);
        } catch (IllegalArgumentException e) {
            throw new AuctionException("Thanh toan that bai: " + e.getMessage());
        }
    }

    public List<AuctionDTO> getAuctions() {
        return toAuctionDTOList(auctionDAO.findAll());
    }

    public List<AuctionDTO> getAuctionsByStatus(AuctionStatus status) {
        return toAuctionDTOList(auctionDAO.findByStatus(status));
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

    private List<AuctionDTO> toAuctionDTOList(List<AuctionSchema> schemas) {
        List<AuctionDTO> auctionDTOList = new ArrayList<>();
        for (AuctionSchema schema : schemas) {
            AuctionDTO dto = toAuctionDTO(schema);
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
                schema.getStatus(),
                schema.getAntiSnipingWindowSeconds(),
                schema.getAntiSnipingExtensionSeconds(),
                depositAmount,
                currentUserDeposited);
    }

    private boolean hasAuctionDeposit(AuctionSchema schema, String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }

        if (schema.hasDepositedBidder(userId)) {
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
            double depositAmount) {
        if (schema == null) {
            return;
        }

        for (String bidderId : schema.getDepositedBidderIds()) {
            if (retainedBidderId != null && retainedBidderId.equals(bidderId)) {
                continue;
            }
            if (depositAmount > 0) {
                walletService.unfreezeDeposit(bidderId, depositAmount);
            }
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
        if (schema != null && schema.addDepositedBidder(bidderId)) {
            auctionDAO.update(schema);
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
            if (schema.hasDepositedBidder(bidderId)) {
                continue;
            }

            try {
                UserSchema bidder = userService.getUserSchemaById(bidderId);
                if (bidder.getFrozenBalance() >= depositAmount) {
                    changed |= schema.addDepositedBidder(bidderId);
                }
            } catch (RuntimeException e) {
                // Ignore stale bid history entries during startup backfill.
            }
        }
        return changed;
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

    private void requireFutureStartTime(LocalDateTime startTime) throws AuctionException {
        if (startTime.isBefore(LocalDateTime.now())) {
            throw new AuctionException("Thoi gian bat dau phai o trong tuong lai");
        }
    }
}
