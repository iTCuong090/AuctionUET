package com.auctionuet.server.persistence.dao;

import com.auctionuet.server.domain.enums.AuctionStatus;
import com.auctionuet.server.persistence.schema.AuctionSchema;
import com.auctionuet.server.util.json.JsonFileHelper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
        return JsonFileHelper.readList(filePath, AuctionSchema.class)
                .stream().filter(e -> e.getId().equals(id)).findFirst().orElse(null);
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
        return JsonFileHelper.readList(filePath, AuctionSchema.class)
                .stream().filter(e -> status.equals(e.getStatus())).collect(Collectors.toList());
    }

    public List<AuctionSchema> findBySellerId(String sellerId) {
        return JsonFileHelper.readList(filePath, AuctionSchema.class)
                .stream().filter(e -> sellerId.equals(e.getSellerId())).collect(Collectors.toList());
    }

    public AuctionSchema findByItemId(String itemId) {
        return JsonFileHelper.readList(filePath, AuctionSchema.class)
                .stream().filter(e -> itemId.equals(e.getItemId())).findFirst().orElse(null);
    }
}
