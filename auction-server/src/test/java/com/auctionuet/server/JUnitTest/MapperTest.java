package com.auctionuet.server.JUnitTest;

import com.auctionuet.protocol.dto.response.auction.AuctionDTO;
import com.auctionuet.protocol.dto.response.item.ItemDTO;
import com.auctionuet.protocol.enums.AuctionStatus;
import com.auctionuet.protocol.enums.ItemType;
import com.auctionuet.server.mapper.AuctionMapper;
import com.auctionuet.server.persistence.schema.AuctionSchema;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapperTest {

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
