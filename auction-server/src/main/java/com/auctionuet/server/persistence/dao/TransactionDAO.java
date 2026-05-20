package com.auctionuet.server.persistence.dao;

import com.auctionuet.server.persistence.schema.TransactionSchema;
import com.auctionuet.server.util.json.JsonFileHelper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO implements GenericDAO<TransactionSchema> {

    private final String filePath;

    public TransactionDAO() {
        this.filePath = "data/transactions.json";
    }

    public TransactionDAO(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void save(TransactionSchema entity) {
        List<TransactionSchema> list = JsonFileHelper.readList(filePath, TransactionSchema.class);
        list.add(entity);
        JsonFileHelper.writeList(filePath, list);
    }

    @Override
    public TransactionSchema findById(String id) {
        List<TransactionSchema> list = JsonFileHelper.readList(filePath, TransactionSchema.class);
        for (TransactionSchema entity : list) {
            if (entity.getId().equals(id)) {
                return entity;
            }
        }
        return null;
    }

    @Override
    public List<TransactionSchema> findAll() {
        return JsonFileHelper.readList(filePath, TransactionSchema.class);
    }

    @Override
    public void update(TransactionSchema entity) {
        List<TransactionSchema> list = JsonFileHelper.readList(filePath, TransactionSchema.class);
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
        List<TransactionSchema> list = JsonFileHelper.readList(filePath, TransactionSchema.class);
        list.removeIf(e -> e.getId().equals(id));
        JsonFileHelper.writeList(filePath, list);
    }

    public List<TransactionSchema> findByUserId(String userId) {
        List<TransactionSchema> result = new ArrayList<>();
        for (TransactionSchema entity : findAll()) {
            if (userId.equals(entity.getUserId())) {
                result.add(entity);
            }
        }
        return result;
    }

    public List<TransactionSchema> findByAuctionId(String auctionId) {
        List<TransactionSchema> result = new ArrayList<>();
        for (TransactionSchema entity : findAll()) {
            if (auctionId.equals(entity.getAuctionId())) {
                result.add(entity);
            }
        }
        return result;
    }
}
