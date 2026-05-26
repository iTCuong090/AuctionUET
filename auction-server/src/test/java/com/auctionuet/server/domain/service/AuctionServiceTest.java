package com.auctionuet.server.domain.service;

import com.auctionuet.protocol.dto.response.auction.AuctionDTO;
import com.auctionuet.protocol.dto.response.bid.AutoBidConfigDTO;
import com.auctionuet.protocol.dto.response.item.ItemDTO;
import com.auctionuet.protocol.dto.response.user.UserDTO;
import com.auctionuet.protocol.enums.AutoBidStatus;
import com.auctionuet.protocol.enums.AuctionStatus;
import com.auctionuet.protocol.enums.BidType;
import com.auctionuet.protocol.enums.ItemCondition;
import com.auctionuet.protocol.enums.ItemApprovalStatus;
import com.auctionuet.protocol.enums.ItemType;
import com.auctionuet.protocol.enums.Permission;
import com.auctionuet.protocol.enums.TransactionType;
import com.auctionuet.protocol.enums.UserRole;
import com.auctionuet.server.domain.manager.AuctionManager;
import com.auctionuet.server.domain.model.Admin;
import com.auctionuet.server.domain.model.AuctionObserver;
import com.auctionuet.server.domain.model.AutoBidConfig;
import com.auctionuet.server.domain.model.BidRecord;
import com.auctionuet.server.domain.model.LiveAuction;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.exception.AuctionException;
import com.auctionuet.server.persistence.dao.AuctionDAO;
import com.auctionuet.server.persistence.dao.BidDAO;
import com.auctionuet.server.persistence.dao.ItemDAO;
import com.auctionuet.server.persistence.dao.SystemSettingDAO;
import com.auctionuet.server.persistence.dao.TransactionDAO;
import com.auctionuet.server.persistence.dao.UserDAO;
import com.auctionuet.server.persistence.schema.AuctionSchema;
import com.auctionuet.server.persistence.schema.BidSchema;
import com.auctionuet.server.persistence.schema.ItemSchema;
import com.auctionuet.server.persistence.schema.TransactionSchema;
import com.auctionuet.server.persistence.schema.UserSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuctionServiceTest {

    private ItemDAO itemDAO;
    private AuctionDAO auctionDAO;
    private UserDAO userDAO;
    private BidDAO bidDAO;
    private TransactionDAO transactionDAO;
    private SystemSettingDAO systemSettingDAO;
    private ItemService itemService;
    private AuctionService auctionService;
    private BidService bidService;
    private WalletService walletService;

    private final String itemFile = "data/test_items_service.json";
    private final String auctionFile = "data/test_auctions_service.json";
    private final String userFile = "data/test_users_service.json";
    private final String bidFile = "data/test_bids_service.json";
    private final String transactionFile = "data/test_transactions_service.json";
    private final String settingFile = "data/test_settings_service.json";

    @BeforeEach
    public void setup() {
        deleteTestFiles();

        itemDAO = new ItemDAO(itemFile);
        auctionDAO = new AuctionDAO(auctionFile);
        userDAO = new UserDAO(userFile);
        bidDAO = new BidDAO(bidFile);
        transactionDAO = new TransactionDAO(transactionFile);
        systemSettingDAO = new SystemSettingDAO(settingFile);
        systemSettingDAO.setItemApprovalEnabled(false);

        UserService userService = new UserService(userDAO);
        itemService = new ItemService(itemDAO, userService, auctionDAO, systemSettingDAO);
        TransactionService transactionService = new TransactionService(transactionDAO);
        walletService = new WalletService(userDAO, userService, transactionService);
        bidService = new BidService(
                AuctionManager.getInstance(),
                walletService,
                bidDAO,
                itemService,
                auctionDAO,
                userService,
                Duration.ZERO,
                Duration.ZERO);
        auctionService = new AuctionService(itemService, bidService, userService, auctionDAO, walletService);

        saveUser("seller1", "seller", UserRole.SELLER);
        saveUser("seller2", "seller2", UserRole.SELLER);
        saveUser("bidder1", "bidder", UserRole.BIDDER);
    }

    @AfterEach
    public void teardown() {
        deleteTestFiles();
    }

    class MockUser extends User {
        private final boolean isSeller;

        public MockUser(String id, String username, UserRole role, boolean isSeller) {
            super(id, username, role);
            this.isSeller = isSeller;
        }

        @Override
        public boolean hasPermission(Permission action) {
            if (action == Permission.CREATE_ITEM
                    || action == Permission.CREATE_AUCTION
                    || action == Permission.DELETE_ITEM) {
                return isSeller;
            }
            return false;
        }

        @Override
        public String getDisplayInfo() {
            return getUsername();
        }
    }

    private ItemDTO createElectronicsDTO() {
        Map<String, Object> extra = new HashMap<>();
        extra.put("brand", "Samsung");
        extra.put("warrantyMonths", 12);
        return new ItemDTO(
                "input-item",
                "Phone",
                null,
                500.0,
                ItemType.ELECTRONICS,
                new UserDTO("seller1", "seller", UserRole.SELLER),
                null,
                ItemCondition.NEW,
                extra);
    }

    @Test
    public void testCreateItemBySeller() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        ItemDTO dto = createElectronicsDTO();

        ItemDTO item = itemService.createItem(seller, dto.getName(), dto.getDescription(), dto.getStartingPrice(),
                dto.getType(), dto.getImageUrl(), dto.getCondition(), dto.getExtraFields());
        assertNotNull(item);
        assertEquals("Phone", item.getName());
        assertEquals("seller", item.getSeller().getUsername());

        ItemSchema saved = itemDAO.findById(item.getId());
        assertNotNull(saved);
        assertEquals(ItemApprovalStatus.APPROVED, saved.getApprovalStatus());
    }

    @Test
    public void testPendingItemCannotCreateAuctionUntilApproved() throws Exception {
        systemSettingDAO.setItemApprovalEnabled(true);
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        ItemDTO dto = createElectronicsDTO();
        ItemDTO item = itemService.createItem(seller, dto.getName(), dto.getDescription(), dto.getStartingPrice(),
                dto.getType(), dto.getImageUrl(), dto.getCondition(), dto.getExtraFields());

        assertEquals(ItemApprovalStatus.PENDING, item.getApprovalStatus());
        assertThrows(AuctionException.class, () ->
                auctionService.createAuction(seller, item.getId(), LocalDateTime.now().plusMinutes(5),
                        LocalDateTime.now().plusHours(1), "Pending auction", "Desc", 60, 120));

        itemService.updateApprovalStatus(item.getId(), ItemApprovalStatus.APPROVED);
        AuctionDTO auction = auctionService.createAuction(
                seller, item.getId(), LocalDateTime.now().plusMinutes(5),
                LocalDateTime.now().plusHours(1), "Approved auction", "Desc", 60, 120);
        assertEquals(AuctionStatus.OPEN, auction.getStatus());
    }

    @Test
    public void testDisablingApprovalApprovesPendingButKeepsRejectedItems() throws Exception {
        systemSettingDAO.setItemApprovalEnabled(true);
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        ItemDTO dto = createElectronicsDTO();
        ItemDTO pending = itemService.createItem(seller, "Pending", dto.getDescription(), dto.getStartingPrice(),
                dto.getType(), dto.getImageUrl(), dto.getCondition(), dto.getExtraFields());
        ItemDTO rejected = itemService.createItem(seller, "Rejected", dto.getDescription(), dto.getStartingPrice(),
                dto.getType(), dto.getImageUrl(), dto.getCondition(), dto.getExtraFields());
        itemService.updateApprovalStatus(rejected.getId(), ItemApprovalStatus.REJECTED);

        itemService.updateApprovalSettings(false);

        assertEquals(ItemApprovalStatus.APPROVED,
                itemDAO.findById(pending.getId()).getApprovalStatus());
        assertEquals(ItemApprovalStatus.REJECTED,
                itemDAO.findById(rejected.getId()).getApprovalStatus());
        assertFalse(itemService.getApprovalSettings().isEnabled());
    }

    @Test
    public void testCreateItemByBidder() {
        User bidder = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        ItemDTO dto = createElectronicsDTO();

        assertThrows(AuctionException.class, () ->
                itemService.createItem(bidder, dto.getName(), dto.getDescription(), dto.getStartingPrice(),
                        dto.getType(), dto.getImageUrl(), dto.getCondition(), dto.getExtraFields()));
    }

    @Test
    public void testGetItemsBySeller() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        ItemDTO dto1 = createElectronicsDTO();
        Map<String, Object> dto2Extra = new HashMap<>();
        dto2Extra.put("brand", "Dell");
        dto2Extra.put("warrantyMonths", 24);
        ItemDTO dto2 = new ItemDTO(
                "input-item-2",
                "Laptop",
                null,
                1500.0,
                ItemType.ELECTRONICS,
                new UserDTO("seller1", "seller", UserRole.SELLER),
                null,
                ItemCondition.GOOD,
                dto2Extra);

        itemService.createItem(seller, dto1.getName(), dto1.getDescription(), dto1.getStartingPrice(),
                dto1.getType(), dto1.getImageUrl(), dto1.getCondition(), dto1.getExtraFields());
        itemService.createItem(seller, dto2.getName(), dto2.getDescription(), dto2.getStartingPrice(),
                dto2.getType(), dto2.getImageUrl(), dto2.getCondition(), dto2.getExtraFields());

        List<ItemDTO> myItems = itemService.getItemsBySellerId("seller1");
        assertEquals(2, myItems.size());

        List<ItemDTO> otherItems = itemService.getItemsBySellerId("seller2");
        assertEquals(0, otherItems.size());
    }

    @Test
    public void testCreateAuction() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        ItemDTO dto = createElectronicsDTO();
        ItemDTO item = itemService.createItem(seller, dto.getName(), dto.getDescription(), dto.getStartingPrice(),
                dto.getType(), dto.getImageUrl(), dto.getCondition(), dto.getExtraFields());

        AuctionDTO auction = auctionService.createAuction(seller, item.getId(), LocalDateTime.now().plusMinutes(5),
                LocalDateTime.now().plusDays(1), "Auction 1", "Desc", 60, 120);
        assertNotNull(auction);
        assertEquals(AuctionStatus.OPEN, auction.getStatus());
        assertEquals("seller", auction.getSeller().getUsername());
        assertEquals(item.getStartingPrice(), auction.getCurrentPrice());
    }

    @Test
    public void testCreateAuctionAllowsStartTimeJustNow() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        ItemDTO dto = createElectronicsDTO();
        ItemDTO item = itemService.createItem(seller, dto.getName(), dto.getDescription(), dto.getStartingPrice(),
                dto.getType(), dto.getImageUrl(), dto.getCondition(), dto.getExtraFields());

        AuctionDTO auction = auctionService.createAuction(seller, item.getId(), LocalDateTime.now().minusSeconds(2),
                LocalDateTime.now().plusHours(1), "Auction now", "Desc", 60, 120);

        assertNotNull(auction);
        AuctionSchema saved = auctionDAO.findById(auction.getId());
        assertNotNull(saved);
        assertFalse(saved.getStartTime().isBefore(LocalDateTime.now().minusSeconds(10)));
    }

    @Test
    public void testCreateAuctionRejectsStartTimeTooFarInPast() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        ItemDTO dto = createElectronicsDTO();
        ItemDTO item = itemService.createItem(seller, dto.getName(), dto.getDescription(), dto.getStartingPrice(),
                dto.getType(), dto.getImageUrl(), dto.getCondition(), dto.getExtraFields());

        assertThrows(AuctionException.class, () ->
                auctionService.createAuction(seller, item.getId(), LocalDateTime.now().minusMinutes(1),
                        LocalDateTime.now().plusHours(1), "Auction past", "Desc", 60, 120));
    }

    @Test
    public void testCreateAuctionRejectsItemWithActiveAuction() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        ItemDTO dto = createElectronicsDTO();
        ItemDTO item = itemService.createItem(seller, dto.getName(), dto.getDescription(), dto.getStartingPrice(),
                dto.getType(), dto.getImageUrl(), dto.getCondition(), dto.getExtraFields());

        auctionService.createAuction(seller, item.getId(), LocalDateTime.now().plusMinutes(5),
                LocalDateTime.now().plusDays(1), "Auction 1", "Desc", 60, 120);

        assertThrows(AuctionException.class, () ->
                auctionService.createAuction(seller, item.getId(), LocalDateTime.now().plusMinutes(10),
                        LocalDateTime.now().plusDays(2), "Auction 2", "Desc", 60, 120));
    }

    @Test
    public void testCreateAuctionAllowsCanceledPreviousAuction() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        ItemDTO dto = createElectronicsDTO();
        ItemDTO item = itemService.createItem(seller, dto.getName(), dto.getDescription(), dto.getStartingPrice(),
                dto.getType(), dto.getImageUrl(), dto.getCondition(), dto.getExtraFields());

        AuctionDTO firstAuction = auctionService.createAuction(
                seller,
                item.getId(),
                LocalDateTime.now().plusMinutes(5),
                LocalDateTime.now().plusDays(1),
                "Auction 1",
                "Desc",
                60,
                120);
        auctionService.startAuction(seller, firstAuction.getId());
        auctionService.endAuction(firstAuction.getId());

        AuctionDTO secondAuction = auctionService.createAuction(
                seller,
                item.getId(),
                LocalDateTime.now().plusMinutes(10),
                LocalDateTime.now().plusDays(2),
                "Auction 2",
                "Desc",
                60,
                120);

        assertNotNull(secondAuction);
        assertEquals(AuctionStatus.CANCELED, auctionDAO.findById(firstAuction.getId()).getStatus());
        assertEquals(AuctionStatus.OPEN, auctionDAO.findById(secondAuction.getId()).getStatus());
        assertEquals(2, auctionDAO.findByItemId(item.getId()).size());
    }

    @Test
    public void testCreateAuctionWrongOwner() throws Exception {
        User seller1 = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User seller2 = new MockUser("seller2", "seller2", UserRole.SELLER, true);
        ItemDTO dto = createElectronicsDTO();
        ItemDTO item = itemService.createItem(seller1, dto.getName(), dto.getDescription(), dto.getStartingPrice(),
                dto.getType(), dto.getImageUrl(), dto.getCondition(), dto.getExtraFields());

        assertThrows(AuctionException.class, () ->
                auctionService.createAuction(seller2, item.getId(), LocalDateTime.now().plusMinutes(5),
                        LocalDateTime.now().plusDays(1), "Auction 1", "Desc", 60, 120));
    }

    @Test
    public void testStartAuction() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        ItemDTO dto = createElectronicsDTO();
        ItemDTO item = itemService.createItem(seller, dto.getName(), dto.getDescription(), dto.getStartingPrice(),
                dto.getType(), dto.getImageUrl(), dto.getCondition(), dto.getExtraFields());

        AuctionDTO auction = auctionService.createAuction(seller, item.getId(), LocalDateTime.now().plusMinutes(5),
                LocalDateTime.now().plusDays(1), "Auction 1", "Desc", 60, 120);
        auctionService.startAuction(seller, auction.getId());

        AuctionSchema updated = auctionDAO.findById(auction.getId());
        assertEquals(AuctionStatus.RUNNING, updated.getStatus());
    }

    @Test
    public void testStartAuctionAlreadyRunning() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        ItemDTO dto = createElectronicsDTO();
        ItemDTO item = itemService.createItem(seller, dto.getName(), dto.getDescription(), dto.getStartingPrice(),
                dto.getType(), dto.getImageUrl(), dto.getCondition(), dto.getExtraFields());

        AuctionDTO auction = auctionService.createAuction(seller, item.getId(), LocalDateTime.now().plusMinutes(5),
                LocalDateTime.now().plusDays(1), "Auction 1", "Desc", 60, 120);
        auctionService.startAuction(seller, auction.getId());

        assertThrows(AuctionException.class, () -> auctionService.startAuction(seller, auction.getId()));
    }

    @Test
    public void testGetAuctions() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        ItemDTO dto = createElectronicsDTO();
        for (int i = 1; i <= 3; i++) {
            ItemDTO item = itemService.createItem(seller, dto.getName() + " " + i, dto.getDescription(),
                    dto.getStartingPrice(), dto.getType(), dto.getImageUrl(), dto.getCondition(), dto.getExtraFields());
            auctionService.createAuction(seller, item.getId(), LocalDateTime.now().plusMinutes(5),
                    LocalDateTime.now().plusDays(1), "Auction " + i, "Desc", 60, 120);
        }

        List<AuctionDTO> auctions = auctionService.getAuctions();
        assertEquals(3, auctions.size());
    }

    @Test
    public void testEndAuction() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        ItemDTO dto = createElectronicsDTO();
        ItemDTO item = itemService.createItem(seller, dto.getName(), dto.getDescription(), dto.getStartingPrice(),
                dto.getType(), dto.getImageUrl(), dto.getCondition(), dto.getExtraFields());

        AuctionDTO auction = auctionService.createAuction(seller, item.getId(), LocalDateTime.now().plusMinutes(5),
                LocalDateTime.now().plusDays(1), "Auction 1", "Desc", 60, 120);
        auctionService.startAuction(seller, auction.getId());

        auctionService.endAuction(auction.getId());

        AuctionSchema updated = auctionDAO.findById(auction.getId());
        assertEquals(AuctionStatus.CANCELED, updated.getStatus());
    }

    @Test
    public void testLeaderSetAutoBidProtectsExistingLead() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        setBalance("bidder1", 1000.0);

        AuctionDTO auction = createRunningAuction(seller, 500.0);

        bidService.placeBid(bidder, auction.getId(), 550.0);
        bidService.setAutoBid(bidder, auction.getId(), 1000.0, 50.0);

        AuctionSchema updated = auctionDAO.findById(auction.getId());
        assertEquals(550.0, updated.getHighestBid());
        assertEquals("bidder1", updated.getWinnerId());
        assertTrue(updated.hasDepositedBidder("bidder1"));

        UserSchema bidderSchema = userDAO.findById("bidder1");
        assertEquals(950.0, bidderSchema.getBalance());
        assertEquals(50.0, bidderSchema.getFrozenBalance());

        List<BidSchema> bids = bidDAO.findByAuctionId(auction.getId());
        assertEquals(1, bids.size());
        assertEquals(BidType.MANUAL, bids.get(0).getBidType());

        AutoBidConfigDTO state = bidService.getAutoBidConfigDTO(bidder, auction.getId());
        assertEquals(AutoBidStatus.PROTECTING, state.getStatus());
        assertEquals(1000.0, state.getProtectedUntil());
    }

    @Test
    public void testLeaderMustBeStableBeforeSettingAutoBid() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        setBalance("bidder1", 1000.0);

        AuctionDTO auction = createRunningAuction(seller, 500.0);
        bidService.placeBid(bidder, auction.getId(), 550.0);

        BidService strictBidService = new BidService(
                AuctionManager.getInstance(),
                walletService,
                bidDAO,
                itemService,
                auctionDAO,
                new UserService(userDAO),
                Duration.ZERO,
                Duration.ofSeconds(3));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> strictBidService.setAutoBid(bidder, auction.getId(), 1000.0, 50.0));
        assertEquals("Bạn cần dẫn đầu ít nhất 3 giây để bật Auto-Bid", exception.getMessage());

        UserSchema bidderSchema = userDAO.findById("bidder1");
        assertEquals(950.0, bidderSchema.getBalance());
        assertEquals(50.0, bidderSchema.getFrozenBalance());
        assertNull(strictBidService.getAutoBidConfigDTO(bidder, auction.getId()));
        assertEquals(1, bidDAO.findByAuctionId(auction.getId()).size());
    }

    @Test
    public void testReconcileDoesNotResetLiveAutoBidStateOrLeaderSince() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        setBalance("bidder1", 1000.0);

        AuctionDTO auction = createRunningAuction(seller, 500.0);
        bidService.placeBid(bidder, auction.getId(), 550.0);
        bidService.setAutoBid(bidder, auction.getId(), 1000.0, 50.0);

        LiveAuction before = AuctionManager.getInstance().getAuction(auction.getId());
        LocalDateTime leaderSince = before.getCurrentLeaderSince();

        auctionService.reconcileAuctionsFromDatabase();

        LiveAuction after = AuctionManager.getInstance().getAuction(auction.getId());
        assertSame(before, after);
        assertEquals(leaderSince, after.getCurrentLeaderSince());
        assertNotNull(after.getAutoBidConfig("bidder1"));
        assertTrue(after.getAutoBidConfig("bidder1").isActive());
    }

    @Test
    public void testReconcileDoesNotClearPendingAutoBid() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder1 = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        User bidder2 = new MockUser("bidder2", "bidder2", UserRole.BIDDER, false);
        saveUser("bidder2", "bidder2", UserRole.BIDDER, 1000.0);
        setBalance("bidder1", 1000.0);

        AuctionDTO auction = createRunningAuction(seller, 500.0);
        bidService.placeBid(bidder1, auction.getId(), 550.0);

        LiveAuction liveAuction = AuctionManager.getInstance().getAuction(auction.getId());
        liveAuction.addAutoBid(new AutoBidConfig("bidder1", "bidder", 1000.0, 50.0));
        LiveAuction.BidPlacementResult pending = liveAuction.placeBid(bidder2, 600.0);

        auctionService.reconcileAuctionsFromDatabase();

        LiveAuction after = AuctionManager.getInstance().getAuction(auction.getId());
        assertSame(liveAuction, after);
        after.resolvePendingAutoBid(pending.pendingAutoBid().sequenceId(), null);

        assertEquals("bidder1", after.getCurrentWinnerId());
        assertEquals(650.0, after.getCurrentHighestBid());
    }

    @Test
    public void testHydrateRestoresLeaderSinceFromWinningBidHistory() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        setBalance("bidder1", 1000.0);

        AuctionDTO auction = createRunningAuction(seller, 500.0);
        bidService.placeBid(bidder, auction.getId(), 550.0);
        LocalDateTime winningBidTime = bidDAO.findByAuctionId(auction.getId()).get(0).getTimestamp();

        AuctionManager.getInstance().removeLiveAuction(auction.getId());
        auctionService.reconcileAuctionsFromDatabase();

        LiveAuction restored = AuctionManager.getInstance().getAuction(auction.getId());
        assertNotNull(restored);
        assertEquals(winningBidTime, restored.getCurrentLeaderSince());
    }

    @Test
    public void testNonLeaderSetAutoBidDoesNotCreateConfigOrDeposit() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        setBalance("bidder1", 40.0);

        AuctionDTO auction = createRunningAuction(seller, 500.0);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bidService.setAutoBid(bidder, auction.getId(), 1000.0, 50.0));
        assertEquals("chỉ người dẫn đầu mới được bật Autobid", exception.getMessage());

        assertEquals(0, bidDAO.findByAuctionId(auction.getId()).size());
        assertFalse(AuctionManager.getInstance().getAuction(auction.getId()).hasDeposited("bidder1"));
        assertFalse(auctionDAO.findById(auction.getId()).hasDepositedBidder("bidder1"));

        UserSchema bidderSchema = userDAO.findById("bidder1");
        assertEquals(40.0, bidderSchema.getBalance());
        assertEquals(0.0, bidderSchema.getFrozenBalance());
    }

    @Test
    public void testInvalidManualBidRollsBackDeposit() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        setBalance("bidder1", 1000.0);

        AuctionDTO auction = createRunningAuction(seller, 500.0);

        assertThrows(Exception.class, () -> bidService.placeBid(bidder, auction.getId(), 500.0));

        UserSchema bidderSchema = userDAO.findById("bidder1");
        assertEquals(1000.0, bidderSchema.getBalance());
        assertEquals(0.0, bidderSchema.getFrozenBalance());
        assertFalse(AuctionManager.getInstance().getAuction(auction.getId()).hasDeposited("bidder1"));
        assertFalse(auctionDAO.findById(auction.getId()).hasDepositedBidder("bidder1"));
    }

    @Test
    public void testSetAutoBidDoesNotCreateExtraBidRecord() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        setBalance("bidder1", 1000.0);

        AuctionDTO auction = createRunningAuction(seller, 500.0);

        bidService.placeBid(bidder, auction.getId(), 550.0);
        bidService.setAutoBid(bidder, auction.getId(), 1000.0, 50.0);

        assertEquals(1, bidDAO.findByAuctionId(auction.getId()).size());
        assertTrue(auctionDAO.findById(auction.getId()).hasDepositedBidder("bidder1"));

        UserSchema bidderSchema = userDAO.findById("bidder1");
        assertEquals(950.0, bidderSchema.getBalance());
        assertEquals(50.0, bidderSchema.getFrozenBalance());

        AutoBidConfigDTO state = bidService.getAutoBidConfigDTO(bidder, auction.getId());
        assertEquals(AutoBidStatus.PROTECTING, state.getStatus());
        assertEquals(1000.0, state.getProtectedUntil());
    }

    @Test
    public void testCheckAutoBidReturnsInactiveForOutbidConfig() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder1 = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        User bidder2 = new MockUser("bidder2", "bidder2", UserRole.BIDDER, false);
        setBalance("bidder1", 1000.0);
        saveUser("bidder2", "bidder2", UserRole.BIDDER, 1000.0);

        AuctionDTO auction = createRunningAuction(seller, 500.0);

        bidService.placeBid(bidder1, auction.getId(), 550.0);
        bidService.setAutoBid(bidder1, auction.getId(), 900.0, 50.0);
        bidService.placeBid(bidder2, auction.getId(), 950.0);

        AutoBidConfigDTO bidder1State = bidService.getAutoBidConfigDTO(bidder1, auction.getId());
        AutoBidConfigDTO bidder2State = bidService.getAutoBidConfigDTO(bidder2, auction.getId());

        assertEquals(AutoBidStatus.INEFFECTIVE, bidder1State.getStatus());
        assertEquals(0.0, bidder1State.getProtectedUntil());
        assertNull(bidder2State);
    }

    @Test
    public void testNonLeaderCannotSetAutoBidOverCurrentLeader() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder1 = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        User bidder2 = new MockUser("bidder2", "bidder2", UserRole.BIDDER, false);
        setBalance("bidder1", 1000.0);
        saveUser("bidder2", "bidder2", UserRole.BIDDER, 1000.0);

        AuctionDTO auction = createRunningAuction(seller, 500.0);

        bidService.placeBid(bidder1, auction.getId(), 550.0);
        bidService.setAutoBid(bidder1, auction.getId(), 1000.0, 50.0);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bidService.setAutoBid(bidder2, auction.getId(), 1200.0, 50.0));
        assertEquals("chỉ người dẫn đầu mới được bật Autobid", exception.getMessage());

        AuctionSchema updated = auctionDAO.findById(auction.getId());
        assertEquals("bidder1", updated.getWinnerId());
        assertEquals(550.0, updated.getHighestBid());

        AutoBidConfigDTO bidder1State = bidService.getAutoBidConfigDTO(bidder1, auction.getId());
        AutoBidConfigDTO bidder2State = bidService.getAutoBidConfigDTO(bidder2, auction.getId());
        assertEquals(AutoBidStatus.PROTECTING, bidder1State.getStatus());
        assertNull(bidder2State);
    }

    @Test
    public void testManualBidNearAutoBidMaxMakesAutoBidIneffective() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder1 = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        User bidder2 = new MockUser("bidder2", "bidder2", UserRole.BIDDER, false);
        setBalance("bidder1", 1000.0);
        saveUser("bidder2", "bidder2", UserRole.BIDDER, 1000.0);

        AuctionDTO auction = createRunningAuction(seller, 500.0);

        bidService.placeBid(bidder1, auction.getId(), 550.0);
        bidService.setAutoBid(bidder1, auction.getId(), 1000.0, 50.0);
        bidService.placeBid(bidder2, auction.getId(), 980.0);

        AuctionSchema updated = auctionDAO.findById(auction.getId());
        assertEquals("bidder2", updated.getWinnerId());
        assertEquals(980.0, updated.getHighestBid());

        AutoBidConfigDTO bidder1State = bidService.getAutoBidConfigDTO(bidder1, auction.getId());
        assertEquals(AutoBidStatus.INEFFECTIVE, bidder1State.getStatus());
        assertFalse(bidDAO.findByAuctionId(auction.getId()).stream()
                .anyMatch(bid -> bid.getBidType() == BidType.AUTO));
    }

    @Test
    public void testAdminCancelsOpenAuctionAndPersistsReason() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        Admin admin = new Admin("admin1", "admin");
        AuctionDTO auction = createOpenAuction(seller, 500.0);

        AuctionDTO canceled = auctionService.adminCancelAuction(admin, auction.getId(), "Thông tin sản phẩm sai");
        AuctionSchema saved = auctionDAO.findById(auction.getId());

        assertEquals(AuctionStatus.CANCELED, saved.getStatus());
        assertEquals("Thông tin sản phẩm sai", saved.getCanceledReason());
        assertEquals("admin1", saved.getCanceledByUserId());
        assertNotNull(saved.getCanceledAt());
        assertEquals(saved.getCanceledReason(), canceled.getCanceledReason());
        assertEquals(saved.getCanceledAt(), canceled.getCanceledAt());
    }

    @Test
    public void testAdminCancelsRunningAuctionAndRefundsAllDeposits() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder1 = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        User bidder2 = new MockUser("bidder2", "bidder2", UserRole.BIDDER, false);
        Admin admin = new Admin("admin1", "admin");
        saveUser("bidder2", "bidder2", UserRole.BIDDER, 1000.0);
        setBalance("bidder1", 1000.0);
        AuctionDTO auction = createRunningAuction(seller, 500.0);

        bidService.placeBid(bidder1, auction.getId(), 550.0);
        bidService.placeBid(bidder2, auction.getId(), 600.0);
        auctionService.adminCancelAuction(admin, auction.getId(), "Nghi ngờ đẩy giá ảo");

        UserSchema first = userDAO.findById("bidder1");
        UserSchema second = userDAO.findById("bidder2");
        AuctionSchema saved = auctionDAO.findById(auction.getId());
        assertEquals(1000.0, first.getBalance());
        assertEquals(0.0, first.getFrozenBalance());
        assertEquals(1000.0, second.getBalance());
        assertEquals(0.0, second.getFrozenBalance());
        assertEquals(AuctionStatus.CANCELED, saved.getStatus());
        assertFalse(saved.hasDepositedBidder("bidder1"));
        assertFalse(saved.hasDepositedBidder("bidder2"));
        assertEquals("bidder2", saved.getWinnerId());
        assertEquals(600.0, saved.getHighestBid());
        assertThrows(Exception.class, () -> bidService.placeBid(bidder1, auction.getId(), 650.0));
    }

    @Test
    public void testAdminCancelsWaitingPaymentAndRefundsWinnerDeposit() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        Admin admin = new Admin("admin1", "admin");
        setBalance("bidder1", 1000.0);
        AuctionDTO auction = createRunningAuction(seller, 500.0);

        bidService.placeBid(bidder, auction.getId(), 600.0);
        auctionService.endAuction(auction.getId());
        auctionService.adminCancelAuction(admin, auction.getId(), "Sản phẩm vi phạm");

        UserSchema winner = userDAO.findById("bidder1");
        AuctionSchema saved = auctionDAO.findById(auction.getId());
        assertEquals(1000.0, winner.getBalance());
        assertEquals(0.0, winner.getFrozenBalance());
        assertEquals(AuctionStatus.CANCELED, saved.getStatus());
        assertNull(saved.getPaymentDeadlineAt());
        assertEquals("seller1", itemDAO.findById(auction.getItem().getId()).getSellerId());
    }

    @Test
    public void testAdminCannotCancelPaidOrCanceledAuction() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        Admin admin = new Admin("admin1", "admin");
        setBalance("bidder1", 1000.0);
        AuctionDTO paidAuction = createRunningAuction(seller, 500.0);

        bidService.placeBid(bidder, paidAuction.getId(), 600.0);
        auctionService.endAuction(paidAuction.getId());
        auctionService.payAuction(bidder, paidAuction.getId());
        assertThrows(AuctionException.class,
                () -> auctionService.adminCancelAuction(admin, paidAuction.getId(), "Hủy muộn"));

        AuctionDTO canceledAuction = createOpenAuction(seller, 700.0);
        auctionService.adminCancelAuction(admin, canceledAuction.getId(), "Hủy hợp lệ");
        assertThrows(AuctionException.class,
                () -> auctionService.adminCancelAuction(admin, canceledAuction.getId(), "Hủy lại"));
    }

    @Test
    public void testNonAdminCannotCancelAuction() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        AuctionDTO auction = createOpenAuction(seller, 500.0);

        assertThrows(AuctionException.class,
                () -> auctionService.adminCancelAuction(seller, auction.getId(), "Không có quyền"));
        assertEquals(AuctionStatus.OPEN, auctionDAO.findById(auction.getId()).getStatus());
    }

    @Test
    public void testAdminCancellationNotifiesObservers() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        Admin admin = new Admin("admin1", "admin");
        AuctionDTO auction = createRunningAuction(seller, 500.0);
        AtomicReference<String> receivedReason = new AtomicReference<>();
        AuctionObserver observer = new AuctionObserver() {
            @Override
            public void onBidPlaced(String auctionId, BidRecord record) {
            }

            @Override
            public void onAuctionEnded(String auctionId, String winnerId, double finalPrice) {
            }

            @Override
            public void onAuctionExtended(String auctionId, LocalDateTime newEndTime) {
            }

            @Override
            public void onAuctionCanceled(String auctionId, String reason, LocalDateTime canceledAt) {
                receivedReason.set(reason);
            }
        };

        AuctionManager.getInstance().addObserver(auction.getId(), observer);
        try {
            auctionService.adminCancelAuction(admin, auction.getId(), "Vi phạm quy chế");
            assertEquals("Vi phạm quy chế", receivedReason.get());
        } finally {
            AuctionManager.getInstance().removeObserver(auction.getId(), observer);
        }
    }

    @Test
    public void testManualBidAtAutoBidMaxMakesAutoBidIneffective() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder1 = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        User bidder2 = new MockUser("bidder2", "bidder2", UserRole.BIDDER, false);
        setBalance("bidder1", 1000.0);
        saveUser("bidder2", "bidder2", UserRole.BIDDER, 1000.0);

        AuctionDTO auction = createRunningAuction(seller, 500.0);

        bidService.placeBid(bidder1, auction.getId(), 550.0);
        bidService.setAutoBid(bidder1, auction.getId(), 1000.0, 50.0);
        bidService.placeBid(bidder2, auction.getId(), 1000.0);

        AuctionSchema updated = auctionDAO.findById(auction.getId());
        assertEquals("bidder2", updated.getWinnerId());
        assertEquals(1000.0, updated.getHighestBid());

        AutoBidConfigDTO bidder1State = bidService.getAutoBidConfigDTO(bidder1, auction.getId());
        assertEquals(AutoBidStatus.INEFFECTIVE, bidder1State.getStatus());
        assertFalse(bidDAO.findByAuctionId(auction.getId()).stream()
                .anyMatch(bid -> bid.getBidType() == BidType.AUTO));
    }

    @Test
    public void testCheckAutoBidReturnsNullWhenMissingOrCanceled() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        setBalance("bidder1", 1000.0);

        AuctionDTO auction = createRunningAuction(seller, 500.0);

        assertNull(bidService.getAutoBidConfigDTO(bidder, auction.getId()));

        bidService.placeBid(bidder, auction.getId(), 550.0);
        bidService.setAutoBid(bidder, auction.getId(), 1000.0, 50.0);
        bidService.cancelAutoBid(bidder, auction.getId());

        assertNull(bidService.getAutoBidConfigDTO(bidder, auction.getId()));
    }

    @Test
    public void testOutbidKeepsBothDepositsUntilAuctionEnds() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder1 = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        User bidder2 = new MockUser("bidder2", "bidder2", UserRole.BIDDER, false);
        saveUser("bidder2", "bidder2", UserRole.BIDDER, 1000.0);
        setBalance("bidder1", 1000.0);

        AuctionDTO auction = createRunningAuction(seller, 500.0);

        bidService.placeBid(bidder1, auction.getId(), 550.0);
        bidService.placeBid(bidder2, auction.getId(), 600.0);

        UserSchema bidder1Schema = userDAO.findById("bidder1");
        UserSchema bidder2Schema = userDAO.findById("bidder2");
        assertEquals(950.0, bidder1Schema.getBalance());
        assertEquals(50.0, bidder1Schema.getFrozenBalance());
        assertEquals(950.0, bidder2Schema.getBalance());
        assertEquals(50.0, bidder2Schema.getFrozenBalance());
        AuctionSchema updated = auctionDAO.findById(auction.getId());
        assertTrue(updated.hasDepositedBidder("bidder1"));
        assertTrue(updated.hasDepositedBidder("bidder2"));
    }

    @Test
    public void testEndAuctionRefundsOnlyNonWinners() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder1 = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        User bidder2 = new MockUser("bidder2", "bidder2", UserRole.BIDDER, false);
        saveUser("bidder2", "bidder2", UserRole.BIDDER, 1000.0);
        setBalance("bidder1", 1000.0);

        AuctionDTO auction = createRunningAuction(seller, 500.0);

        bidService.placeBid(bidder1, auction.getId(), 550.0);
        bidService.placeBid(bidder2, auction.getId(), 600.0);
        auctionService.endAuction(auction.getId());

        UserSchema bidder1Schema = userDAO.findById("bidder1");
        UserSchema bidder2Schema = userDAO.findById("bidder2");
        assertEquals(1000.0, bidder1Schema.getBalance());
        assertEquals(0.0, bidder1Schema.getFrozenBalance());
        assertEquals(950.0, bidder2Schema.getBalance());
        assertEquals(50.0, bidder2Schema.getFrozenBalance());
        AuctionSchema updated = auctionDAO.findById(auction.getId());
        assertEquals(AuctionStatus.WAITING_PAYMENT, updated.getStatus());
        assertFalse(updated.hasDepositedBidder("bidder1"));
        assertTrue(updated.hasDepositedBidder("bidder2"));
        List<TransactionSchema> bidder1Transactions = transactionDAO.findByUserId("bidder1");
        assertTrue(bidder1Transactions.stream()
                .anyMatch(t -> t.getType() == TransactionType.AUCTION_DEPOSIT_REFUND));
    }

    @Test
    public void testAuctionDepositSurvivesLiveAuctionReloadAndDoesNotDoubleFreeze() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        setBalance("bidder1", 1000.0);

        AuctionDTO auction = createRunningAuction(seller, 500.0);

        bidService.placeBid(bidder, auction.getId(), 550.0);
        AuctionManager.getInstance().endAuction(auction.getId());
        AuctionManager.getInstance().loadAuction(auctionDAO.findById(auction.getId()), 500.0);

        assertTrue(AuctionManager.getInstance().getAuction(auction.getId()).hasDeposited("bidder1"));
        assertTrue(auctionService.getAuctionById(auction.getId(), "bidder1").isCurrentUserDeposited());

        bidService.placeBid(bidder, auction.getId(), 600.0);

        UserSchema bidderSchema = userDAO.findById("bidder1");
        assertEquals(950.0, bidderSchema.getBalance());
        assertEquals(50.0, bidderSchema.getFrozenBalance());
    }

    @Test
    public void testAuctionDetailRepairsPersistedDepositFromLiveAuction() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        setBalance("bidder1", 1000.0);

        AuctionDTO auction = createRunningAuction(seller, 500.0);

        bidService.placeBid(bidder, auction.getId(), 550.0);
        AuctionSchema schema = auctionDAO.findById(auction.getId());
        schema.clearDepositedBidders();
        auctionDAO.update(schema);

        AuctionDTO detail = auctionService.getAuctionById(auction.getId(), "bidder1");

        assertTrue(detail.isCurrentUserDeposited());
        assertTrue(auctionDAO.findById(auction.getId()).hasDepositedBidder("bidder1"));
    }

    @Test
    public void testAuctionDetailBackfillsPersistedDepositFromBidHistoryAndFrozenBalance() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        setBalance("bidder1", 1000.0);

        AuctionDTO auction = createRunningAuction(seller, 500.0);

        bidService.placeBid(bidder, auction.getId(), 550.0);
        AuctionSchema schema = auctionDAO.findById(auction.getId());
        schema.clearDepositedBidders();
        auctionDAO.update(schema);
        AuctionManager.getInstance().endAuction(auction.getId());

        AuctionDTO detail = auctionService.getAuctionById(auction.getId(), "bidder1");

        assertTrue(detail.isCurrentUserDeposited());
        assertTrue(auctionDAO.findById(auction.getId()).hasDepositedBidder("bidder1"));
    }

    @Test
    public void testAuctionDetailDoesNotBackfillDepositWithoutFrozenBalance() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        setBalance("bidder1", 1000.0);

        AuctionDTO auction = createRunningAuction(seller, 500.0);
        AuctionManager.getInstance().endAuction(auction.getId());

        LocalDateTime now = LocalDateTime.now();
        bidDAO.save(new BidSchema(
                "legacy-unfrozen-bid",
                now,
                now,
                auction.getId(),
                "bidder1",
                550.0,
                now,
                BidType.MANUAL));

        AuctionDTO detail = auctionService.getAuctionById(auction.getId(), "bidder1");

        assertFalse(detail.isCurrentUserDeposited());
        assertFalse(auctionDAO.findById(auction.getId()).hasDepositedBidder("bidder1"));
    }

    @Test
    public void testLoadRunningAuctionsBackfillsAndHydratesDeposits() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        setBalance("bidder1", 950.0);
        UserSchema bidderSchema = userDAO.findById("bidder1");
        bidderSchema.setFrozenBalance(50.0);
        userDAO.update(bidderSchema);

        AuctionDTO auction = createRunningAuction(seller, 500.0);
        AuctionSchema schema = auctionDAO.findById(auction.getId());
        schema.setHighestBid(550.0);
        schema.setWinnerId("bidder1");
        schema.clearDepositedBidders();
        auctionDAO.update(schema);

        LocalDateTime now = LocalDateTime.now();
        bidDAO.save(new BidSchema(
                "legacy-bid",
                now,
                now,
                auction.getId(),
                "bidder1",
                550.0,
                now,
                BidType.MANUAL));
        AuctionManager.getInstance().endAuction(auction.getId());

        auctionService.loadRunningAuctions();

        assertTrue(auctionDAO.findById(auction.getId()).hasDepositedBidder("bidder1"));
        assertTrue(AuctionManager.getInstance().getAuction(auction.getId()).hasDeposited("bidder1"));

        bidService.placeBid(bidder, auction.getId(), 600.0);

        bidderSchema = userDAO.findById("bidder1");
        assertEquals(950.0, bidderSchema.getBalance());
        assertEquals(50.0, bidderSchema.getFrozenBalance());
    }

    @Test
    public void testEndAuctionRefundsPersistedDepositsWithoutLiveAuction() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder1 = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        User bidder2 = new MockUser("bidder2", "bidder2", UserRole.BIDDER, false);
        saveUser("bidder2", "bidder2", UserRole.BIDDER, 1000.0);
        setBalance("bidder1", 1000.0);

        AuctionDTO auction = createRunningAuction(seller, 500.0);

        bidService.placeBid(bidder1, auction.getId(), 550.0);
        bidService.placeBid(bidder2, auction.getId(), 600.0);
        AuctionManager.getInstance().endAuction(auction.getId());

        auctionService.endAuction(auction.getId());

        UserSchema bidder1Schema = userDAO.findById("bidder1");
        UserSchema bidder2Schema = userDAO.findById("bidder2");
        assertEquals(1000.0, bidder1Schema.getBalance());
        assertEquals(0.0, bidder1Schema.getFrozenBalance());
        assertEquals(950.0, bidder2Schema.getBalance());
        assertEquals(50.0, bidder2Schema.getFrozenBalance());

        AuctionSchema updated = auctionDAO.findById(auction.getId());
        assertEquals(AuctionStatus.WAITING_PAYMENT, updated.getStatus());
        assertFalse(updated.hasDepositedBidder("bidder1"));
        assertTrue(updated.hasDepositedBidder("bidder2"));
    }

    @Test
    public void testWinnerPaymentUsesFrozenDeposit() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        setBalance("bidder1", 1000.0);

        AuctionDTO auction = createRunningAuction(seller, 500.0);

        bidService.placeBid(bidder, auction.getId(), 600.0);
        auctionService.endAuction(auction.getId());
        auctionService.payAuction(bidder, auction.getId());

        UserSchema bidderSchema = userDAO.findById("bidder1");
        UserSchema sellerSchema = userDAO.findById("seller1");
        assertEquals(400.0, bidderSchema.getBalance());
        assertEquals(0.0, bidderSchema.getFrozenBalance());
        assertEquals(600.0, sellerSchema.getBalance());
        AuctionSchema updated = auctionDAO.findById(auction.getId());
        assertEquals(AuctionStatus.PAID, updated.getStatus());
        assertFalse(updated.hasDepositedBidder("bidder1"));
        assertEquals("bidder1", itemDAO.findById(auction.getItem().getId()).getSellerId());
        assertEquals(1, itemService.getItemsByOwnerId("bidder1").size());
        List<TransactionSchema> bidderTransactions = transactionDAO.findByUserId("bidder1");
        assertTrue(bidderTransactions.stream().anyMatch(t -> t.getType() == TransactionType.AUCTION_PAYMENT));
        assertTrue(bidderTransactions.stream().anyMatch(t -> t.getType() == TransactionType.AUCTION_DEPOSIT_APPLIED));
        assertTrue(transactionDAO.findByUserId("seller1").stream()
                .anyMatch(t -> t.getType() == TransactionType.SELLER_PAYOUT));
    }

    @Test
    public void testWinnerPaymentInsufficientBalanceKeepsWaitingPaymentAndDeposit() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        setBalance("bidder1", 560.0);

        AuctionDTO auction = createRunningAuction(seller, 500.0);

        bidService.placeBid(bidder, auction.getId(), 600.0);
        auctionService.endAuction(auction.getId());

        assertThrows(AuctionException.class, () -> auctionService.payAuction(bidder, auction.getId()));

        UserSchema bidderSchema = userDAO.findById("bidder1");
        assertEquals(510.0, bidderSchema.getBalance());
        assertEquals(50.0, bidderSchema.getFrozenBalance());

        AuctionSchema updated = auctionDAO.findById(auction.getId());
        assertEquals(AuctionStatus.WAITING_PAYMENT, updated.getStatus());
        assertTrue(updated.hasDepositedBidder("bidder1"));
        assertEquals("seller1", itemDAO.findById(auction.getItem().getId()).getSellerId());
    }

    @Test
    public void testWinnerPaymentAfterDeadlineIsRejectedAndForfeitsDeposit() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        setBalance("bidder1", 1000.0);

        AuctionDTO auction = createRunningAuction(seller, 500.0);

        bidService.placeBid(bidder, auction.getId(), 600.0);
        auctionService.endAuction(auction.getId());

        AuctionSchema waitingPayment = auctionDAO.findById(auction.getId());
        waitingPayment.setPaymentDeadlineAt(LocalDateTime.now().minusMinutes(1));
        auctionDAO.update(waitingPayment);

        assertThrows(AuctionException.class, () -> auctionService.payAuction(bidder, auction.getId()));

        UserSchema bidderSchema = userDAO.findById("bidder1");
        assertEquals(950.0, bidderSchema.getBalance());
        assertEquals(0.0, bidderSchema.getFrozenBalance());

        AuctionSchema updated = auctionDAO.findById(auction.getId());
        assertEquals(AuctionStatus.CANCELED, updated.getStatus());
        assertFalse(updated.hasDepositedBidder("bidder1"));
        assertEquals("seller1", itemDAO.findById(auction.getItem().getId()).getSellerId());
    }

    @Test
    public void testPaymentDeadlineForfeitsDepositAndKeepsSellerOwnership() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        setBalance("bidder1", 1000.0);

        AuctionDTO auction = createRunningAuction(seller, 500.0);

        bidService.placeBid(bidder, auction.getId(), 600.0);
        auctionService.endAuction(auction.getId());
        auctionService.expirePaymentDeadline(auction.getId());

        UserSchema bidderSchema = userDAO.findById("bidder1");
        assertEquals(950.0, bidderSchema.getBalance());
        assertEquals(0.0, bidderSchema.getFrozenBalance());

        AuctionSchema updated = auctionDAO.findById(auction.getId());
        assertEquals(AuctionStatus.CANCELED, updated.getStatus());
        assertFalse(updated.hasDepositedBidder("bidder1"));
        assertEquals("seller1", itemDAO.findById(auction.getItem().getId()).getSellerId());
        assertTrue(transactionDAO.findByUserId("bidder1").stream()
                .anyMatch(t -> t.getType() == TransactionType.AUCTION_DEPOSIT_FORFEIT));
    }

    @Test
    public void testPendingPaymentsOnlyReturnsWinnerWaitingPaymentAuctions() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        saveUser("bidder2", "bidder2", UserRole.BIDDER, 1000.0);
        setBalance("bidder1", 1000.0);

        AuctionDTO auction = createRunningAuction(seller, 500.0);

        bidService.placeBid(bidder, auction.getId(), 600.0);
        auctionService.endAuction(auction.getId());

        List<AuctionDTO> bidderPending = auctionService.getPendingPaymentsForWinner("bidder1");
        List<AuctionDTO> otherPending = auctionService.getPendingPaymentsForWinner("bidder2");

        assertEquals(1, bidderPending.size());
        assertEquals(auction.getId(), bidderPending.get(0).getId());
        assertNotNull(bidderPending.get(0).getPaymentDeadlineAt());
        assertEquals(0, otherPending.size());
    }

    @Test
    public void testArchiveItemSoftDeletesWithoutBreakingHistory() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        ItemDTO item = itemService.createItem(
                seller,
                "Item can go",
                "Desc",
                500.0,
                ItemType.ELECTRONICS,
                null,
                ItemCondition.NEW,
                Map.of("brand", "Test", "warrantyMonths", 12));

        itemService.archiveItem(seller, item.getId());

        assertTrue(itemDAO.findById(item.getId()).isArchived());
        assertEquals(0, itemService.getItemsByOwnerId("seller1").size());
        assertNotNull(itemService.getItemById(item.getId()));
    }

    @Test
    public void testArchiveItemWithActiveAuctionIsRejected() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        ItemDTO item = itemService.createItem(
                seller,
                "Item active",
                "Desc",
                500.0,
                ItemType.ELECTRONICS,
                null,
                ItemCondition.NEW,
                Map.of("brand", "Test", "warrantyMonths", 12));

        auctionService.createAuction(
                seller,
                item.getId(),
                LocalDateTime.now().plusMinutes(5),
                LocalDateTime.now().plusDays(1),
                "Auction active",
                "Desc",
                60,
                120);

        assertThrows(AuctionException.class, () -> itemService.archiveItem(seller, item.getId()));
        assertFalse(itemDAO.findById(item.getId()).isArchived());
    }

    private void saveUser(String id, String username, UserRole role) {
        saveUser(id, username, role, 0.0);
    }

    private void saveUser(String id, String username, UserRole role, double balance) {
        LocalDateTime now = LocalDateTime.now();
        UserSchema user = new UserSchema(id, now, now, username, "hash", "salt", username + "@uet.vn", role);
        user.setBalance(balance);
        userDAO.save(user);
    }

    private void setBalance(String userId, double balance) {
        UserSchema user = userDAO.findById(userId);
        user.setBalance(balance);
        userDAO.update(user);
    }

    private AuctionDTO createRunningAuction(User seller, double startingPrice) throws Exception {
        AuctionDTO auction = createOpenAuction(seller, startingPrice);
        auctionService.startAuction(seller, auction.getId());
        return auction;
    }

    private AuctionDTO createOpenAuction(User seller, double startingPrice) throws Exception {
        ItemDTO item = itemService.createItem(
                seller,
                "Test item",
                "Desc",
                startingPrice,
                ItemType.ELECTRONICS,
                null,
                ItemCondition.NEW,
                Map.of("brand", "Test", "warrantyMonths", 12));
        AuctionDTO auction = auctionService.createAuction(
                seller,
                item.getId(),
                LocalDateTime.now().plusMinutes(5),
                LocalDateTime.now().plusDays(1),
                "Auction " + startingPrice,
                "Desc",
                60,
                120);
        return auction;
    }

    private void deleteTestFiles() {
        new File(itemFile).delete();
        new File(auctionFile).delete();
        new File(userFile).delete();
        new File(bidFile).delete();
        new File(transactionFile).delete();
        new File(settingFile).delete();
    }
}
