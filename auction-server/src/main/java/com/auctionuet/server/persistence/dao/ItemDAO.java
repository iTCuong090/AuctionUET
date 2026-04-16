package com.auctionuet.server.persistence.dao;

import com.auctionuet.server.domain.enums.ItemType;
import com.auctionuet.server.persistence.schema.ItemSchema;
import com.auctionuet.server.util.json.JsonFileHelper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class ItemDAO implements GenericDAO<ItemSchema> {

    private final String filePath;

    public ItemDAO() {
        this.filePath = "data/items.json";
    }

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
        return JsonFileHelper.readList(filePath, ItemSchema.class)
                .stream().filter(e -> e.getId().equals(id)).findFirst().orElse(null);
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
        return JsonFileHelper.readList(filePath, ItemSchema.class)
                .stream().filter(e -> sellerId.equals(e.getSellerId())).collect(Collectors.toList());
    }

    public List<ItemSchema> findByType(ItemType type) {
        return JsonFileHelper.readList(filePath, ItemSchema.class)
                .stream().filter(e -> type.equals(e.getType())).collect(Collectors.toList());
    }
}
