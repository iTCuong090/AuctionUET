package com.auctionuet.server.domain.service;

import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.exception.AuctionException;
import com.auctionuet.server.mapper.ItemMapper;
import com.auctionuet.protocol.dto.response.item.ItemDTO;
import com.auctionuet.server.persistence.dao.ItemDAO;
import com.auctionuet.server.persistence.schema.ItemSchema;

import java.util.List;
import java.util.Map;
import com.auctionuet.protocol.enums.ItemType;
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
    public ItemSchema createItem(User seller, String name, String description, double startingPrice, ItemType type, String imageUrl, String condition, Map<String, Object> extraFields) throws AuctionException, IllegalArgumentException {
        if (!seller.hasPermission(com.auctionuet.protocol.enums.Permission.CREATE_ITEM)) {
            throw new AuctionException("Không có quyền CREATE_ITEM");
        }

        ItemSchema schema = ItemMapper.toNewSchema(name, description, startingPrice, type, imageUrl, condition, extraFields, seller.getId());
        itemDAO.save(schema);

        return schema;
    }

    /**
     * Lấy danh sách item của chính user đang đăng nhập (dùng cho GET_MY_ITEMS).
     */
    public List<ItemSchema> getItemsBySellerId(String sellerId) {
        List<ItemSchema> items = itemDAO.findBySellerId(sellerId);
        return items;
    }

    /**
     * Tìm item theo ID.
     */
    public ItemSchema getItemById(String itemId) {
        ItemSchema item = itemDAO.findById(itemId);
        return item;
    }

    /**
     * Cập nhật thông tin item (tăng auctionCount, v.v.)
     */
    public void updateItem(ItemSchema item) {
        itemDAO.update(item);
    }
}
