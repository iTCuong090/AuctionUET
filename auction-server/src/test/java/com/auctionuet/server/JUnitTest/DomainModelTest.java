package com.auctionuet.server.JUnitTest;

import com.auctionuet.protocol.enums.AuctionStatus;
import com.auctionuet.server.domain.model.Bidder;
import com.auctionuet.server.domain.model.LiveAuction;
import com.auctionuet.server.domain.model.Seller;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.exception.AuctionClosedException;
import com.auctionuet.server.exception.InvalidBidException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DomainModelTest {

    @Test
    void testLiveAuctionPlaceBid() throws Exception {
        LiveAuction auction = new LiveAuction("auc1", "item1", "seller1",
                LocalDateTime.now().plusHours(1), AuctionStatus.RUNNING, 100.0, null, 60, 120);

        User bidder = new Bidder("bid1", "user1");
        auction.placeBid(bidder, 150.0, null);

        assertEquals(150.0, auction.getCurrentHighestBid());
        assertEquals("bid1", auction.getCurrentWinnerId());
        assertEquals(1, auction.getBidHistory().size());
    }

    @Test
    void testLiveAuctionBidTooLow() {
        LiveAuction auction = new LiveAuction("auc1", "item1", "seller1",
                LocalDateTime.now().plusHours(1), AuctionStatus.RUNNING, 200.0, null, 60, 120);

        User bidder = new Bidder("bid1", "user1");
        assertThrows(InvalidBidException.class, () -> auction.placeBid(bidder, 200.0, null));
        assertThrows(InvalidBidException.class, () -> auction.placeBid(bidder, 150.0, null));
    }

    @Test
    void testLiveAuctionSellerCannotBid() {
        LiveAuction auction = new LiveAuction("auc1", "item1", "seller1",
                LocalDateTime.now().plusHours(1), AuctionStatus.RUNNING, 100.0, null, 60, 120);

        User seller = new Seller("seller1", "seller1");
        assertThrows(InvalidBidException.class, () -> auction.placeBid(seller, 200.0, null));
    }

    @Test
    void testLiveAuctionClosed() {
        LiveAuction auction = new LiveAuction("auc1", "item1", "seller1",
                LocalDateTime.now().plusHours(1), AuctionStatus.FINISHED, 100.0, null, 60, 120);

        User bidder = new Bidder("bid1", "user1");
        assertThrows(AuctionClosedException.class, () -> auction.placeBid(bidder, 200.0, null));
    }
}
