package com.auctionuet.server.domain.service;

import com.auctionuet.server.domain.enums.AuctionStatus;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.domain.manager.AuctionManager;
import com.auctionuet.server.exception.AuctionException;
import com.auctionuet.server.mapper.ItemMapper;
import com.auctionuet.server.network.dto.ItemDTO;
import com.auctionuet.server.persistence.dao.ItemDAO;
import com.auctionuet.server.persistence.dao.AuctionDAO;
import com.auctionuet.server.persistence.schema.ItemSchema;
import com.auctionuet.server.persistence.schema.AuctionSchema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class AuctionService {

    private final ItemDAO itemDAO;
    private final AuctionDAO auctionDAO;
    private final AuctionManager auctionManager;

    public AuctionService(ItemDAO itemDAO, AuctionDAO auctionDAO) {
        this.itemDAO = itemDAO;
        this.auctionDAO = auctionDAO;
        this.auctionManager = AuctionManager.getInstance();
    }

    public ItemSchema createItem(User seller, ItemDTO itemDTO) throws AuctionException, IllegalArgumentException {
        if (!seller.hasPermission("CREATE_ITEM")) {
            throw new AuctionException("Không có quyền CREATE_ITEM");
        }

        if (itemDTO.getName() == null || itemDTO.getName().isEmpty() || itemDTO.getStartingPrice() <= 0) {
            throw new IllegalArgumentException("Dữ liệu item không hợp lệ");
        }

        ItemSchema schema = ItemMapper.toNewSchema(itemDTO, seller.getId());
        itemDAO.save(schema);
        return schema;
    }

    public AuctionSchema createAuction(User seller, String itemId, LocalDateTime startTime, LocalDateTime endTime, String title, String description) throws AuctionException {
        if (!seller.hasPermission("CREATE_AUCTION")) {
            throw new AuctionException("Không có quyền CREATE_AUCTION");
        }
        ItemSchema item = itemDAO.findById(itemId);
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
            null
        );

        auctionDAO.save(schema);
        
        item.setAuctionCount(item.getAuctionCount() + 1);
        itemDAO.update(item);
        
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
            schema.setStatus(AuctionStatus.FINISHED);
            auctionDAO.update(schema);
        }
        auctionManager.endAuction(auctionId);
    }

    public List<AuctionSchema> getAuctions() {
        return auctionDAO.findAll();
    }

    public List<AuctionSchema> getAuctionsByStatus(AuctionStatus status) {
        return auctionDAO.findByStatus(status);
    }

    public AuctionSchema getAuctionById(String Id){
        return auctionDAO.findById(Id);
    }
    public List<AuctionSchema> getAuctionsBySeller(String sellerId) {
        return auctionDAO.findBySellerId(sellerId);
    }
}
