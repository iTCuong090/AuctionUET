package com.auctionuet.server.JUnitTest;

import com.auctionuet.server.domain.enums.AuctionStatus;
import com.auctionuet.server.domain.model.*;
import com.auctionuet.server.exception.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class DomainModelTest {

    @Test
    void testItemImmutable() {
        Class<?> itemClass = Item.class;
        for (Field field : itemClass.getDeclaredFields()) {
            assertTrue(Modifier.isFinal(field.getModifiers()), "Field " + field.getName() + " phải là final");
        }
    }

    @Test
    void testLiveAuctionPlaceBid() throws Exception {
        LiveAuction auction = new LiveAuction("auc1", "item1", "seller1",
                LocalDateTime.now().plusHours(1), AuctionStatus.RUNNING, 100.0, null, 60, 120);

        User bidder = new Bidder("bid1", "user1");
        auction.placeBid(bidder, 150.0);

        assertEquals(150.0, auction.getCurrentHighestBid());
        assertEquals("bid1", auction.getCurrentWinnerId());
        assertEquals(1, auction.getBidHistory().size());
    }

    @Test
    void testLiveAuctionBidTooLow() {
        LiveAuction auction = new LiveAuction("auc1", "item1", "seller1",
                LocalDateTime.now().plusHours(1), AuctionStatus.RUNNING, 200.0, null, 60, 120);

        User bidder = new Bidder("bid1", "user1");
        assertThrows(InvalidBidException.class, () -> auction.placeBid(bidder, 200.0));
        assertThrows(InvalidBidException.class, () -> auction.placeBid(bidder, 150.0));
    }

    @Test
    void testLiveAuctionSellerCannotBid() {
        LiveAuction auction = new LiveAuction("auc1", "item1", "seller1",
                LocalDateTime.now().plusHours(1), AuctionStatus.RUNNING, 100.0, null, 60, 120);

        User seller = new Seller("seller1", "seller1");
        assertThrows(InvalidBidException.class, () -> auction.placeBid(seller, 200.0));
    }

    @Test
    void testLiveAuctionClosed() {
        LiveAuction auction = new LiveAuction("auc1", "item1", "seller1",
                LocalDateTime.now().plusHours(1), AuctionStatus.FINISHED, 100.0, null, 60, 120);

        User bidder = new Bidder("bid1", "user1");
        assertThrows(AuctionClosedException.class, () -> auction.placeBid(bidder, 200.0));
    }
}