package com.auctionuet.server.mapper;

import com.auctionuet.server.domain.enums.ItemType;
import com.auctionuet.server.domain.model.Item;
import com.auctionuet.server.network.dto.ItemDTO;
import com.auctionuet.server.persistence.schema.*;
import com.auctionuet.server.util.IdGenerator;


import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class ItemMapper {

    // TO DOMAIN
    public static Item toDomain(ItemSchema schema) {
        return new Item(
                schema.getId(), schema.getName(), schema.getDescription(),
                schema.getStartingPrice(), schema.getType(), schema.getSellerId(),
                schema.getImageUrl(), schema.getCondition()
        );
    }

    // TO DTO
    public static ItemDTO toDTO(ItemSchema schema, String sellerUsername) {
        Map<String, Object> extraFields = new HashMap<>();

        if (schema instanceof ElectronicsSchema e) {
            extraFields.put("brand", e.getBrand());
            extraFields.put("warrantyMonths", e.getWarrantyMonths());
        } else if (schema instanceof ArtSchema a) {
            extraFields.put("artist", a.getArtist());
            extraFields.put("year", a.getYear());
            extraFields.put("medium", a.getMedium());
        } else if (schema instanceof VehicleSchema v) {
            extraFields.put("make", v.getMake());
            extraFields.put("model", v.getModel());
            extraFields.put("mileage", v.getMileage());
            extraFields.put("vehicleYear", v.getVehicleYear());
        }

        return new ItemDTO(
                schema.getId(), schema.getName(), schema.getDescription(),
                schema.getStartingPrice(), schema.getType(), sellerUsername,
                schema.getImageUrl(), schema.getCondition(), extraFields
        );
    }

    public static ItemSchema toNewSchema(Map<String, Object> data, String sellerId) {
        String typeStr = (String) data.get("type");
        ItemType type = ItemType.valueOf(typeStr);

        String id = IdGenerator.generate();
        LocalDateTime now = LocalDateTime.now();
        String name = (String) data.get("name");
        String description = (String) data.get("description");
        double startingPrice = ((Number) data.get("startingPrice")).doubleValue();
        String imageUrl = (String) data.getOrDefault("imageUrl", "");
        String condition = (String) data.getOrDefault("condition", "GOOD");

        return switch (type) {
            case ELECTRONICS -> new ElectronicsSchema(
                    id, now, now, name, description, startingPrice, type, sellerId,
                    imageUrl, condition, 0,
                    (String) data.getOrDefault("brand", ""),
                    data.containsKey("warrantyMonths")
                            ? ((Number) data.get("warrantyMonths")).intValue() : 0
            );

            case ART -> new ArtSchema(
                    id, now, now, name, description, startingPrice, type, sellerId,
                    imageUrl, condition, 0,
                    (String) data.getOrDefault("artist", ""),
                    data.containsKey("year") ? ((Number) data.get("year")).intValue() : 0,
                    (String) data.getOrDefault("medium", "")
            );

            case VEHICLE -> new VehicleSchema(
                    id, now, now, name, description, startingPrice, type, sellerId,
                    imageUrl, condition, 0,
                    (String) data.getOrDefault("make", ""),
                    (String) data.getOrDefault("model", ""),
                    data.containsKey("mileage") ? ((Number) data.get("mileage")).intValue() : 0,
                    data.containsKey("vehicleYear") ? ((Number) data.get("vehicleYear")).intValue() : 0
            );
        };
    }
}

