package com.auctionuet.server.domain.service;

import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.exception.AuctionException;
import com.auctionuet.server.mapper.ItemMapper;
import com.auctionuet.server.network.dto.ItemDTO;
import com.auctionuet.server.persistence.dao.ItemDAO;
import com.auctionuet.server.persistence.schema.ItemSchema;

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
        if (!seller.hasPermission(com.auctionuet.server.domain.enums.Permission.CREATE_ITEM)) {
            throw new AuctionException("Không có quyền CREATE_ITEM");
        }

        if (itemDTO.getName() == null || itemDTO.getName().isEmpty() || itemDTO.getStartingPrice() <= 0) {
            throw new IllegalArgumentException("Dữ liệu item không hợp lệ");
        }

        ItemSchema schema = ItemMapper.toNewSchema(itemDTO, seller.getId());
        itemDAO.save(schema);
        return schema;
    }

    /**
     * Lấy danh sách item của chính user đang đăng nhập (dùng cho GET_MY_ITEMS).
     */
    public List<ItemSchema> getItemsBySellerId(String sellerId) {
        return itemDAO.findBySellerId(sellerId);
    }

    /**
     * Tìm item theo ID.
     */
    public ItemSchema getItemById(String itemId) {
        return itemDAO.findById(itemId);
    }

    /**
     * Cập nhật thông tin item (tăng auctionCount, v.v.)
     */
    public void updateItem(ItemSchema item) {
        itemDAO.update(item);
    }
}
