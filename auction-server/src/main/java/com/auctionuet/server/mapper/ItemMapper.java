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

    // TO DTO (Schema → DTO, chiều ra cho client xem)
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

    // FROM REQUEST DATA (Map → DTO, chiều vào từ client gửi lên)
    public static ItemDTO fromRequestData(Map<String, Object> data) {
        if (data == null) {
            throw new IllegalArgumentException("Item data không được null");
        }

        try {
            String name = (String) data.get("name");
            String description = (String) data.get("description");
            String imageUrl = (String) data.get("imageUrl");
            String condition = (String) data.get("condition");

            double startingPrice = 0;
            Object spObj = data.get("startingPrice");
            if (spObj instanceof Number) {
                startingPrice = ((Number) spObj).doubleValue();
            } else if (spObj instanceof String) {
                startingPrice = Double.parseDouble((String) spObj);
            }

            String typeStr = data.getOrDefault("type", "ELECTRONICS").toString();
            ItemType type;
            try {
                type = ItemType.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Loại item không hợp lệ: " + typeStr);
            }

            // Thu thập extra fields theo loại item
            Map<String, Object> extraFields = new HashMap<>();
            switch (type) {
                case ELECTRONICS -> {
                    extraFields.put("brand", data.get("brand"));
                    extraFields.put("warrantyMonths", data.get("warrantyMonths"));
                }
                case ART -> {
                    extraFields.put("artist", data.get("artist"));
                    extraFields.put("year", data.get("year"));
                    extraFields.put("medium", data.get("medium"));
                }
                case VEHICLE -> {
                    extraFields.put("make", data.get("make"));
                    extraFields.put("model", data.get("model"));
                    extraFields.put("mileage", data.get("mileage"));
                    extraFields.put("vehicleYear", data.get("vehicleYear"));
                }
            }

            return new ItemDTO(
                    null, name, description, startingPrice, type,
                    null, imageUrl, condition, extraFields
            );
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Sai kiểu dữ liệu trong item data: " + e.getMessage());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Giá trị số không hợp lệ: " + e.getMessage());
        }
    }

    // TO SCHEMA (DTO → Schema, tự sinh id/timestamps, logic extraFields ở đây)
    public static ItemSchema toNewSchema(ItemDTO dto, String sellerId) {
        String id = IdGenerator.generate();
        LocalDateTime now = LocalDateTime.now();
        ItemType type = dto.getType();

        Map<String, Object> extra = dto.getExtraFields();
        if (extra == null) {
            extra = new HashMap<>();
        }

        return switch (type) {
            case ELECTRONICS -> new ElectronicsSchema(
                    id, now, now,
                    dto.getName(), dto.getDescription(), dto.getStartingPrice(),
                    type, sellerId, dto.getImageUrl(), dto.getCondition(), 0,
                    (String) extra.getOrDefault("brand", ""),
                    extra.containsKey("warrantyMonths")
                            ? ((Number) extra.get("warrantyMonths")).intValue() : 0
            );
            case ART -> new ArtSchema(
                    id, now, now,
                    dto.getName(), dto.getDescription(), dto.getStartingPrice(),
                    type, sellerId, dto.getImageUrl(), dto.getCondition(), 0,
                    (String) extra.getOrDefault("artist", ""),
                    extra.containsKey("year") ? ((Number) extra.get("year")).intValue() : 0,
                    (String) extra.getOrDefault("medium", "")
            );
            case VEHICLE -> new VehicleSchema(
                    id, now, now,
                    dto.getName(), dto.getDescription(), dto.getStartingPrice(),
                    type, sellerId, dto.getImageUrl(), dto.getCondition(), 0,
                    (String) extra.getOrDefault("make", ""),
                    (String) extra.getOrDefault("model", ""),
                    extra.containsKey("mileage") ? ((Number) extra.get("mileage")).intValue() : 0,
                    extra.containsKey("vehicleYear") ? ((Number) extra.get("vehicleYear")).intValue() : 0
            );
        };
    }
}
