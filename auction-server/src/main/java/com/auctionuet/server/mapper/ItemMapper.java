package com.auctionuet.server.mapper;

import com.auctionuet.protocol.dto.response.item.ItemDTO;
import com.auctionuet.protocol.enums.ItemType;
import com.auctionuet.server.domain.model.Item;
import com.auctionuet.server.persistence.schema.ItemSchema;
import com.auctionuet.server.util.IdGenerator;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class ItemMapper {

    public static Item toDomain(ItemSchema schema) {
        return new Item(
                schema.getId(),
                schema.getName(),
                schema.getDescription(),
                schema.getStartingPrice(),
                schema.getType(),
                schema.getSellerId(),
                schema.getImageUrl(),
                schema.getCondition()
        );
    }

    public static ItemDTO toDTO(ItemSchema schema, String sellerUsername) {
        Map<String, Object> extra = schema.getExtraFields();
        if (extra == null || extra.isEmpty()) {
            extra = legacyExtraFields(schema);
        }
        Map<String, Object> normalizedExtra = schema.getType().normalizeAndValidateExtraFields(extra);

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

    private static Map<String, Object> legacyExtraFields(ItemSchema schema) {
        Map<String, Object> legacy = new LinkedHashMap<>();
        switch (schema.getType()) {
            case ELECTRONICS -> {
                legacy.put("brand", schema.getBrand());
                legacy.put("warrantyMonths", schema.getWarrantyMonths());
            }
            case ART -> {
                legacy.put("artist", schema.getArtist());
                legacy.put("year", schema.getYear());
                legacy.put("medium", schema.getMedium());
            }
            case VEHICLE -> {
                legacy.put("make", schema.getMake());
                legacy.put("model", schema.getModel());
                legacy.put("mileage", schema.getMileage());
                legacy.put("vehicleYear", schema.getVehicleYear());
            }
        }
        return legacy;
    }

    public static ItemDTO fromRequestData(Map<String, Object> data) {
        if (data == null) {
            throw new IllegalArgumentException("Item data must not be null");
        }

        String name = readString(data, "name");
        String description = readOptionalString(data, "description");
        String imageUrl = readOptionalString(data, "imageUrl");
        String condition = readOptionalString(data, "condition");
        double startingPrice = readDouble(data, "startingPrice");

        String typeStr = String.valueOf(data.getOrDefault("type", ItemType.ELECTRONICS.name()));
        ItemType type = ItemType.valueOf(typeStr);

        Map<String, Object> extra = new LinkedHashMap<>();
        for (String key : data.keySet()) {
            if (!isCoreItemKey(key)) {
                extra.put(key, data.get(key));
            }
        }
        Map<String, Object> normalizedExtra = type.normalizeAndValidateExtraFields(extra);

        return new ItemDTO(
                "request-item",
                name,
                description,
                startingPrice,
                type,
                "request-seller",
                imageUrl,
                condition,
                normalizedExtra
        );
    }

    public static ItemSchema toNewSchema(
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

    private static boolean isCoreItemKey(String key) {
        return "name".equals(key)
                || "description".equals(key)
                || "startingPrice".equals(key)
                || "type".equals(key)
                || "imageUrl".equals(key)
                || "condition".equals(key);
    }

    private static String readString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (!(value instanceof String s) || s.isBlank()) {
            throw new IllegalArgumentException(key + " must be a non-blank string");
        }
        return s;
    }

    private static String readOptionalString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (!(value instanceof String s)) {
            throw new IllegalArgumentException(key + " must be a string");
        }
        return s;
    }

    private static double readDouble(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value instanceof String s) {
            return Double.parseDouble(s);
        }
        throw new IllegalArgumentException(key + " must be a number");
    }
}
