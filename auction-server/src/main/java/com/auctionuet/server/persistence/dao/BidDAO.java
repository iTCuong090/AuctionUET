package com.auctionuet.server.persistence.dao;

import com.auctionuet.server.persistence.schema.BidSchema;
import com.auctionuet.server.util.json.JsonFileHelper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class BidDAO implements GenericDAO<BidSchema> {

    private final String filePath;

    public BidDAO() {
        this.filePath = "data/bids.json";
    }

    public BidDAO(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void save(BidSchema entity) {
        List<BidSchema> list = JsonFileHelper.readList(filePath, BidSchema.class);
        list.add(entity);
        JsonFileHelper.writeList(filePath, list);
    }

    @Override
    public BidSchema findById(String id) {
        return JsonFileHelper.readList(filePath, BidSchema.class)
                .stream().filter(e -> e.getId().equals(id)).findFirst().orElse(null);
    }

    @Override
    public List<BidSchema> findAll() {
        return JsonFileHelper.readList(filePath, BidSchema.class);
    }

    @Override
    public void update(BidSchema entity) {
        List<BidSchema> list = JsonFileHelper.readList(filePath, BidSchema.class);
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
        List<BidSchema> list = JsonFileHelper.readList(filePath, BidSchema.class);
        list.removeIf(e -> e.getId().equals(id));
        JsonFileHelper.writeList(filePath, list);
    }

    public List<BidSchema> findByAuctionId(String auctionId) {
        return JsonFileHelper.readList(filePath, BidSchema.class)
                .stream().filter(e -> auctionId.equals(e.getAuctionId())).collect(Collectors.toList());
    }
}
