package com.auctionuet.server.JUnitTest;

import com.auctionuet.protocol.enums.AuctionStatus;
import com.auctionuet.server.domain.model.AutoBidConfig;
import com.auctionuet.server.domain.model.Bidder;
import com.auctionuet.server.domain.model.LiveAuction;
import com.auctionuet.server.domain.model.Seller;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.exception.AuctionClosedException;
import com.auctionuet.server.exception.InvalidBidException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void testAutoBidEqualMaxPrioritizesEarlierRegistration() throws Exception {
        LiveAuction auction = new LiveAuction("auc1", "item1", "seller1",
                LocalDateTime.now().plusHours(1), AuctionStatus.RUNNING, 500.0, null, 60, 120);

        auction.addAutoBid(new AutoBidConfig("bid1", "user1", 1000.0, 50.0));
        Thread.sleep(2);
        auction.addAutoBid(new AutoBidConfig("bid2", "user2", 1000.0, 50.0));

        assertEquals("bid1", auction.getCurrentWinnerId());
        assertEquals(1000.0, auction.getCurrentHighestBid());
        assertTrue(auction.getAutoBidConfig("bid1").isActive());
        assertFalse(auction.getAutoBidConfig("bid2").isActive());
    }

    @Test
    void testAutoBidHigherMaxHasPriority() throws Exception {
        LiveAuction auction = new LiveAuction("auc1", "item1", "seller1",
                LocalDateTime.now().plusHours(1), AuctionStatus.RUNNING, 500.0, null, 60, 120);

        auction.addAutoBid(new AutoBidConfig("bid1", "user1", 900.0, 50.0));
        Thread.sleep(2);
        auction.addAutoBid(new AutoBidConfig("bid2", "user2", 1000.0, 50.0));

        assertEquals("bid2", auction.getCurrentWinnerId());
        assertEquals(950.0, auction.getCurrentHighestBid());
        assertFalse(auction.getAutoBidConfig("bid1").isActive());
        assertTrue(auction.getAutoBidConfig("bid2").isActive());
    }

    @Test
    void testAutoBidFinalPriceUsesWinnerIncrement() {
        LiveAuction auction = new LiveAuction("auc1", "item1", "seller1",
                LocalDateTime.now().plusHours(1), AuctionStatus.RUNNING, 500.0, null, 60, 120);

        auction.addAutoBid(new AutoBidConfig("bid1", "user1", 900.0, 100.0));
        auction.addAutoBid(new AutoBidConfig("bid2", "user2", 1000.0, 25.0));

        assertEquals("bid2", auction.getCurrentWinnerId());
        assertEquals(925.0, auction.getCurrentHighestBid());
        assertFalse(auction.getAutoBidConfig("bid1").isActive());
        assertTrue(auction.getAutoBidConfig("bid2").isActive());
    }

    @Test
    void testAutoBidWaitingConfigRemainsActive() {
        LiveAuction auction = new LiveAuction("auc1", "item1", "seller1",
                LocalDateTime.now().plusHours(1), AuctionStatus.RUNNING, 500.0, null, 60, 120);

        auction.addAutoBid(new AutoBidConfig("bid1", "user1", 520.0, 50.0));

        assertEquals(500.0, auction.getCurrentHighestBid());
        assertTrue(auction.getAutoBidConfig("bid1").isActive());
    }

    @Test
    void testAutoBidCanBeEnabledAgainAfterInactive() throws Exception {
        LiveAuction auction = new LiveAuction("auc1", "item1", "seller1",
                LocalDateTime.now().plusHours(1), AuctionStatus.RUNNING, 500.0, null, 60, 120);

        auction.addAutoBid(new AutoBidConfig("bid1", "user1", 520.0, 50.0));
        auction.placeBid(new Bidder("bid2", "user2"), 530.0, null);

        assertFalse(auction.getAutoBidConfig("bid1").isActive());

        auction.addAutoBid(new AutoBidConfig("bid1", "user1", 700.0, 50.0));

        assertEquals("bid1", auction.getCurrentWinnerId());
        assertEquals(580.0, auction.getCurrentHighestBid());
        assertTrue(auction.getAutoBidConfig("bid1").isActive());
    }
}
