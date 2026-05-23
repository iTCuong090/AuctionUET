package com.auctionuet.server.persistence.dao;

import com.auctionuet.protocol.enums.AuctionStatus;
import com.auctionuet.server.persistence.schema.AuctionSchema;
import com.auctionuet.server.util.json.JsonFileHelper;

import java.time.LocalDateTime;
import java.util.List;

public class AuctionDAO implements GenericDAO<AuctionSchema> {

    private final String filePath;

    public AuctionDAO() {
        this.filePath = "data/auctions.json";
    }

    public AuctionDAO(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void save(AuctionSchema entity) {
        List<AuctionSchema> list = JsonFileHelper.readList(filePath, AuctionSchema.class);
        list.add(entity);
        JsonFileHelper.writeList(filePath, list);
    }

    @Override
    public AuctionSchema findById(String id) {
        List<AuctionSchema> list = JsonFileHelper.readList(filePath, AuctionSchema.class);
        for (AuctionSchema entity : list) {
            if (entity.getId().equals(id)) {
                return entity;
            }
        }
        return null;
    }

    @Override
    public List<AuctionSchema> findAll() {
        return JsonFileHelper.readList(filePath, AuctionSchema.class);
    }

    @Override
    public void update(AuctionSchema entity) {
        List<AuctionSchema> list = JsonFileHelper.readList(filePath, AuctionSchema.class);
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
        List<AuctionSchema> list = JsonFileHelper.readList(filePath, AuctionSchema.class);
        list.removeIf(e -> e.getId().equals(id));
        JsonFileHelper.writeList(filePath, list);
    }

    public List<AuctionSchema> findByStatus(AuctionStatus status) {
        List<AuctionSchema> list = JsonFileHelper.readList(filePath, AuctionSchema.class);
        List<AuctionSchema> result = new java.util.ArrayList<>();
        for (AuctionSchema entity : list) {
            if (status.equals(entity.getStatus())) {
                result.add(entity);
            }
        }
        return result;
    }

    public List<AuctionSchema> findBySellerId(String sellerId) {
        List<AuctionSchema> list = JsonFileHelper.readList(filePath, AuctionSchema.class);
        List<AuctionSchema> result = new java.util.ArrayList<>();
        for (AuctionSchema entity : list) {
            if (sellerId.equals(entity.getSellerId())) {
                result.add(entity);
            }
        }
        return result;
    }

    public List<AuctionSchema> findByItemId(String itemId) {
        List<AuctionSchema> list = JsonFileHelper.readList(filePath, AuctionSchema.class);
        List<AuctionSchema> result = new java.util.ArrayList<>();
        for (AuctionSchema entity : list) {
            if (itemId.equals(entity.getItemId())) {
                result.add(entity);
            }
        }
        return result;
    }
}
