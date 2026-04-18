package com.auctionuet.server.JUnitTest;

import com.auctionuet.server.domain.enums.*;
import com.auctionuet.server.domain.model.*;
import com.auctionuet.server.mapper.AuctionMapper;
import com.auctionuet.server.mapper.ItemMapper;
import com.auctionuet.server.network.dto.*;
import com.auctionuet.server.persistence.schema.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MapperTest {

    @Test
    void testItemMapperToDTO() {
        ElectronicsSchema schema = new ElectronicsSchema(
                "E1",
                LocalDateTime.now(),
                LocalDateTime.now(),
                "ThinkPad",
                "Laptop xịn",
                1000.0,
                ItemType.ELECTRONICS,
                "seller1",
                "img.png",
                "NEW",
                0,
                "Lenovo",
                24
        );

        ItemDTO dto = ItemMapper.toDTO(schema, "seller1");

        assertEquals("E1", dto.getId());
        assertEquals("ThinkPad", dto.getName());
        assertEquals("Lenovo", dto.getExtraFields().get("brand"));
    }

    @Test
    void testItemMapperToNewSchema() {
        Map<String, Object> data = Map.of(
                "type", "ART",
                "name", "Tranh Dong Ho",
                "startingPrice", 500.0,
                "artist", "Nghe nhan A"
        );
        // Cường refactored: Map → ItemDTO (fromRequestData) → Schema (toNewSchema)
        ItemDTO dto = ItemMapper.fromRequestData(data);
        ItemSchema schema = ItemMapper.toNewSchema(dto, "seller_id");

        assertTrue(schema instanceof ArtSchema);
        assertEquals("Nghe nhan A", ((ArtSchema)schema).getArtist());
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
                "Đấu giá laptop",
                "Mô tả",
                now,
                now.plusDays(1),
                AuctionStatus.OPEN,
                0.0,
                null
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
                null
        );

        AuctionDTO dto = AuctionMapper.toDTO(schema, itemDTO, "seller1", null);

        assertEquals("Đấu giá laptop", dto.getTitle());
        assertEquals(AuctionStatus.OPEN, dto.getStatus());
    }
}