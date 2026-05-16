package com.auctionuet.server.domain.service;

import com.auctionuet.protocol.dto.response.auction.AuctionDTO;
import com.auctionuet.protocol.dto.response.item.ItemDTO;
import com.auctionuet.protocol.dto.response.user.UserDTO;
import com.auctionuet.protocol.enums.AuctionStatus;
import com.auctionuet.protocol.enums.ItemCondition;
import com.auctionuet.protocol.enums.ItemType;
import com.auctionuet.protocol.enums.Permission;
import com.auctionuet.protocol.enums.UserRole;
import com.auctionuet.server.domain.manager.AuctionManager;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.exception.AuctionException;
import com.auctionuet.server.persistence.dao.AuctionDAO;
import com.auctionuet.server.persistence.dao.BidDAO;
import com.auctionuet.server.persistence.dao.ItemDAO;
import com.auctionuet.server.persistence.dao.UserDAO;
import com.auctionuet.server.persistence.schema.AuctionSchema;
import com.auctionuet.server.persistence.schema.ItemSchema;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AuctionServiceTest {

    private ItemDAO itemDAO;
    private AuctionDAO auctionDAO;
    private UserDAO userDAO;
    private BidDAO bidDAO;
    private ItemService itemService;
    private AuctionService auctionService;

    private final String itemFile = "data/test_items_service.json";
    private final String auctionFile = "data/test_auctions_service.json";
    private final String userFile = "data/test_users_service.json";
    private final String bidFile = "data/test_bids_service.json";

    @BeforeEach
    public void setup() {
        deleteTestFiles();

        itemDAO = new ItemDAO(itemFile);
        auctionDAO = new AuctionDAO(auctionFile);
        userDAO = new UserDAO(userFile);
        bidDAO = new BidDAO(bidFile);

        UserService userService = new UserService(userDAO);
        itemService = new ItemService(itemDAO, userService);
        WalletService walletService = new WalletService(userDAO, userService);
        BidService bidService = new BidService(
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
            if (action == Permission.CREATE_ITEM || action == Permission.CREATE_AUCTION) {
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

    private void saveUser(String id, String username, UserRole role) {
        LocalDateTime now = LocalDateTime.now();
        userDAO.save(new UserSchema(id, now, now, username, "hash", "salt", username + "@uet.vn", role));
    }

    private void deleteTestFiles() {
        new File(itemFile).delete();
        new File(auctionFile).delete();
        new File(userFile).delete();
        new File(bidFile).delete();
    }
}
