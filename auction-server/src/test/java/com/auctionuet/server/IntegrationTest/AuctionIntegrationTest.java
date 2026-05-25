package com.auctionuet.server.IntegrationTest;

import com.auctionuet.protocol.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuctionIntegrationTest {

    private static final int PORT = 9999;

    @BeforeAll
    static void setup() throws Exception {
        TestHelper.startTestServer(PORT, false);
        Thread.sleep(500);
    }

    @AfterAll
    static void teardown() {
        TestHelper.stopTestServer();
        TestHelper.cleanTestData();
    }

    @BeforeEach
    void clean() {
        TestHelper.cleanTestData();
    }

    private String registerAndLogin(String username, String role) throws Exception {
        Response reg = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"REGISTER\",\"data\":{\"username\":\"" + username
                        + "\",\"password\":\"12345678\",\"email\":\"" + username
                        + "@uet.vn\",\"role\":\"" + role + "\"}}");

        assertEquals("OK", reg.getStatus(), reg.getMessage());

        Response login = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"LOGIN\",\"data\":{\"username\":\"" + username
                        + "\",\"password\":\"12345678\"}}");

        assertEquals("OK", login.getStatus(), login.getMessage());
        return ((Map<String, Object>) login.getData()).get("token").toString();
    }

    @Test
    @Order(1)
    void testFullAuctionFlow() throws Exception {
        String sToken = registerAndLogin("seller123", "SELLER");

        Response itemRes = createItem(sToken, artItemData("Vase", 100));
        assertEquals("OK", itemRes.getStatus(), itemRes.getMessage());

        Map<String, Object> item = (Map<String, Object>) itemRes.getData();
        assertNotNull(item);
        String itemId = item.get("id").toString();

        Response auctionRes = createAuction(sToken, itemId, "Auction Test");
        assertEquals("OK", auctionRes.getStatus(), auctionRes.getMessage());

        Map<String, Object> auction = (Map<String, Object>) auctionRes.getData();
        assertNotNull(auction);
        String auctionId = auction.get("id").toString();
        assertEquals("OPEN", auction.get("status").toString());

        Response startRes = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"START_AUCTION\",\"token\":\"" + sToken
                        + "\",\"data\":{\"auctionId\":\"" + auctionId + "\"}}");
        assertEquals("OK", startRes.getStatus(), startRes.getMessage());

        String bToken = registerAndLogin("bidder123", "BIDDER");

        Response listRes = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"GET_AUCTIONS\",\"token\":\"" + bToken + "\"}");
        assertEquals("OK", listRes.getStatus(), listRes.getMessage());

        List<Map<String, Object>> list = (List<Map<String, Object>>) listRes.getData();
        assertNotNull(list);
        assertFalse(list.isEmpty());

        Response detailRes = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"GET_AUCTION_DETAIL\",\"token\":\"" + bToken
                        + "\",\"data\":{\"auctionId\":\"" + auctionId + "\"}}");
        assertEquals("OK", detailRes.getStatus(), detailRes.getMessage());

        Map<String, Object> detail = (Map<String, Object>) detailRes.getData();
        assertNotNull(detail);
        assertEquals("RUNNING", detail.get("status").toString());
    }

    @Test
    @Order(2)
    void testPermissionCreateItem_BidderDenied() throws Exception {
        String token = registerAndLogin("bidderAAA", "BIDDER");

        Response res = createItem(token, artItemData("Laptop", 100));
        assertEquals("ERROR", res.getStatus());
    }

    @Test
    @Order(3)
    void testPermissionCreateAuction_BidderDenied() throws Exception {
        String token = registerAndLogin("bidderBBB", "BIDDER");

        Response res = createAuction(token, "fake", "Denied Auction");
        assertEquals("ERROR", res.getStatus());
    }

    @Test
    @Order(4)
    void testSellerCannotCreateAuctionForOtherItem() throws Exception {
        String tokenA = registerAndLogin("sellerAAA", "SELLER");

        Response itemRes = createItem(tokenA, artItemData("ItemA", 10));
        assertEquals("OK", itemRes.getStatus(), itemRes.getMessage());
        String itemId = ((Map<String, Object>) itemRes.getData()).get("id").toString();

        String tokenB = registerAndLogin("sellerBBB", "SELLER");

        Response res = createAuction(tokenB, itemId, "Other Seller Auction");
        assertEquals("ERROR", res.getStatus());
    }

    @Test
    @Order(5)
    void testStartAuctionAlreadyRunning() throws Exception {
        String token = registerAndLogin("sellerCCC", "SELLER");

        Response itemRes = createItem(token, artItemData("Item", 10));
        assertEquals("OK", itemRes.getStatus(), itemRes.getMessage());
        String itemId = ((Map<String, Object>) itemRes.getData()).get("id").toString();

        Response auctionRes = createAuction(token, itemId, "Auction");
        assertEquals("OK", auctionRes.getStatus(), auctionRes.getMessage());
        String auctionId = ((Map<String, Object>) auctionRes.getData()).get("id").toString();

        Response firstStart = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"START_AUCTION\",\"token\":\"" + token
                        + "\",\"data\":{\"auctionId\":\"" + auctionId + "\"}}");
        assertEquals("OK", firstStart.getStatus(), firstStart.getMessage());

        Response res = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"START_AUCTION\",\"token\":\"" + token
                        + "\",\"data\":{\"auctionId\":\"" + auctionId + "\"}}");
        assertEquals("ERROR", res.getStatus());
    }

    @Test
    @Order(6)
    void testGetAuctionsNoAuth() throws Exception {
        Response res = TestHelper.sendRawRequest(PORT, "{\"action\":\"GET_AUCTIONS\"}");
        assertEquals("ERROR", res.getStatus());
    }

    @Test
    @Order(7)
    void testCreateItemWithAllTypes() throws Exception {
        String token = registerAndLogin("sellerDDD", "SELLER");

        Response r1 = createItem(token, electronicsItemData("PC", 100));
        Response r2 = createItem(token, artItemData("Painting", 200));
        Response r3 = createItem(token, vehicleItemData("Car", 1000));

        assertEquals("OK", r1.getStatus(), r1.getMessage());
        assertEquals("OK", r2.getStatus(), r2.getMessage());
        assertEquals("OK", r3.getStatus(), r3.getMessage());
    }

    @Test
    @Order(8)
    void testMultipleSellersCreateItems() throws Exception {
        String tokenA = registerAndLogin("sellerEEE", "SELLER");

        createItem(tokenA, artItemData("Item1", 10));
        createItem(tokenA, artItemData("Item2", 20));

        String tokenB = registerAndLogin("sellerFFF", "SELLER");
        createItem(tokenB, artItemData("Item3", 30));

        Response res = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"GET_AUCTIONS\",\"token\":\"" + tokenA + "\"}");
        assertEquals("OK", res.getStatus(), res.getMessage());
    }

    private Response createItem(String token, String dataJson) throws Exception {
        return TestHelper.sendRawRequest(PORT,
                "{\"action\":\"CREATE_ITEM\",\"token\":\"" + token + "\",\"data\":" + dataJson + "}");
    }

    private Response createAuction(String token, String itemId, String title) throws Exception {
        String startTime = LocalDateTime.now().plusMinutes(5).toString();
        String endTime = LocalDateTime.now().plusDays(1).toString();
        return TestHelper.sendRawRequest(PORT,
                "{\"action\":\"CREATE_AUCTION\",\"token\":\"" + token
                        + "\",\"data\":{\"itemId\":\"" + itemId
                        + "\",\"title\":\"" + title
                        + "\",\"description\":\"Desc\",\"startTime\":\"" + startTime
                        + "\",\"endTime\":\"" + endTime + "\"}}");
    }

    private String artItemData(String name, double price) {
        return "{\"name\":\"" + name
                + "\",\"description\":\"Desc\",\"type\":\"ART\",\"startingPrice\":" + price
                + ",\"condition\":\"GOOD\",\"extraFields\":{\"artist\":\"UET\",\"year\":2024,\"medium\":\"Oil\"}}";
    }

    private String electronicsItemData(String name, double price) {
        return "{\"name\":\"" + name
                + "\",\"description\":\"Desc\",\"type\":\"ELECTRONICS\",\"startingPrice\":" + price
                + ",\"condition\":\"GOOD\",\"extraFields\":{\"brand\":\"UET\",\"warrantyMonths\":12}}";
    }

    private String vehicleItemData(String name, double price) {
        return "{\"name\":\"" + name
                + "\",\"description\":\"Desc\",\"type\":\"VEHICLE\",\"startingPrice\":" + price
                + ",\"condition\":\"GOOD\",\"extraFields\":{\"make\":\"VinFast\",\"model\":\"VF\",\"mileage\":0,\"vehicleYear\":2024}}";
    }
}
