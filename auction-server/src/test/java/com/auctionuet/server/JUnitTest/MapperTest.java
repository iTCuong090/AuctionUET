package com.auctionuet.server.JUnitTest;

import com.auctionuet.protocol.dto.response.auction.AuctionDTO;
import com.auctionuet.protocol.dto.response.item.ItemDTO;
import com.auctionuet.protocol.dto.response.user.UserDTO;
import com.auctionuet.protocol.enums.AuctionStatus;
import com.auctionuet.protocol.enums.ItemCondition;
import com.auctionuet.protocol.enums.ItemType;
import com.auctionuet.protocol.enums.UserRole;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MapperTest {

    @Test
    void testAuctionDTOResourceShapeWithoutBid() {
        LocalDateTime now = LocalDateTime.now();
        UserDTO seller = new UserDTO("seller1", "seller", UserRole.SELLER);
        ItemDTO itemDTO = new ItemDTO(
                "I1",
                "Laptop",
                "Desc",
                100.0,
                ItemType.ELECTRONICS,
                seller,
                "",
                ItemCondition.NEW,
                new LinkedHashMap<>(Map.of("brand", "Lenovo", "warrantyMonths", 24))
        );

        AuctionDTO dto = new AuctionDTO(
                "A1",
                itemDTO,
                seller,
                null,
                null,
                itemDTO.getStartingPrice(),
                "Auction",
                "Desc",
                now,
                now.plusDays(1),
                AuctionStatus.OPEN,
                60,
                120);

        assertEquals("Auction", dto.getTitle());
        assertEquals(AuctionStatus.OPEN, dto.getStatus());
        assertEquals("seller", dto.getSeller().getUsername());
        assertNull(dto.getWinner());
        assertNull(dto.getCurrentHighestBid());
        assertEquals(100.0, dto.getCurrentPrice());
    }
}
