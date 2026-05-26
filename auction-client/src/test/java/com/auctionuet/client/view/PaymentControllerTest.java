package com.auctionuet.client.view;

import com.auctionuet.protocol.dto.response.auction.AuctionDTO;
import com.auctionuet.protocol.dto.response.item.ItemDTO;
import com.auctionuet.protocol.dto.response.user.UserDTO;
import com.auctionuet.protocol.enums.AuctionStatus;
import com.auctionuet.protocol.enums.ItemCondition;
import com.auctionuet.protocol.enums.ItemType;
import com.auctionuet.protocol.enums.UserRole;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentControllerTest {

    @Test
    void excludesAdminCanceledAuctionFromExpiredPaymentHistory() {
        AuctionDTO auction = canceledAuction("admin-1");

        assertFalse(PaymentController.isExpiredPaymentForWinner(auction, "bidder-1"));
    }

    @Test
    void includesDeadlineExpiredAuctionInExpiredPaymentHistory() {
        AuctionDTO auction = canceledAuction(null);

        assertTrue(PaymentController.isExpiredPaymentForWinner(auction, "bidder-1"));
    }

    private AuctionDTO canceledAuction(String canceledByUserId) {
        UserDTO seller = new UserDTO("seller-1", "seller", UserRole.SELLER);
        UserDTO winner = new UserDTO("bidder-1", "bidder", UserRole.BIDDER);
        ItemDTO item = new ItemDTO(
                "item-1",
                "Laptop",
                null,
                500.0,
                ItemType.ELECTRONICS,
                seller,
                null,
                ItemCondition.GOOD,
                Map.of("brand", "Dell", "warrantyMonths", 12));
        LocalDateTime startTime = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime endTime = startTime.plusHours(1);

        return new AuctionDTO(
                "auction-1",
                item,
                seller,
                winner,
                null,
                600.0,
                "Laptop auction",
                null,
                startTime,
                endTime,
                null,
                null,
                AuctionStatus.CANCELED,
                60,
                120,
                50.0,
                false,
                canceledByUserId != null ? "Invalid item" : null,
                canceledByUserId,
                canceledByUserId != null ? endTime : null);
    }
}
