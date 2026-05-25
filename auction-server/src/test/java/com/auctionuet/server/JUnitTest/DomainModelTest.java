package com.auctionuet.server.JUnitTest;

import com.auctionuet.protocol.enums.AuctionStatus;
import com.auctionuet.protocol.enums.BidType;
import com.auctionuet.server.domain.model.AutoBidConfig;
import com.auctionuet.server.domain.model.Bidder;
import com.auctionuet.server.domain.model.LiveAuction;
import com.auctionuet.server.domain.model.Seller;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.exception.AuctionClosedException;
import com.auctionuet.server.exception.InvalidBidException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void testAutoBidRequiresCurrentLeader() {
        LiveAuction auction = new LiveAuction("auc1", "item1", "seller1",
                LocalDateTime.now().plusHours(1), AuctionStatus.RUNNING, 500.0, null, 60, 120);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> auction.addAutoBid(new AutoBidConfig("bid1", "user1", 1000.0, 50.0)));
        assertEquals("chỉ người dẫn đầu mới được bật Autobid", exception.getMessage());
        assertEquals(500.0, auction.getCurrentHighestBid());
        assertNull(auction.getCurrentWinnerId());
    }

    @Test
    void testCurrentLeaderCanEnableAutoBid() throws Exception {
        LiveAuction auction = new LiveAuction("auc1", "item1", "seller1",
                LocalDateTime.now().plusHours(1), AuctionStatus.RUNNING, 500.0, null, 60, 120);

        auction.placeBid(new Bidder("bid1", "user1"), 550.0, null);
        auction.addAutoBid(new AutoBidConfig("bid1", "user1", 1000.0, 50.0));

        assertEquals("bid1", auction.getCurrentWinnerId());
        assertEquals(550.0, auction.getCurrentHighestBid());
        assertTrue(auction.getAutoBidConfig("bid1").isActive());
        assertEquals(1, auction.getBidHistory().size());
    }

    @Test
    void testCurrentLeaderMustBeStableBeforeEnablingAutoBid() throws Exception {
        LocalDateTime baseTime = LocalDateTime.of(2026, 1, 1, 10, 0);
        AtomicReference<LocalDateTime> now = new AtomicReference<>(baseTime);
        LiveAuction auction = new LiveAuction("auc1", "item1", "seller1",
                baseTime.plusHours(1), AuctionStatus.RUNNING, 500.0, null, 60, 120,
                500.0, null, now::get);

        auction.placeBid(new Bidder("bid1", "user1"), 550.0, null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> auction.addAutoBid(
                        new AutoBidConfig("bid1", "user1", 1000.0, 50.0),
                        null,
                        Duration.ofSeconds(3)));
        assertEquals("Bạn cần dẫn đầu ít nhất 3 giây để bật Auto-Bid", exception.getMessage());

        now.set(baseTime.plusSeconds(3));
        auction.addAutoBid(
                new AutoBidConfig("bid1", "user1", 1000.0, 50.0),
                null,
                Duration.ofSeconds(3));

        assertTrue(auction.getAutoBidConfig("bid1").isActive());
    }

    @Test
    void testAutoBidProtectsLeaderWithinMaxBid() throws Exception {
        LiveAuction auction = new LiveAuction("auc1", "item1", "seller1",
                LocalDateTime.now().plusHours(1), AuctionStatus.RUNNING, 500.0, null, 60, 120);

        auction.placeBid(new Bidder("bid1", "user1"), 550.0, null);
        auction.addAutoBid(new AutoBidConfig("bid1", "user1", 1000.0, 50.0));
        LiveAuction.BidPlacementResult result = auction.placeBid(new Bidder("bid2", "user2"), 950.0);

        assertEquals("bid2", auction.getCurrentWinnerId());
        assertEquals(950.0, auction.getCurrentHighestBid());
        assertTrue(auction.getAutoBidConfig("bid1").isActive());

        auction.resolvePendingAutoBid(result.pendingAutoBid().sequenceId(), null);

        assertEquals("bid1", auction.getCurrentWinnerId());
        assertEquals(1000.0, auction.getCurrentHighestBid());
        assertTrue(auction.getAutoBidConfig("bid1").isActive());
        assertEquals(BidType.AUTO, auction.getBidHistory().get(2).getBidType());
    }

    @Test
    void testAutoBidBecomesInactiveWhenFullIncrementWouldExceedMaxBid() throws Exception {
        LiveAuction auction = new LiveAuction("auc1", "item1", "seller1",
                LocalDateTime.now().plusHours(1), AuctionStatus.RUNNING, 500.0, null, 60, 120);

        auction.placeBid(new Bidder("bid1", "user1"), 550.0, null);
        auction.addAutoBid(new AutoBidConfig("bid1", "user1", 1000.0, 50.0));
        LiveAuction.BidPlacementResult result = auction.placeBid(new Bidder("bid2", "user2"), 980.0);

        auction.resolvePendingAutoBid(result.pendingAutoBid().sequenceId(), null);

        assertEquals("bid2", auction.getCurrentWinnerId());
        assertEquals(980.0, auction.getCurrentHighestBid());
        assertFalse(auction.getAutoBidConfig("bid1").isActive());
        assertEquals(2, auction.getBidHistory().size());
    }

    @Test
    void testAutoBidBecomesInactiveWhenManualBidMatchesMaxBid() throws Exception {
        LiveAuction auction = new LiveAuction("auc1", "item1", "seller1",
                LocalDateTime.now().plusHours(1), AuctionStatus.RUNNING, 500.0, null, 60, 120);

        auction.placeBid(new Bidder("bid1", "user1"), 550.0, null);
        auction.addAutoBid(new AutoBidConfig("bid1", "user1", 1000.0, 50.0));
        LiveAuction.BidPlacementResult result = auction.placeBid(new Bidder("bid2", "user2"), 1000.0);

        assertEquals("bid2", auction.getCurrentWinnerId());
        assertEquals(1000.0, auction.getCurrentHighestBid());

        auction.resolvePendingAutoBid(result.pendingAutoBid().sequenceId(), null);

        assertEquals("bid2", auction.getCurrentWinnerId());
        assertEquals(1000.0, auction.getCurrentHighestBid());
        assertFalse(auction.getAutoBidConfig("bid1").isActive());
        assertEquals(2, auction.getBidHistory().size());
    }

    @Test
    void testAutoBidBecomesInactiveWhenManualBidExceedsMaxBid() throws Exception {
        LiveAuction auction = new LiveAuction("auc1", "item1", "seller1",
                LocalDateTime.now().plusHours(1), AuctionStatus.RUNNING, 500.0, null, 60, 120);

        auction.placeBid(new Bidder("bid1", "user1"), 550.0, null);
        auction.addAutoBid(new AutoBidConfig("bid1", "user1", 1000.0, 50.0));
        LiveAuction.BidPlacementResult result = auction.placeBid(new Bidder("bid2", "user2"), 1050.0);

        assertEquals("bid2", auction.getCurrentWinnerId());
        assertEquals(1050.0, auction.getCurrentHighestBid());
        assertTrue(auction.getAutoBidConfig("bid1").isActive());

        auction.resolvePendingAutoBid(result.pendingAutoBid().sequenceId(), null);

        assertFalse(auction.getAutoBidConfig("bid1").isActive());

        LiveAuction.BidPlacementResult nextBid = auction.placeBid(new Bidder("bid3", "user3"), 1100.0);

        assertNull(nextBid.pendingAutoBid());
        assertEquals("bid3", auction.getCurrentWinnerId());
    }

    @Test
    void testAutoBidCanBeEnabledAgainAfterBidderRegainsLead() throws Exception {
        LiveAuction auction = new LiveAuction("auc1", "item1", "seller1",
                LocalDateTime.now().plusHours(1), AuctionStatus.RUNNING, 500.0, null, 60, 120);

        auction.placeBid(new Bidder("bid1", "user1"), 550.0, null);
        auction.addAutoBid(new AutoBidConfig("bid1", "user1", 700.0, 50.0));
        LiveAuction.BidPlacementResult result = auction.placeBid(new Bidder("bid2", "user2"), 750.0);

        auction.resolvePendingAutoBid(result.pendingAutoBid().sequenceId(), null);

        assertFalse(auction.getAutoBidConfig("bid1").isActive());

        auction.placeBid(new Bidder("bid1", "user1"), 800.0, null);
        auction.addAutoBid(new AutoBidConfig("bid1", "user1", 1000.0, 50.0));

        assertEquals("bid1", auction.getCurrentWinnerId());
        assertEquals(800.0, auction.getCurrentHighestBid());
        assertTrue(auction.getAutoBidConfig("bid1").isActive());
    }

    @Test
    void testLatestManualBidReplacesPendingAutoBidTarget() throws Exception {
        LiveAuction auction = new LiveAuction("auc1", "item1", "seller1",
                LocalDateTime.now().plusHours(1), AuctionStatus.RUNNING, 500.0, null, 60, 120);

        auction.placeBid(new Bidder("bid1", "user1"), 550.0, null);
        auction.addAutoBid(new AutoBidConfig("bid1", "user1", 1000.0, 50.0));
        LiveAuction.BidPlacementResult firstPending = auction.placeBid(new Bidder("bid2", "user2"), 600.0);
        LiveAuction.BidPlacementResult latestPending = auction.placeBid(new Bidder("bid3", "user3"), 700.0);

        auction.resolvePendingAutoBid(firstPending.pendingAutoBid().sequenceId(), null);

        assertEquals("bid3", auction.getCurrentWinnerId());
        assertEquals(700.0, auction.getCurrentHighestBid());

        auction.resolvePendingAutoBid(latestPending.pendingAutoBid().sequenceId(), null);

        assertEquals("bid1", auction.getCurrentWinnerId());
        assertEquals(750.0, auction.getCurrentHighestBid());
        assertTrue(auction.getAutoBidConfig("bid1").isActive());
    }

    @Test
    void testPendingAutoBidDoesNothingAfterAuctionFinished() throws Exception {
        LiveAuction auction = new LiveAuction("auc1", "item1", "seller1",
                LocalDateTime.now().plusHours(1), AuctionStatus.RUNNING, 500.0, null, 60, 120);

        auction.placeBid(new Bidder("bid1", "user1"), 550.0, null);
        auction.addAutoBid(new AutoBidConfig("bid1", "user1", 1000.0, 50.0));
        LiveAuction.BidPlacementResult pending = auction.placeBid(new Bidder("bid2", "user2"), 600.0);

        auction.setStatus(AuctionStatus.FINISHED);
        LiveAuction.AutoBidResolution resolution =
                auction.resolvePendingAutoBid(pending.pendingAutoBid().sequenceId(), null);

        assertNull(resolution.autoBid());
        assertFalse(resolution.autoBidDeactivated());
        assertEquals("bid2", auction.getCurrentWinnerId());
        assertEquals(600.0, auction.getCurrentHighestBid());
        assertTrue(auction.getAutoBidConfig("bid1").isActive());
    }
}
