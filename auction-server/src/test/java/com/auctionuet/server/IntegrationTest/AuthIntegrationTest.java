package com.auctionuet.server.IntegrationTest;

import com.auctionuet.protocol.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthIntegrationTest {

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
    void cleanBefore() {
        TestHelper.cleanTestData();
    }

    @Test
    @Order(1)
    @DisplayName("Test 1: Full Auth Flow")
    void testFullAuthFlow() throws Exception {
        Response regRes = register("testuser", "test12345", "test@uet.vn", "BIDDER");
        assertEquals("OK", regRes.getStatus());

        Response loginRes = login("testuser", "test12345");
        assertEquals("OK", loginRes.getStatus());

        assertNotNull(loginRes.getData());
        Map<String, Object> data = (Map<String, Object>) loginRes.getData();

        assertNotNull(data.get("token"));
        String token = data.get("token").toString();
        assertFalse(token.isEmpty());

        Map<String, Object> user = (Map<String, Object>) data.get("user");
        assertEquals("testuser", user.get("username"));
        assertEquals("BIDDER", user.get("role"));
        assertNull(user.get("password"));
        assertNull(user.get("hashedPassword"));

        Response ping = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"PING\",\"token\":\"" + token + "\"}");
        assertEquals("OK", ping.getStatus());

        Response logoutRes = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"LOGOUT\",\"token\":\"" + token + "\"}");
        assertEquals("OK", logoutRes.getStatus());

        Response afterLogout = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"PING\",\"token\":\"" + token + "\"}");
        assertEquals("ERROR", afterLogout.getStatus());
    }

    @Test
    @Order(2)
    @DisplayName("Test 2: Register Duplicate Username")
    void testRegisterDuplicateUsername() throws Exception {
        Response res1 = register("cuong", "12345678", "cuong@uet.vn", "BIDDER");
        assertEquals("OK", res1.getStatus());

        Response res2 = register("cuong", "12345678", "cuong2@uet.vn", "BIDDER");
        assertEquals("ERROR", res2.getStatus());
        assertNotNull(res2.getMessage());
    }

    @Test
    @Order(3)
    @DisplayName("Test 3: Login Wrong Password")
    void testLoginWrongPassword() throws Exception {
        register("cuong", "correct123", "cuong@uet.vn", "BIDDER");

        Response res = login("cuong", "wrongpass");
        assertEquals("ERROR", res.getStatus());
        assertNotNull(res.getMessage());
    }

    @Test
    @Order(4)
    @DisplayName("Test 4: Login Non-existent User")
    void testLoginNonExistentUser() throws Exception {
        Response res = login("ghost", "12345678");
        assertEquals("ERROR", res.getStatus());
        assertNotNull(res.getMessage());
    }

    @Test
    @Order(5)
    @DisplayName("Test 5: Invalid Request")
    void testInvalidRequest() throws Exception {
        Response res1 = TestHelper.sendRawRequest(PORT, "this is not json");
        assertNotNull(res1);
        assertEquals("ERROR", res1.getStatus());

        Response res2 = TestHelper.sendRawRequest(PORT, "{\"data\":{}}");
        assertEquals("ERROR", res2.getStatus());
    }

    @Test
    @Order(6)
    @DisplayName("Test 6: Multiple Clients Login")
    void testMultipleClientsLogin() throws Exception {
        register("userA", "12345678", "userA@uet.vn", "BIDDER");
        Response resA = login("userA", "12345678");
        assertEquals("OK", resA.getStatus());
        String tokenA = ((Map<String, Object>) resA.getData()).get("token").toString();

        register("userB", "12345678", "userB@uet.vn", "BIDDER");
        Response resB = login("userB", "12345678");
        assertEquals("OK", resB.getStatus());
        String tokenB = ((Map<String, Object>) resB.getData()).get("token").toString();

        assertNotEquals(tokenA, tokenB);

        Response pingA = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"PING\",\"token\":\"" + tokenA + "\"}");
        Response pingB = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"PING\",\"token\":\"" + tokenB + "\"}");

        assertEquals("OK", pingA.getStatus());
        assertEquals("OK", pingB.getStatus());
    }

    @Test
    @Order(7)
    @DisplayName("Test 7: Ping No Auth")
    void testPingNoAuth() throws Exception {
        Response res = TestHelper.sendRawRequest(PORT, "{\"action\":\"PING\"}");
        assertEquals("OK", res.getStatus());
        assertEquals("PONG", res.getMessage());
    }

    private Response register(String username, String password, String email, String role) throws Exception {
        return TestHelper.sendRawRequest(PORT,
                "{\"action\":\"REGISTER\",\"data\":{\"username\":\"" + username
                        + "\",\"password\":\"" + password
                        + "\",\"email\":\"" + email
                        + "\",\"role\":\"" + role + "\"}}");
    }

    private Response login(String username, String password) throws Exception {
        return TestHelper.sendRawRequest(PORT,
                "{\"action\":\"LOGIN\",\"data\":{\"username\":\"" + username
                        + "\",\"password\":\"" + password + "\"}}");
    }
}
