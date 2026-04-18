package com.auctionuet.server.IntegrationTest;

import com.auctionuet.server.network.protocol.Response;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuctionIntegrationTest {

    private static final int PORT = 9999;

    @BeforeAll
    static void setup() throws Exception {
        TestHelper.startTestServer(PORT);
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

    // ================= HELPER =================
    private String registerAndLogin(String username, String role) throws Exception {

        Response reg = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"REGISTER\",\"data\":{\"username\":\"" + username + "\",\"password\":\"12345678\",\"role\":\"" + role + "\"}}");

        assertEquals("OK", reg.getStatus(), reg.getMessage());

        Response login = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"LOGIN\",\"data\":{\"username\":\"" + username + "\",\"password\":\"12345678\"}}");

        assertEquals("OK", login.getStatus(), login.getMessage());

        return ((Map<String, Object>) login.getData()).get("token").toString();
    }

    // ================= FULL FLOW =================
    @Test
    @Order(1)
    void testFullAuctionFlow() throws Exception {

        String sToken = registerAndLogin("seller123", "SELLER");

        Response itemRes = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"CREATE_ITEM\",\"token\":\"" + sToken + "\",\"data\":{\"name\":\"Vase\",\"type\":\"ART\",\"startingPrice\":100,\"artist\":\"UET\"}}");

        assertEquals("OK", itemRes.getStatus(), itemRes.getMessage());
        Map<String, Object> item = (Map<String, Object>) itemRes.getData();
        assertNotNull(item);
        String itemId = item.get("id").toString();

        Response auctionRes = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"CREATE_AUCTION\",\"token\":\"" + sToken + "\",\"data\":{\"itemId\":\"" + itemId + "\"}}");

        assertEquals("OK", auctionRes.getStatus());
        Map<String, Object> auction = (Map<String, Object>) auctionRes.getData();
        assertNotNull(auction);
        String auctionId = auction.get("id").toString();
        assertEquals("OPEN", auction.get("status").toString());

        Response startRes = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"START_AUCTION\",\"token\":\"" + sToken + "\",\"data\":{\"auctionId\":\"" + auctionId + "\"}}");

        assertEquals("OK", startRes.getStatus());

        String bToken = registerAndLogin("bidder123", "BIDDER");

        Response listRes = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"GET_AUCTIONS\",\"token\":\"" + bToken + "\"}");

        assertEquals("OK", listRes.getStatus());
        List<Map<String, Object>> list = (List<Map<String, Object>>) listRes.getData();
        assertNotNull(list);
        assertFalse(list.isEmpty());

        Response detailRes = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"GET_AUCTION_DETAIL\",\"token\":\"" + bToken + "\",\"data\":{\"id\":\"" + auctionId + "\"}}");

        assertEquals("OK", detailRes.getStatus());
        Map<String, Object> detail = (Map<String, Object>) detailRes.getData();
        assertNotNull(detail);
        assertEquals("RUNNING", detail.get("status").toString());
    }

    // ================= PERMISSION =================
    @Test
    @Order(2)
    void testPermissionCreateItem_BidderDenied() throws Exception {

        String token = registerAndLogin("bidderAAA", "BIDDER");

        Response res = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"CREATE_ITEM\",\"token\":\"" + token + "\",\"data\":{\"name\":\"Laptop\"}}");

        assertEquals("ERROR", res.getStatus());
    }

    @Test
    @Order(3)
    void testPermissionCreateAuction_BidderDenied() throws Exception {

        String token = registerAndLogin("bidderBBB", "BIDDER");

        Response res = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"CREATE_AUCTION\",\"token\":\"" + token + "\",\"data\":{\"itemId\":\"fake\"}}");

        assertEquals("ERROR", res.getStatus());
    }

    // ================= BUSINESS =================
    @Test
    @Order(4)
    void testSellerCannotCreateAuctionForOtherItem() throws Exception {

        String tokenA = registerAndLogin("sellerAAA", "SELLER");

        Response itemRes = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"CREATE_ITEM\",\"token\":\"" + tokenA + "\",\"data\":{\"name\":\"ItemA\",\"type\":\"ART\",\"startingPrice\":10,\"artist\":\"UET\"}}");

        assertEquals("OK", itemRes.getStatus());
        String itemId = ((Map<String, Object>) itemRes.getData()).get("id").toString();

        String tokenB = registerAndLogin("sellerBBB", "SELLER");

        Response res = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"CREATE_AUCTION\",\"token\":\"" + tokenB + "\",\"data\":{\"itemId\":\"" + itemId + "\"}}");

        assertEquals("ERROR", res.getStatus());
    }

    // ================= STATE =================
    @Test
    @Order(5)
    void testStartAuctionAlreadyRunning() throws Exception {

        String token = registerAndLogin("sellerCCC", "SELLER");

        Response itemRes = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"CREATE_ITEM\",\"token\":\"" + token + "\",\"data\":{\"name\":\"Item\",\"type\":\"ART\",\"startingPrice\":10,\"artist\":\"UET\"}}");

        assertEquals("OK", itemRes.getStatus());
        String itemId = ((Map<String, Object>) itemRes.getData()).get("id").toString();

        Response auctionRes = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"CREATE_AUCTION\",\"token\":\"" + token + "\",\"data\":{\"itemId\":\"" + itemId + "\"}}");

        assertEquals("OK", auctionRes.getStatus());
        String auctionId = ((Map<String, Object>) auctionRes.getData()).get("id").toString();

        TestHelper.sendRawRequest(PORT,
                "{\"action\":\"START_AUCTION\",\"token\":\"" + token + "\",\"data\":{\"auctionId\":\"" + auctionId + "\"}}");

        Response res = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"START_AUCTION\",\"token\":\"" + token + "\",\"data\":{\"auctionId\":\"" + auctionId + "\"}}");

        assertEquals("ERROR", res.getStatus());
    }

    // ================= AUTH =================
    @Test
    @Order(6)
    void testGetAuctionsNoAuth() throws Exception {

        Response res = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"GET_AUCTIONS\"}");

        assertEquals("ERROR", res.getStatus());
    }

    // ================= ITEM TYPES =================
    @Test
    @Order(7)
    void testCreateItemWithAllTypes() throws Exception {

        String token = registerAndLogin("sellerDDD", "SELLER");

        Response r1 = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"CREATE_ITEM\",\"token\":\"" + token + "\",\"data\":{\"type\":\"ELECTRONICS\",\"name\":\"PC\",\"startingPrice\":100,\"brand\":\"UET\"}}");

        Response r2 = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"CREATE_ITEM\",\"token\":\"" + token + "\",\"data\":{\"type\":\"ART\",\"name\":\"Painting\",\"startingPrice\":200,\"artist\":\"UET\"}}");

        Response r3 = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"CREATE_ITEM\",\"token\":\"" + token + "\",\"data\":{\"type\":\"VEHICLE\",\"name\":\"Car\",\"startingPrice\":1000,\"make\":\"VinFast\"}}");

        assertEquals("OK", r1.getStatus(), r1.getMessage());
        assertEquals("OK", r2.getStatus(), r2.getMessage());
        assertEquals("OK", r3.getStatus(), r3.getMessage());
    }

    // ================= MULTI SELLER =================
    @Test
    @Order(8)
    void testMultipleSellersCreateItems() throws Exception {

        String tokenA = registerAndLogin("sellerEEE", "SELLER");

        TestHelper.sendRawRequest(PORT,
                "{\"action\":\"CREATE_ITEM\",\"token\":\"" + tokenA + "\",\"data\":{\"name\":\"Item1\",\"type\":\"ART\",\"startingPrice\":10,\"artist\":\"UET\"}}");

        TestHelper.sendRawRequest(PORT,
                "{\"action\":\"CREATE_ITEM\",\"token\":\"" + tokenA + "\",\"data\":{\"name\":\"Item2\",\"type\":\"ART\",\"startingPrice\":20,\"artist\":\"UET\"}}");

        String tokenB = registerAndLogin("sellerFFF", "SELLER");

        TestHelper.sendRawRequest(PORT,
                "{\"action\":\"CREATE_ITEM\",\"token\":\"" + tokenB + "\",\"data\":{\"name\":\"Item3\",\"type\":\"ART\",\"startingPrice\":30,\"artist\":\"UET\"}}");

        Response res = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"GET_AUCTIONS\",\"token\":\"" + tokenA + "\"}");

        assertEquals("OK", res.getStatus());
    }
}