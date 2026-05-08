package com.auctionuet.server.domain.service;

import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.exception.AuctionException;
import com.auctionuet.server.mapper.ItemMapper;
import com.auctionuet.server.network.dto.ItemDTO;
import com.auctionuet.server.persistence.dao.ItemDAO;
import com.auctionuet.server.persistence.schema.ItemSchema;
import com.auctionuet.server.util.AppLogger;

import java.util.List;

/**
 * Service chuyên xử lý các hành động liên quan tới Item Management.
 * Được tách ra từ AuctionService để tuân thủ Single Responsibility Principle.
 */
public class ItemService {

    private final ItemDAO itemDAO;

    public ItemService(ItemDAO itemDAO) {
        this.itemDAO = itemDAO;
    }

    /**
     * Tạo item mới. Chỉ Seller mới có quyền.
     */
    public ItemSchema createItem(User seller, ItemDTO itemDTO) throws AuctionException, IllegalArgumentException {
        AppLogger.logServiceCall("ItemService", "createItem",
            "seller=" + seller.getUsername() + " itemName=" + itemDTO.getName()
            + " startingPrice=" + itemDTO.getStartingPrice());

        if (!seller.hasPermission(com.auctionuet.server.domain.enums.Permission.CREATE_ITEM)) {
            AppLogger.logServiceResult("ItemService", "createItem",
                "FAIL — no permission CREATE_ITEM | user=" + seller.getUsername());
            throw new AuctionException("Không có quyền CREATE_ITEM");
        }

        if (itemDTO.getName() == null || itemDTO.getName().isEmpty() || itemDTO.getStartingPrice() <= 0) {
            AppLogger.logServiceResult("ItemService", "createItem",
                "FAIL — invalid item data | name=" + itemDTO.getName());
            throw new IllegalArgumentException("Dữ liệu item không hợp lệ");
        }

        ItemSchema schema = ItemMapper.toNewSchema(itemDTO, seller.getId());
        itemDAO.save(schema);

        AppLogger.logServiceResult("ItemService", "createItem",
            "OK | itemId=" + schema.getId() + " name=" + schema.getName());
        return schema;
    }

    /**
     * Lấy danh sách item của chính user đang đăng nhập (dùng cho GET_MY_ITEMS).
     */
    public List<ItemSchema> getItemsBySellerId(String sellerId) {
        AppLogger.logServiceCall("ItemService", "getItemsBySellerId", "sellerId=" + sellerId);
        List<ItemSchema> items = itemDAO.findBySellerId(sellerId);
        AppLogger.logServiceResult("ItemService", "getItemsBySellerId", "Found " + items.size() + " items");
        return items;
    }

    /**
     * Tìm item theo ID.
     */
    public ItemSchema getItemById(String itemId) {
        AppLogger.logServiceCall("ItemService", "getItemById", "itemId=" + itemId);
        ItemSchema item = itemDAO.findById(itemId);
        AppLogger.logServiceResult("ItemService", "getItemById",
            item != null ? "Found: " + item.getName() : "NOT FOUND");
        return item;
    }

    /**
     * Cập nhật thông tin item (tăng auctionCount, v.v.)
     */
    public void updateItem(ItemSchema item) {
        AppLogger.logServiceCall("ItemService", "updateItem",
            "itemId=" + item.getId() + " auctionCount=" + item.getAuctionCount());
        itemDAO.update(item);
        AppLogger.logServiceResult("ItemService", "updateItem", "OK");
    }
}
