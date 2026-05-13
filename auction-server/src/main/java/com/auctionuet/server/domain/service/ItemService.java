package com.auctionuet.server.domain.service;

import com.auctionuet.protocol.dto.response.item.ItemDTO;
import com.auctionuet.protocol.enums.ItemType;
import com.auctionuet.protocol.enums.Permission;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.exception.AuctionException;
import com.auctionuet.server.persistence.dao.ItemDAO;
import com.auctionuet.server.persistence.schema.ItemSchema;
import com.auctionuet.server.util.IdGenerator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ItemService {

    private final ItemDAO itemDAO;

    public ItemService(ItemDAO itemDAO) {
        this.itemDAO = itemDAO;
    }

    public ItemDTO createItem(
            User seller,
            String name,
            String description,
            double startingPrice,
            ItemType type,
            String imageUrl,
            String condition,
            Map<String, Object> extraFields) throws AuctionException {
        if (!seller.hasPermission(Permission.CREATE_ITEM)) {
            throw new AuctionException("Khong co quyen CREATE_ITEM");
        }

        ItemSchema schema = toNewSchema(
                name,
                description,
                startingPrice,
                type,
                imageUrl,
                condition,
                extraFields,
                seller.getId()
        );
        itemDAO.save(schema);
        return toItemDTO(schema, seller.getUsername());
    }

    public List<ItemDTO> getItemsBySellerId(String sellerId) {
        return itemDAO.findBySellerId(sellerId)
                .stream()
                .map(this::toItemDTO)
                .collect(Collectors.toList());
    }

    public ItemDTO getItemById(String itemId) {
        ItemSchema schema = getItemSchemaById(itemId);
        if (schema == null) {
            return null;
        }
        return toItemDTO(schema);
    }

    public ItemSchema getItemSchemaById(String itemId) {
        return itemDAO.findById(itemId);
    }

    public void updateItem(ItemSchema item) {
        itemDAO.update(item);
    }

    private ItemDTO toItemDTO(ItemSchema schema) {
        return toItemDTO(schema, schema.getSellerId());
    }

    private ItemDTO toItemDTO(ItemSchema schema, String sellerUsername) {
        Map<String, Object> normalizedExtra = schema.getType()
                .normalizeAndValidateExtraFields(schema.getExtraFields());

        return new ItemDTO(
                schema.getId(),
                schema.getName(),
                schema.getDescription(),
                schema.getStartingPrice(),
                schema.getType(),
                sellerUsername,
                schema.getImageUrl(),
                schema.getCondition(),
                normalizedExtra
        );
    }

    private ItemSchema toNewSchema(
            String name,
            String description,
            double startingPrice,
            ItemType type,
            String imageUrl,
            String condition,
            Map<String, Object> extraFields,
            String sellerId) {
        String id = IdGenerator.generate();
        LocalDateTime now = LocalDateTime.now();

        Map<String, Object> normalizedExtra = type.normalizeAndValidateExtraFields(
                extraFields == null ? Map.of() : extraFields
        );

        return new ItemSchema(
                id,
                now,
                now,
                name,
                description,
                startingPrice,
                type,
                sellerId,
                imageUrl,
                condition,
                0,
                normalizedExtra
        );
    }
}
