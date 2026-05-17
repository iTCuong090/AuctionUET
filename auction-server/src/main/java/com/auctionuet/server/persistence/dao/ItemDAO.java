package com.auctionuet.server.persistence.dao;

import com.auctionuet.protocol.enums.ItemType;
import com.auctionuet.server.persistence.schema.ItemSchema;
import com.auctionuet.server.util.json.JsonFileHelper;

import java.time.LocalDateTime;
import java.util.List;

public class ItemDAO implements GenericDAO<ItemSchema> {

    private final String filePath;

    // Constructor mặc định, mặc định đường dẫn là items.json.
    public ItemDAO() {
        this.filePath = "data/items.json";
    }

    // Constructor custom, nhận đường dẫn bất kì, dùng để test.
    public ItemDAO(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void save(ItemSchema entity) {
        List<ItemSchema> list = JsonFileHelper.readList(filePath, ItemSchema.class);
        list.add(entity);
        JsonFileHelper.writeList(filePath, list);
    }

    @Override
    public ItemSchema findById(String id) {
        List<ItemSchema> list = JsonFileHelper.readList(filePath, ItemSchema.class);
        for (ItemSchema entity : list) {
            if (entity.getId().equals(id)) {
                return entity;
            }
        }
        return null;
    }

    @Override
    public List<ItemSchema> findAll() {
        return JsonFileHelper.readList(filePath, ItemSchema.class);
    }

    @Override
    public void update(ItemSchema entity) {
        List<ItemSchema> list = JsonFileHelper.readList(filePath, ItemSchema.class);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(entity.getId())) {
                entity.setUpdatedAt(LocalDateTime.now());
                list.set(i, entity);
                break;
            }
        }
        JsonFileHelper.writeList(filePath, list);
    }

    @Override
    public void delete(String id) {
        List<ItemSchema> list = JsonFileHelper.readList(filePath, ItemSchema.class);
        list.removeIf(e -> e.getId().equals(id));
        JsonFileHelper.writeList(filePath, list);
    }

    public List<ItemSchema> findBySellerId(String sellerId) {
        List<ItemSchema> list = JsonFileHelper.readList(filePath, ItemSchema.class);
        List<ItemSchema> result = new java.util.ArrayList<>();
        for (ItemSchema entity : list) {
            if (sellerId.equals(entity.getSellerId())) {
                result.add(entity);
            }
        }
        return result;
    }

    public List<ItemSchema> findByType(ItemType type) {
        List<ItemSchema> list = JsonFileHelper.readList(filePath, ItemSchema.class);
        List<ItemSchema> result = new java.util.ArrayList<>();
        for (ItemSchema entity : list) {
            if (type.equals(entity.getType())) {
                result.add(entity);
            }
        }
        return result;
    }
}
