package com.auctionuet.server.domain.service;

import com.auctionuet.protocol.enums.AuctionStatus;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.domain.manager.AuctionManager;
import com.auctionuet.server.exception.AuctionException;
import com.auctionuet.server.persistence.dao.AuctionDAO;
import com.auctionuet.server.persistence.schema.ItemSchema;
import com.auctionuet.server.persistence.schema.AuctionSchema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service chuyên xử lý các hành động liên quan tới Auction Management.
 * Logic về Item đã được tách sang ItemService.
 */
public class AuctionService {

    private final ItemService itemService;
    private final AuctionDAO auctionDAO;
    private final AuctionManager auctionManager;
    private final WalletService walletService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public AuctionService(ItemService itemService, AuctionDAO auctionDAO, WalletService walletService) {
        this.itemService = itemService;
        this.auctionDAO = auctionDAO;
        this.walletService = walletService;
        this.auctionManager = AuctionManager.getInstance();
        this.auctionManager.setEndAuctionCallback(this::endAuction);
    }

    public AuctionSchema createAuction(User seller, String itemId, LocalDateTime startTime,
            LocalDateTime endTime, String title, String description,
            int antiSnipingWindowSeconds, int antiSnipingExtensionSeconds) throws AuctionException {
        if (!seller.hasPermission(com.auctionuet.protocol.enums.Permission.CREATE_AUCTION)) {
            throw new AuctionException("Không có quyền CREATE_AUCTION");
        }

        ItemSchema item = itemService.getItemById(itemId);
        if (item == null) {
            throw new AuctionException("Item không tồn tại");
        }

        if (!seller.getId().equals(item.getSellerId())) {
            throw new AuctionException("Bạn không phải chủ sở hữu của item này");
        }

        if (endTime.isBefore(startTime) || endTime.isEqual(startTime)) {
            throw new AuctionException("Thời gian kết thúc phải sau thời gian bắt đầu");
        }

        if (startTime.isBefore(LocalDateTime.now())) {
            throw new AuctionException("Thời gian bắt đầu phải ở trong tương lai");
        }

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
            antiSnipingExtensionSeconds
        );

        auctionDAO.save(schema);

        item.setAuctionCount(item.getAuctionCount() + 1);
        itemService.updateItem(item);

        return schema;
    }

    public void startAuction(User seller, String auctionId) throws AuctionException {
        AuctionSchema schema = auctionDAO.findById(auctionId);
        if (schema == null) {
            throw new AuctionException("Auction không tồn tại");
        }

        if (!schema.getSellerId().equals(seller.getId())) {
            throw new AuctionException("Bạn không phải chủ sở hữu của auction này");
        }

        if (schema.getStatus() != AuctionStatus.OPEN) {
            throw new AuctionException("Auction hiện không ở trạng thái OPEN");
        }

        schema.setStatus(AuctionStatus.RUNNING);
        auctionDAO.update(schema);
        auctionManager.loadAuction(schema);
    }

    public void endAuction(String auctionId) throws AuctionException {
        AuctionSchema schema = auctionDAO.findById(auctionId);
        if (schema != null) {
            if (schema.getWinnerId() != null) {
                schema.setStatus(AuctionStatus.WAITING_PAYMENT);
                auctionDAO.update(schema);
                
                ItemSchema item = itemService.getItemById(schema.getItemId());
                double depositAmount = item != null ? item.getStartingPrice() * 0.10 : 0;
                
                scheduler.schedule(() -> {
                    AuctionSchema currentSchema = auctionDAO.findById(auctionId);
                    if (currentSchema != null && currentSchema.getStatus() == AuctionStatus.WAITING_PAYMENT) {
                        try {
                            walletService.forfeitDeposit(currentSchema.getWinnerId(), depositAmount);
                            currentSchema.setStatus(AuctionStatus.CANCELED);
                            auctionDAO.update(currentSchema);
                        } catch (Exception e) {
                            // Do nothing or handle
                        }
                    }
                }, 24, TimeUnit.HOURS);
            } else {
                schema.setStatus(AuctionStatus.CANCELED);
                auctionDAO.update(schema);
            }
        }
        auctionManager.endAuction(auctionId);
    }

    public void payAuction(User winner, String auctionId) throws AuctionException {
        AuctionSchema schema = auctionDAO.findById(auctionId);
        if (schema == null) {
            throw new AuctionException("Auction không tồn tại");
        }
        if (schema.getStatus() != AuctionStatus.WAITING_PAYMENT) {
            throw new AuctionException("Auction không ở trạng thái chờ thanh toán");
        }
        if (!winner.getId().equals(schema.getWinnerId())) {
            throw new AuctionException("Bạn không phải người chiến thắng");
        }
        
        ItemSchema itemSchema = itemService.getItemById(schema.getItemId());
        if (itemSchema == null) {
            throw new AuctionException("Item không tồn tại");
        }
        
        double totalPrice = schema.getHighestBid();
        double depositAmount = itemSchema.getStartingPrice() * 0.10;
        double remaining = totalPrice - depositAmount;
        
        // This relies on withdraw throwing exception if insufficient balance
        try {
            walletService.withdraw(winner.getId(), remaining);
            walletService.forfeitDeposit(winner.getId(), depositAmount); // Chuyển cọc thành thanh toán (hoặc trừ vào admin, nhưng theo spec là biến mất hoặc trừ)
            walletService.deposit(schema.getSellerId(), totalPrice);
            
            schema.setStatus(AuctionStatus.PAID);
            auctionDAO.update(schema);
        } catch (IllegalArgumentException e) {
            throw new AuctionException("Thanh toán thất bại: " + e.getMessage());
        }
    }

    public List<AuctionSchema> getAuctions() {
        List<AuctionSchema> list = auctionDAO.findAll();
        return list;
    }

    public List<AuctionSchema> getAuctionsByStatus(AuctionStatus status) {
        List<AuctionSchema> list = auctionDAO.findByStatus(status);
        return list;
    }

    public AuctionSchema getAuctionById(String id) {
        AuctionSchema schema = auctionDAO.findById(id);
        return schema;
    }

    public List<AuctionSchema> getAuctionsBySeller(String sellerId) {
        List<AuctionSchema> list = auctionDAO.findBySellerId(sellerId);
        return list;
    }
}
