package com.auctionuet.server.domain.service;

import com.auctionuet.protocol.dto.response.item.ItemDTO;
import com.auctionuet.protocol.dto.response.user.UserDTO;
import com.auctionuet.protocol.enums.ItemCondition;
import com.auctionuet.protocol.enums.ItemType;
import com.auctionuet.protocol.enums.Permission;
import com.auctionuet.protocol.enums.AuctionStatus;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.exception.AuctionException;
import com.auctionuet.server.persistence.dao.AuctionDAO;
import com.auctionuet.server.persistence.dao.ItemDAO;
import com.auctionuet.server.persistence.schema.AuctionSchema;
import com.auctionuet.server.persistence.schema.ItemSchema;
import com.auctionuet.server.util.IdGenerator;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ItemService {

    private final ItemDAO itemDAO;
    private final UserService userService;
    private final AuctionDAO auctionDAO;

    public ItemService(ItemDAO itemDAO, UserService userService) {
        this(itemDAO, userService, new AuctionDAO());
    }

    public ItemService(ItemDAO itemDAO, UserService userService, AuctionDAO auctionDAO) {
        this.itemDAO = itemDAO;
        this.userService = userService;
        this.auctionDAO = auctionDAO;
    }

    public ItemDTO createItem(
            User seller,
            String name,
            String description,
            double startingPrice,
            ItemType type,
            String imageUrl,
            ItemCondition condition,
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
        return toItemDTO(schema, userService.toDTO(seller));
    }

    public List<ItemDTO> getItemsBySellerId(String sellerId) {
        return itemDAO.findBySellerId(sellerId)
                .stream()
                .map(this::toItemDTO)
                .collect(Collectors.toList());
    }

    public List<ItemDTO> getItemsByOwnerId(String ownerId) {
        return getItemsBySellerId(ownerId);
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

    public void transferOwnership(String itemId, String newOwnerId) throws AuctionException {
        ItemSchema item = requireItemSchema(itemId);
        userService.getUserSchemaById(newOwnerId);
        item.setSellerId(newOwnerId);
        item.setArchived(false);
        itemDAO.update(item);
    }

    public void archiveItem(User owner, String itemId) throws AuctionException {
        if (!owner.hasPermission(Permission.DELETE_ITEM)) {
            throw new AuctionException("Khong co quyen DELETE_ITEM");
        }

        ItemSchema item = requireItemSchema(itemId);
        if (!owner.getId().equals(item.getSellerId())) {
            throw new AuctionException("Ban khong phai chu so huu cua item nay");
        }
        if (hasActiveAuction(itemId)) {
            throw new AuctionException("Khong the go vat pham dang co phien dau gia active");
        }

        item.setArchived(true);
        itemDAO.update(item);
    }

    private boolean hasActiveAuction(String itemId) {
        for (AuctionSchema auction : auctionDAO.findByItemId(itemId)) {
            AuctionStatus status = auction.getStatus();
            if (status == AuctionStatus.OPEN
                    || status == AuctionStatus.RUNNING
                    || status == AuctionStatus.WAITING_PAYMENT) {
                return true;
            }
        }
        return false;
    }

    private ItemDTO toItemDTO(ItemSchema schema) {
        return toItemDTO(schema, userService.getUserDTOById(schema.getSellerId()));
    }

    private ItemSchema requireItemSchema(String itemId) throws AuctionException {
        ItemSchema item = itemDAO.findById(itemId);
        if (item == null) {
            throw new AuctionException("Item khong ton tai");
        }
        return item;
    }

    private ItemDTO toItemDTO(ItemSchema schema, UserDTO seller) {
        Map<String, Object> normalizedExtra = normalizeExtraFieldsForResponse(schema);

        return new ItemDTO(
                schema.getId(),
                schema.getName(),
                schema.getDescription(),
                schema.getStartingPrice(),
                schema.getType(),
                seller,
                schema.getImageUrl(),
                schema.getCondition(),
                normalizedExtra
        );
    }

    private Map<String, Object> normalizeExtraFieldsForResponse(ItemSchema schema) {
        try {
            return schema.getType().normalizeAndValidateExtraFields(schema.getExtraFields());
        } catch (IllegalArgumentException ex) {
            return defaultExtraFields(schema.getType());
        }
    }

    private Map<String, Object> defaultExtraFields(ItemType type) {
        Map<String, Object> defaults = new LinkedHashMap<>();
        switch (type) {
            case ELECTRONICS -> {
                defaults.put("brand", "Unknown");
                defaults.put("warrantyMonths", 0);
            }
            case ART -> {
                defaults.put("artist", "Unknown");
                defaults.put("year", Year.now().getValue());
                defaults.put("medium", "Unknown");
            }
            case VEHICLE -> {
                defaults.put("make", "Unknown");
                defaults.put("model", "Unknown");
                defaults.put("mileage", 0);
                defaults.put("vehicleYear", 1886);
            }
        }
        return defaults;
    }

    private ItemSchema toNewSchema(
            String name,
            String description,
            double startingPrice,
            ItemType type,
            String imageUrl,
            ItemCondition condition,
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
