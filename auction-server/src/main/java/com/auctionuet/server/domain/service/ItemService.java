package com.auctionuet.server.domain.service;

import com.auctionuet.protocol.enums.ItemType;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.exception.AuctionException;
import com.auctionuet.server.persistence.dao.ItemDAO;
import com.auctionuet.server.persistence.schema.*;
import com.auctionuet.server.util.IdGenerator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ItemService {

    private final ItemDAO itemDAO;

    public ItemService(ItemDAO itemDAO) {
        this.itemDAO = itemDAO;
    }

    public ItemSchema createItem(User seller, String name, String description, double startingPrice, ItemType type, String imageUrl, String condition, Map<String, Object> extraFields) throws AuctionException, IllegalArgumentException {
        if (!seller.hasPermission(com.auctionuet.protocol.enums.Permission.CREATE_ITEM)) {
            throw new AuctionException("Không có quyền CREATE_ITEM");
        }

        String id = IdGenerator.generate();
        LocalDateTime now = LocalDateTime.now();
        ItemSchema schema;

        if (type == ItemType.ELECTRONICS) {
            String brand = (String) extraFields.get("brand");
            int warranty = extraFields.get("warrantyMonths") != null ? ((Number) extraFields.get("warrantyMonths")).intValue() : 0;
            schema = new ElectronicsSchema(id, now, now, name, description, startingPrice, type, seller.getId(), imageUrl, condition, 0, brand, warranty);
        } else if (type == ItemType.ART) {
            String artist = (String) extraFields.get("artist");
            int year = extraFields.get("year") != null ? ((Number) extraFields.get("year")).intValue() : 0;
            String medium = (String) extraFields.get("medium");
            schema = new ArtSchema(id, now, now, name, description, startingPrice, type, seller.getId(), imageUrl, condition, 0, artist, year, medium);
        } else if (type == ItemType.VEHICLE) {
            String make = (String) extraFields.get("make");
            String model = (String) extraFields.get("model");
            int mileage = extraFields.get("mileage") != null ? ((Number) extraFields.get("mileage")).intValue() : 0;
            int modelYear = extraFields.get("modelYear") != null ? ((Number) extraFields.get("modelYear")).intValue() : 0;
            schema = new VehicleSchema(id, now, now, name, description, startingPrice, type, seller.getId(), imageUrl, condition, 0, make, model, mileage, modelYear);
        } else {
            throw new IllegalArgumentException("Unsupported item type: " + type);
        }

        itemDAO.save(schema);
        return schema;
    }

    public List<ItemSchema> getItemsBySellerId(String sellerId) {
        return itemDAO.findBySellerId(sellerId);
    }

    public ItemSchema getItemById(String itemId) {
        return itemDAO.findById(itemId);
    }

    public void updateItem(ItemSchema item) {
        itemDAO.update(item);
    }
}
