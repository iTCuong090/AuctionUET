package com.auctionuet.server.domain.service;

import com.auctionuet.protocol.enums.AuctionStatus;
import com.auctionuet.protocol.enums.ItemType;
import com.auctionuet.protocol.enums.Permission;
import com.auctionuet.protocol.enums.UserRole;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.exception.AuctionException;
import com.auctionuet.server.mapper.ItemMapper;
import com.auctionuet.protocol.dto.response.item.ItemDTO;
import com.auctionuet.server.persistence.dao.AuctionDAO;
import com.auctionuet.server.persistence.dao.ItemDAO;
import com.auctionuet.server.persistence.schema.AuctionSchema;
import com.auctionuet.server.persistence.schema.ItemSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionServiceTest {

    private ItemDAO itemDAO;
    private AuctionDAO auctionDAO;
    private ItemService itemService;
    private AuctionService auctionService;
    private final String itemFile = "data/test_items_service.json";
    private final String auctionFile = "data/test_auctions_service.json";

    @BeforeEach
    public void setup() {
        itemDAO = new ItemDAO(itemFile);
        auctionDAO = new AuctionDAO(auctionFile);
        itemService = new ItemService(itemDAO);
        auctionService = new AuctionService(itemService, auctionDAO, null);
        new File(itemFile).delete();
        new File(auctionFile).delete();
    }

    @AfterEach
    public void teardown() {
        new File(itemFile).delete();
        new File(auctionFile).delete();
    }

    class MockUser extends User {
        private boolean isSeller;
        
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
        return new ItemDTO(null, "Phone", null, 500.0, ItemType.ELECTRONICS, null, null, null, extra);
    }

    @Test
    public void testCreateItemBySeller() throws Exception {
        User seller = new MockUser("seller1", "seller", null, true);
        ItemDTO dto = createElectronicsDTO();

        ItemSchema item = itemService.createItem(seller, dto.getName(), dto.getDescription(), dto.getStartingPrice(), dto.getType(), dto.getImageUrl(), dto.getCondition(), dto.getExtraFields());
        assertNotNull(item);
        assertEquals("Phone", item.getName());
        assertEquals("seller1", item.getSellerId());
        
        ItemSchema saved = itemDAO.findById(item.getId());
        assertNotNull(saved);
    }

    @Test
    public void testCreateItemByBidder() {
        User bidder = new MockUser("bidder1", "bidder", null, false);
        ItemDTO dto = createElectronicsDTO();

        assertThrows(AuctionException.class, () -> {
            itemService.createItem(bidder, dto.getName(), dto.getDescription(), dto.getStartingPrice(), dto.getType(), dto.getImageUrl(), dto.getCondition(), dto.getExtraFields());
        });
    }

    @Test
    public void testGetItemsBySeller() throws Exception {
        User seller = new MockUser("seller1", "seller", null, true);
        ItemDTO dto1 = createElectronicsDTO();
        ItemDTO dto2 = new ItemDTO(null, "Laptop", null, 1500.0, ItemType.ELECTRONICS, null, null, null, new HashMap<>());

        itemService.createItem(seller, dto1.getName(), dto1.getDescription(), dto1.getStartingPrice(), dto1.getType(), dto1.getImageUrl(), dto1.getCondition(), dto1.getExtraFields());
        itemService.createItem(seller, dto2.getName(), dto2.getDescription(), dto2.getStartingPrice(), dto2.getType(), dto2.getImageUrl(), dto2.getCondition(), dto2.getExtraFields());

        List<ItemSchema> myItems = itemService.getItemsBySellerId("seller1");
        assertEquals(2, myItems.size());

        // User khác không có item nào
        List<ItemSchema> otherItems = itemService.getItemsBySellerId("seller2");
        assertEquals(0, otherItems.size());
    }

    @Test
    public void testCreateAuction() throws Exception {
        User seller = new MockUser("seller1", "seller", null, true);
        ItemDTO dto = createElectronicsDTO();
        ItemSchema item = itemService.createItem(seller, dto.getName(), dto.getDescription(), dto.getStartingPrice(), dto.getType(), dto.getImageUrl(), dto.getCondition(), dto.getExtraFields());

        AuctionSchema auction = auctionService.createAuction(seller, item.getId(), LocalDateTime.now().plusMinutes(5), LocalDateTime.now().plusDays(1), "Auction 1", "Desc", 60, 120);
        assertNotNull(auction);
        assertEquals(AuctionStatus.OPEN, auction.getStatus());
        assertEquals("seller1", auction.getSellerId());
    }

    @Test
    public void testCreateAuctionWrongOwner() throws Exception {
        User seller1 = new MockUser("seller1", "seller1", null, true);
        User seller2 = new MockUser("seller2", "seller2", null, true);
        ItemDTO dto = createElectronicsDTO();
        ItemSchema item = itemService.createItem(seller1, dto.getName(), dto.getDescription(), dto.getStartingPrice(), dto.getType(), dto.getImageUrl(), dto.getCondition(), dto.getExtraFields());

        assertThrows(AuctionException.class, () -> {
            auctionService.createAuction(seller2, item.getId(), LocalDateTime.now().plusMinutes(5), LocalDateTime.now().plusDays(1), "Auction 1", "Desc", 60, 120);
        });
    }

    @Test
    public void testStartAuction() throws Exception {
        User seller = new MockUser("seller1", "seller", null, true);
        ItemDTO dto = createElectronicsDTO();
        ItemSchema item = itemService.createItem(seller, dto.getName(), dto.getDescription(), dto.getStartingPrice(), dto.getType(), dto.getImageUrl(), dto.getCondition(), dto.getExtraFields());

        AuctionSchema auction = auctionService.createAuction(seller, item.getId(), LocalDateTime.now().plusMinutes(5), LocalDateTime.now().plusDays(1), "Auction 1", "Desc", 60, 120);
        auctionService.startAuction(seller, auction.getId());
        
        AuctionSchema updated = auctionDAO.findById(auction.getId());
        assertEquals(AuctionStatus.RUNNING, updated.getStatus());
    }

    @Test
    public void testStartAuctionAlreadyRunning() throws Exception {
        User seller = new MockUser("seller1", "seller", null, true);
        ItemDTO dto = createElectronicsDTO();
        ItemSchema item = itemService.createItem(seller, dto.getName(), dto.getDescription(), dto.getStartingPrice(), dto.getType(), dto.getImageUrl(), dto.getCondition(), dto.getExtraFields());

        AuctionSchema auction = auctionService.createAuction(seller, item.getId(), LocalDateTime.now().plusMinutes(5), LocalDateTime.now().plusDays(1), "Auction 1", "Desc", 60, 120);
        auctionService.startAuction(seller, auction.getId());

        assertThrows(AuctionException.class, () -> {
            auctionService.startAuction(seller, auction.getId());
        });
    }

    @Test
    public void testGetAuctions() throws Exception {
        User seller = new MockUser("seller1", "seller", null, true);
        ItemDTO dto = createElectronicsDTO();
        ItemSchema item = itemService.createItem(seller, dto.getName(), dto.getDescription(), dto.getStartingPrice(), dto.getType(), dto.getImageUrl(), dto.getCondition(), dto.getExtraFields());

        auctionService.createAuction(seller, item.getId(), LocalDateTime.now().plusMinutes(5), LocalDateTime.now().plusDays(1), "Auction 1", "Desc", 60, 120);
        auctionService.createAuction(seller, item.getId(), LocalDateTime.now().plusMinutes(5), LocalDateTime.now().plusDays(1), "Auction 2", "Desc", 60, 120);
        auctionService.createAuction(seller, item.getId(), LocalDateTime.now().plusMinutes(5), LocalDateTime.now().plusDays(1), "Auction 3", "Desc", 60, 120);

        List<AuctionSchema> auctions = auctionService.getAuctions();
        assertEquals(3, auctions.size());
    }

    @Test
    public void testEndAuction() throws Exception {
        User seller = new MockUser("seller1", "seller", null, true);
        ItemDTO dto = createElectronicsDTO();
        ItemSchema item = itemService.createItem(seller, dto.getName(), dto.getDescription(), dto.getStartingPrice(), dto.getType(), dto.getImageUrl(), dto.getCondition(), dto.getExtraFields());

        AuctionSchema auction = auctionService.createAuction(seller, item.getId(), LocalDateTime.now().plusMinutes(5), LocalDateTime.now().plusDays(1), "Auction 1", "Desc", 60, 120);
        auctionService.startAuction(seller, auction.getId());
        
        auctionService.endAuction(auction.getId());
        
        AuctionSchema updated = auctionDAO.findById(auction.getId());
        assertEquals(AuctionStatus.CANCELED, updated.getStatus());
    }
}
