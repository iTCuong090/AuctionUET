package com.auctionuet.server.domain.service;

import com.auctionuet.protocol.dto.response.auction.AuctionDTO;
import com.auctionuet.protocol.dto.response.bid.AutoBidConfigDTO;
import com.auctionuet.protocol.dto.response.item.ItemDTO;
import com.auctionuet.protocol.dto.response.user.UserDTO;
import com.auctionuet.protocol.enums.AutoBidStatus;
import com.auctionuet.protocol.enums.AuctionStatus;
import com.auctionuet.protocol.enums.BidType;
import com.auctionuet.protocol.enums.ItemCondition;
import com.auctionuet.protocol.enums.ItemType;
import com.auctionuet.protocol.enums.Permission;
import com.auctionuet.protocol.enums.TransactionType;
import com.auctionuet.protocol.enums.UserRole;
import com.auctionuet.server.domain.manager.AuctionManager;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.exception.AuctionException;
import com.auctionuet.server.persistence.dao.AuctionDAO;
import com.auctionuet.server.persistence.dao.BidDAO;
import com.auctionuet.server.persistence.dao.ItemDAO;
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
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuctionServiceTest {

    private ItemDAO itemDAO;
    private AuctionDAO auctionDAO;
    private UserDAO userDAO;
    private BidDAO bidDAO;
    private TransactionDAO transactionDAO;
    private ItemService itemService;
    private AuctionService auctionService;
    private BidService bidService;
    private WalletService walletService;

    private final String itemFile = "data/test_items_service.json";
    private final String auctionFile = "data/test_auctions_service.json";
    private final String userFile = "data/test_users_service.json";
    private final String bidFile = "data/test_bids_service.json";
    private final String transactionFile = "data/test_transactions_service.json";

    @BeforeEach
    public void setup() {
        deleteTestFiles();

        itemDAO = new ItemDAO(itemFile);
        auctionDAO = new AuctionDAO(auctionFile);
        userDAO = new UserDAO(userFile);
        bidDAO = new BidDAO(bidFile);
        transactionDAO = new TransactionDAO(transactionFile);

        UserService userService = new UserService(userDAO);
        itemService = new ItemService(itemDAO, userService, auctionDAO);
        TransactionService transactionService = new TransactionService(transactionDAO);
        walletService = new WalletService(userDAO, userService, transactionService);
        bidService = new BidService(
                AuctionManager.getInstance(),
                walletService,
                bidDAO,
                itemService,
                auctionDAO,
                userService);
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
        ItemDTO item = itemService.createItem(seller, dto.getName(), dto.getDescription(), dto.getStartingPrice(),
                dto.getType(), dto.getImageUrl(), dto.getCondition(), dto.getExtraFields());

        auctionService.createAuction(seller, item.getId(), LocalDateTime.now().plusMinutes(5),
                LocalDateTime.now().plusDays(1), "Auction 1", "Desc", 60, 120);
        auctionService.createAuction(seller, item.getId(), LocalDateTime.now().plusMinutes(5),
                LocalDateTime.now().plusDays(1), "Auction 2", "Desc", 60, 120);
        auctionService.createAuction(seller, item.getId(), LocalDateTime.now().plusMinutes(5),
                LocalDateTime.now().plusDays(1), "Auction 3", "Desc", 60, 120);

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
    public void testSetAutoBidFreezesDepositAndBidsImmediately() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        setBalance("bidder1", 1000.0);

        AuctionDTO auction = createRunningAuction(seller, 500.0);

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
        assertEquals(BidType.AUTO, bids.get(0).getBidType());

        AutoBidConfigDTO state = bidService.getAutoBidConfigDTO(bidder, auction.getId());
        assertEquals(AutoBidStatus.PROTECTING, state.getStatus());
        assertEquals(1000.0, state.getProtectedUntil());
    }

    @Test
    public void testSetAutoBidWithoutDepositBalanceDoesNotCreateConfigOrBid() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        setBalance("bidder1", 40.0);

        AuctionDTO auction = createRunningAuction(seller, 500.0);

        assertThrows(IllegalArgumentException.class,
                () -> bidService.setAutoBid(bidder, auction.getId(), 1000.0, 50.0));

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
    public void testSetAutoBidPersistsDepositEvenWithoutAutoBidRecord() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        setBalance("bidder1", 1000.0);

        AuctionDTO auction = createRunningAuction(seller, 500.0);

        bidService.setAutoBid(bidder, auction.getId(), 520.0, 50.0);

        assertEquals(0, bidDAO.findByAuctionId(auction.getId()).size());
        assertTrue(auctionDAO.findById(auction.getId()).hasDepositedBidder("bidder1"));

        UserSchema bidderSchema = userDAO.findById("bidder1");
        assertEquals(950.0, bidderSchema.getBalance());
        assertEquals(50.0, bidderSchema.getFrozenBalance());

        AutoBidConfigDTO state = bidService.getAutoBidConfigDTO(bidder, auction.getId());
        assertEquals(AutoBidStatus.WAITING, state.getStatus());
        assertEquals(520.0, state.getProtectedUntil());
    }

    @Test
    public void testCheckAutoBidReturnsInactiveForOutbidConfig() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder1 = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        User bidder2 = new MockUser("bidder2", "bidder2", UserRole.BIDDER, false);
        setBalance("bidder1", 1000.0);
        saveUser("bidder2", "bidder2", UserRole.BIDDER, 1000.0);

        AuctionDTO auction = createRunningAuction(seller, 500.0);

        bidService.setAutoBid(bidder1, auction.getId(), 900.0, 50.0);
        bidService.setAutoBid(bidder2, auction.getId(), 1000.0, 50.0);

        AutoBidConfigDTO bidder1State = bidService.getAutoBidConfigDTO(bidder1, auction.getId());
        AutoBidConfigDTO bidder2State = bidService.getAutoBidConfigDTO(bidder2, auction.getId());

        assertEquals(AutoBidStatus.INEFFECTIVE, bidder1State.getStatus());
        assertEquals(0.0, bidder1State.getProtectedUntil());
        assertEquals(AutoBidStatus.PROTECTING, bidder2State.getStatus());
        assertEquals(1000.0, bidder2State.getProtectedUntil());
    }

    @Test
    public void testSetAutoBidEqualMaxKeepsEarlierBidderAsWinner() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder1 = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        User bidder2 = new MockUser("bidder2", "bidder2", UserRole.BIDDER, false);
        setBalance("bidder1", 1000.0);
        saveUser("bidder2", "bidder2", UserRole.BIDDER, 1000.0);

        AuctionDTO auction = createRunningAuction(seller, 500.0);

        bidService.setAutoBid(bidder1, auction.getId(), 1000.0, 50.0);
        bidService.setAutoBid(bidder2, auction.getId(), 1000.0, 50.0);

        AuctionSchema updated = auctionDAO.findById(auction.getId());
        assertEquals("bidder1", updated.getWinnerId());
        assertEquals(1000.0, updated.getHighestBid());

        AutoBidConfigDTO bidder1State = bidService.getAutoBidConfigDTO(bidder1, auction.getId());
        AutoBidConfigDTO bidder2State = bidService.getAutoBidConfigDTO(bidder2, auction.getId());
        assertEquals(AutoBidStatus.PROTECTING, bidder1State.getStatus());
        assertEquals(AutoBidStatus.INEFFECTIVE, bidder2State.getStatus());
    }

    @Test
    public void testManualBidAtAutoBidMaxKeepsEarlierAutoBidderAsWinner() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder1 = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        User bidder2 = new MockUser("bidder2", "bidder2", UserRole.BIDDER, false);
        setBalance("bidder1", 1000.0);
        saveUser("bidder2", "bidder2", UserRole.BIDDER, 1000.0);

        AuctionDTO auction = createRunningAuction(seller, 500.0);

        bidService.setAutoBid(bidder1, auction.getId(), 1000.0, 50.0);
        bidService.placeBid(bidder2, auction.getId(), 1000.0);

        AuctionSchema updated = auctionDAO.findById(auction.getId());
        assertEquals("bidder1", updated.getWinnerId());
        assertEquals(1000.0, updated.getHighestBid());

        AutoBidConfigDTO bidder1State = bidService.getAutoBidConfigDTO(bidder1, auction.getId());
        assertEquals(AutoBidStatus.PROTECTING, bidder1State.getStatus());
        assertTrue(bidDAO.findByAuctionId(auction.getId()).stream()
                .anyMatch(bid -> "bidder1".equals(bid.getBidderId())
                        && Double.compare(1000.0, bid.getAmount()) == 0
                        && bid.getBidType() == BidType.AUTO));
    }

    @Test
    public void testCheckAutoBidReturnsNullWhenMissingOrCanceled() throws Exception {
        User seller = new MockUser("seller1", "seller", UserRole.SELLER, true);
        User bidder = new MockUser("bidder1", "bidder", UserRole.BIDDER, false);
        setBalance("bidder1", 1000.0);

        AuctionDTO auction = createRunningAuction(seller, 500.0);

        assertNull(bidService.getAutoBidConfigDTO(bidder, auction.getId()));

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
        assertTrue(bidderTransactions.stream().anyMatch(t -> t.getType() == TransactionType.AUCTION_DEPOSIT_FORFEIT));
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
        auctionService.startAuction(seller, auction.getId());
        return auction;
    }

    private void deleteTestFiles() {
        new File(itemFile).delete();
        new File(auctionFile).delete();
        new File(userFile).delete();
        new File(bidFile).delete();
        new File(transactionFile).delete();
    }
}
