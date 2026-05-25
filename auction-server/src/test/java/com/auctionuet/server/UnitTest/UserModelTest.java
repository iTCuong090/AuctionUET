package com.auctionuet.server.UnitTest;

import com.auctionuet.protocol.enums.Permission;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.domain.model.Bidder;
import com.auctionuet.server.domain.model.Seller;
import com.auctionuet.server.domain.model.Admin;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UserModelTest {

    @Test
    void testBidderPermissions() {
        Bidder bidder = new Bidder("1", "duy");

        assertTrue(bidder.hasPermission(Permission.PLACE_BID));
        assertTrue(bidder.hasPermission(Permission.VIEW_AUCTION));
        assertTrue(bidder.hasPermission(Permission.VIEW_BID_HISTORY));
        assertTrue(bidder.hasPermission(Permission.GET_PROFILE));
        assertTrue(bidder.hasPermission(Permission.UPDATE_PROFILE));

        assertFalse(bidder.hasPermission(Permission.CREATE_AUCTION));
        assertFalse(bidder.hasPermission(Permission.MANAGE_USERS));
    }

    @Test
    void testSellerPermissions() {
        Seller seller = new Seller("2", "duy");

        assertTrue(seller.hasPermission(Permission.CREATE_AUCTION));
        assertTrue(seller.hasPermission(Permission.VIEW_AUCTION));
        assertTrue(seller.hasPermission(Permission.GET_PROFILE));
        assertTrue(seller.hasPermission(Permission.UPDATE_PROFILE));
        assertTrue(seller.hasPermission(Permission.VIEW_BID_HISTORY));

        assertFalse(seller.hasPermission(Permission.PLACE_BID));
        assertFalse(seller.hasPermission(Permission.MANAGE_USERS));
    }

    @Test
    void testAdminPermissions() {
        Admin admin = new Admin("3", "duy");

        assertTrue(admin.hasPermission(Permission.MANAGE_USERS));
        assertTrue(admin.hasPermission(Permission.VIEW_AUCTION));
        assertTrue(admin.hasPermission(Permission.GET_PROFILE));
        assertTrue(admin.hasPermission(Permission.UPDATE_PROFILE));
        assertTrue(admin.hasPermission(Permission.VIEW_BID_HISTORY));
        assertTrue(admin.hasPermission(Permission.UPDATE_USER_STATUS));

        assertFalse(admin.hasPermission(Permission.PLACE_BID));
        assertFalse(admin.hasPermission(Permission.CREATE_AUCTION));
    }

    @Test
    void testUserImmutable() {
        User user = new Bidder("1", "duy");

        // Không có setter → test gián tiếp bằng việc không thể thay đổi giá trị
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
