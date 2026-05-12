package com.auctionuet.server.JUnitTest;

import com.auctionuet.protocol.dto.response.auction.AuctionDTO;
import com.auctionuet.protocol.dto.response.item.ItemDTO;
import com.auctionuet.protocol.enums.AuctionStatus;
import com.auctionuet.protocol.enums.ItemType;
import com.auctionuet.server.mapper.AuctionMapper;
import com.auctionuet.server.mapper.ItemMapper;
import com.auctionuet.server.persistence.schema.AuctionSchema;
import com.auctionuet.server.persistence.schema.ItemSchema;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapperTest {

    @Test
    void testItemMapperToDTO() {
        ItemSchema schema = new ItemSchema(
                "E1",
                LocalDateTime.now(),
                LocalDateTime.now(),
                "ThinkPad",
                "Laptop",
                1000.0,
                ItemType.ELECTRONICS,
                "seller1",
                "img.png",
                "NEW",
                0,
                new LinkedHashMap<>(Map.of("brand", "Lenovo", "warrantyMonths", 24))
        );

        ItemDTO dto = ItemMapper.toDTO(schema, "seller1");
        assertEquals("E1", dto.getId());
        assertEquals("ThinkPad", dto.getName());
        assertEquals("Lenovo", dto.getExtraFields().get("brand"));
    }

    @Test
    void testItemMapperToNewSchema() {
        ItemSchema schema = ItemMapper.toNewSchema(
                "Tranh Dong Ho",
                null,
                500.0,
                ItemType.ART,
                null,
                null,
                new LinkedHashMap<>(Map.of("artist", "Nghe nhan A", "year", 1995, "medium", "Watercolor")),
                "seller_id"
        );

        assertEquals(ItemType.ART, schema.getType());
        assertEquals("Nghe nhan A", schema.getExtraFields().get("artist"));
    }

    @Test
    void testAuctionMapperToDTO() {
        LocalDateTime now = LocalDateTime.now();

        AuctionSchema schema = new AuctionSchema(
                "A1",
                now,
                now,
                "I1",
                "S1",
                "Auction",
                "Desc",
                now,
                now.plusDays(1),
                AuctionStatus.OPEN,
                0.0,
                null,
                60,
                120
        );

        ItemDTO itemDTO = new ItemDTO(
                "I1",
                "Laptop",
                "Desc",
                100.0,
                ItemType.ELECTRONICS,
                "seller1",
                "",
                "NEW",
                new LinkedHashMap<>(Map.of("brand", "Lenovo", "warrantyMonths", 24))
        );

        AuctionDTO dto = AuctionMapper.toDTO(schema, itemDTO, "seller1", null);
        assertEquals("Auction", dto.getTitle());
        assertEquals(AuctionStatus.OPEN, dto.getStatus());
    }
}
