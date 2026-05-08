package com.auctionuet.server.domain.service;

import com.auctionuet.server.domain.enums.AuctionStatus;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.domain.manager.AuctionManager;
import com.auctionuet.server.exception.AuctionException;
import com.auctionuet.server.persistence.dao.AuctionDAO;
import com.auctionuet.server.persistence.schema.ItemSchema;
import com.auctionuet.server.persistence.schema.AuctionSchema;
import com.auctionuet.server.util.AppLogger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service chuyên xử lý các hành động liên quan tới Auction Management.
 * Logic về Item đã được tách sang ItemService.
 */
public class AuctionService {

    private final ItemService itemService;
    private final AuctionDAO auctionDAO;
    private final AuctionManager auctionManager;

    public AuctionService(ItemService itemService, AuctionDAO auctionDAO) {
        this.itemService = itemService;
        this.auctionDAO = auctionDAO;
        this.auctionManager = AuctionManager.getInstance();
    }

    public AuctionSchema createAuction(User seller, String itemId, LocalDateTime startTime,
            LocalDateTime endTime, String title, String description) throws AuctionException {

        AppLogger.logServiceCall("AuctionService", "createAuction",
            "seller=" + seller.getUsername() + " itemId=" + itemId + " title=" + title);

        if (!seller.hasPermission(com.auctionuet.server.domain.enums.Permission.CREATE_AUCTION)) {
            AppLogger.logServiceResult("AuctionService", "createAuction",
                "FAIL — no permission CREATE_AUCTION | user=" + seller.getUsername());
            throw new AuctionException("Không có quyền CREATE_AUCTION");
        }

        ItemSchema item = itemService.getItemById(itemId);
        if (item == null) {
            AppLogger.logServiceResult("AuctionService", "createAuction",
                "FAIL — item not found: " + itemId);
            throw new AuctionException("Item không tồn tại");
        }

        if (!seller.getId().equals(item.getSellerId())) {
            AppLogger.logServiceResult("AuctionService", "createAuction",
                "FAIL — seller is not item owner | seller=" + seller.getUsername());
            throw new AuctionException("Bạn không phải chủ sở hữu của item này");
        }

        if (endTime.isBefore(startTime) || endTime.isEqual(startTime)) {
            AppLogger.logServiceResult("AuctionService", "createAuction",
                "FAIL — endTime must be after startTime");
            throw new AuctionException("Thời gian kết thúc phải sau thời gian bắt đầu");
        }

        if (startTime.isBefore(LocalDateTime.now())) {
            AppLogger.logServiceResult("AuctionService", "createAuction",
                "FAIL — startTime must be in the future");
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
            null
        );

        auctionDAO.save(schema);

        item.setAuctionCount(item.getAuctionCount() + 1);
        itemService.updateItem(item);

        AppLogger.logServiceResult("AuctionService", "createAuction",
            "OK | auctionId=" + schema.getId() + " status=" + schema.getStatus()
            + " start=" + startTime + " end=" + endTime);
        return schema;
    }

    public void startAuction(User seller, String auctionId) throws AuctionException {
        AppLogger.logServiceCall("AuctionService", "startAuction",
            "seller=" + seller.getUsername() + " auctionId=" + auctionId);

        AuctionSchema schema = auctionDAO.findById(auctionId);
        if (schema == null) {
            AppLogger.logServiceResult("AuctionService", "startAuction",
                "FAIL — auction not found: " + auctionId);
            throw new AuctionException("Auction không tồn tại");
        }

        if (!schema.getSellerId().equals(seller.getId())) {
            AppLogger.logServiceResult("AuctionService", "startAuction",
                "FAIL — not owner | seller=" + seller.getUsername());
            throw new AuctionException("Bạn không phải chủ sở hữu của auction này");
        }

        if (schema.getStatus() != AuctionStatus.OPEN) {
            AppLogger.logServiceResult("AuctionService", "startAuction",
                "FAIL — invalid status: " + schema.getStatus());
            throw new AuctionException("Auction hiện không ở trạng thái OPEN");
        }

        schema.setStatus(AuctionStatus.RUNNING);
        auctionDAO.update(schema);
        auctionManager.loadAuction(schema);

        AppLogger.logServiceResult("AuctionService", "startAuction",
            "OK | auctionId=" + auctionId + " status=RUNNING");
    }

    public void endAuction(String auctionId) throws AuctionException {
        AppLogger.logServiceCall("AuctionService", "endAuction", "auctionId=" + auctionId);

        AuctionSchema schema = auctionDAO.findById(auctionId);
        if (schema != null) {
            schema.setStatus(AuctionStatus.FINISHED);
            auctionDAO.update(schema);
        }
        auctionManager.endAuction(auctionId);

        AppLogger.logServiceResult("AuctionService", "endAuction",
            "OK | auctionId=" + auctionId + " status=FINISHED");
    }

    public List<AuctionSchema> getAuctions() {
        AppLogger.logServiceCall("AuctionService", "getAuctions", "all");
        List<AuctionSchema> list = auctionDAO.findAll();
        AppLogger.logServiceResult("AuctionService", "getAuctions", "Found " + list.size() + " auctions");
        return list;
    }

    public List<AuctionSchema> getAuctionsByStatus(AuctionStatus status) {
        AppLogger.logServiceCall("AuctionService", "getAuctionsByStatus", "status=" + status);
        List<AuctionSchema> list = auctionDAO.findByStatus(status);
        AppLogger.logServiceResult("AuctionService", "getAuctionsByStatus", "Found " + list.size());
        return list;
    }

    public AuctionSchema getAuctionById(String id) {
        AppLogger.logServiceCall("AuctionService", "getAuctionById", "id=" + id);
        AuctionSchema schema = auctionDAO.findById(id);
        AppLogger.logServiceResult("AuctionService", "getAuctionById",
            schema != null ? "Found: " + schema.getTitle() : "NOT FOUND");
        return schema;
    }

    public List<AuctionSchema> getAuctionsBySeller(String sellerId) {
        AppLogger.logServiceCall("AuctionService", "getAuctionsBySeller", "sellerId=" + sellerId);
        List<AuctionSchema> list = auctionDAO.findBySellerId(sellerId);
        AppLogger.logServiceResult("AuctionService", "getAuctionsBySeller", "Found " + list.size());
        return list;
    }
}
