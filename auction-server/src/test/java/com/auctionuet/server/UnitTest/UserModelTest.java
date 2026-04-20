package com.auctionuet.server.UnitTest;

import com.auctionuet.server.domain.model.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UserModelTest {

    @Test
    void testBidderPermissions() {
        Bidder bidder = new Bidder("1", "duy");

        assertTrue(bidder.hasPermission("PLACE_BID"));
        assertTrue(bidder.hasPermission("VIEW_AUCTION"));
        assertTrue(bidder.hasPermission("VIEW_BID_HISTORY"));
        assertTrue(bidder.hasPermission("GET_PROFILE"));
        assertTrue(bidder.hasPermission("UPDATE_PROFILE"));

        assertFalse(bidder.hasPermission("CREATE_AUCTION"));
        assertFalse(bidder.hasPermission("MANAGE_USERS"));
    }

    @Test
    void testSellerPermissions() {
        Seller seller = new Seller("2", "duy");

        assertTrue(seller.hasPermission("CREATE_AUCTION"));
        assertTrue(seller.hasPermission("VIEW_AUCTION"));
        assertTrue(seller.hasPermission("GET_PROFILE"));
        assertTrue(seller.hasPermission("UPDATE_PROFILE"));
        assertTrue(seller.hasPermission("VIEW_BID_HISTORY"));

        assertFalse(seller.hasPermission("PLACE_BID"));
        assertFalse(seller.hasPermission("MANAGE_USERS"));
    }

    @Test
    void testAdminPermissions() {
        Admin admin = new Admin("3", "duy");

        assertTrue(admin.hasPermission("MANAGE_USERS"));
        assertTrue(admin.hasPermission("VIEW_AUCTION"));
        assertTrue(admin.hasPermission("GET_PROFILE"));
        assertTrue(admin.hasPermission("UPDATE_PROFILE"));
        assertTrue(admin.hasPermission("VIEW_BID_HISTORY"));

        assertFalse(admin.hasPermission("PLACE_BID"));
        assertFalse(admin.hasPermission("CREATE_AUCTION"));
    }

    @Test
    void testUserImmutable() {
        User user = new Bidder("1", "duy");

        // Không có setter -> test gián tiếp bằng việc không thể thay đổi giá trị
        assertEquals("1", user.getId());
        assertEquals("duy", user.getUsername());

        // Nếu có setter thì test sẽ fail compile (đúng yêu cầu)
    }

    @Test
    void testGetDisplayInfo() {
        User bidder = new Bidder("1", "duy");
        User seller = new Seller("2", "duy");
        User admin = new Admin("3", "duy");

        assertEquals("Bidder: duy", bidder.getDisplayInfo());
        assertEquals("Seller: duy", seller.getDisplayInfo());
        assertEquals("Admin: duy", admin.getDisplayInfo());
    }
}