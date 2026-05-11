package com.auctionuet.server.IntegrationTest;

import com.auctionuet.protocol.Response;
import org.junit.jupiter.api.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthIntegrationTest {

    private static final int PORT = 9999;

    // ================= SETUP =================
    @BeforeAll
    static void setup() throws Exception {
        TestHelper.startTestServer(PORT);
        Thread.sleep(500); // chờ server start
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

    // ================= TEST 1 =================
    @Test
    @Order(1)
    @DisplayName("Test 1: Full Auth Flow")
    void testFullAuthFlow() throws Exception {

        // REGISTER
        String regJson = "{\"action\":\"REGISTER\",\"data\":{\"username\":\"testuser\",\"password\":\"test12345\",\"email\":\"test@uet.vn\",\"role\":\"BIDDER\"}}";
        Response regRes = TestHelper.sendRawRequest(PORT, regJson);
        assertEquals("OK", regRes.getStatus());

        // LOGIN
        String loginJson = "{\"action\":\"LOGIN\",\"data\":{\"username\":\"testuser\",\"password\":\"test12345\"}}";
        Response loginRes = TestHelper.sendRawRequest(PORT, loginJson);
        assertEquals("OK", loginRes.getStatus());

        assertNotNull(loginRes.getData());
        Map<String, Object> data = (Map<String, Object>) loginRes.getData();

        // TOKEN
        assertNotNull(data.get("token"));
        String token = data.get("token").toString();
        assertFalse(token.isEmpty());

        // USER
        Map<String, Object> user = (Map<String, Object>) data.get("user");
        assertEquals("testuser", user.get("username"));
        assertEquals("BIDDER", user.get("role"));

        // KHÔNG trả password
        assertNull(user.get("password"));
        assertNull(user.get("hashedPassword"));

        // VALIDATE TOKEN (PING có auth)
        Response ping = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"PING\",\"token\":\"" + token + "\"}");
        assertEquals("OK", ping.getStatus());

        // LOGOUT
        Response logoutRes = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"LOGOUT\",\"token\":\"" + token + "\"}");
        assertEquals("OK", logoutRes.getStatus());

        // TOKEN hết hạn
        Response afterLogout = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"PING\",\"token\":\"" + token + "\"}");
        assertEquals("ERROR", afterLogout.getStatus());
    }

    // ================= TEST 2 =================
    @Test
    @Order(2)
    @DisplayName("Test 2: Register Duplicate Username")
    void testRegisterDuplicateUsername() throws Exception {

        String regJson = "{\"action\":\"REGISTER\",\"data\":{\"username\":\"cuong\",\"password\":\"12345678\"}}";

        Response res1 = TestHelper.sendRawRequest(PORT, regJson);
        assertEquals("OK", res1.getStatus());

        Response res2 = TestHelper.sendRawRequest(PORT, regJson);
        assertEquals("ERROR", res2.getStatus());

        assertNotNull(res2.getMessage());
        assertTrue(res2.getMessage().toLowerCase().contains("tồn tại"));
    }

    // ================= TEST 3 =================
    @Test
    @Order(3)
    @DisplayName("Test 3: Login Wrong Password")
    void testLoginWrongPassword() throws Exception {

        TestHelper.sendRawRequest(PORT,
                "{\"action\":\"REGISTER\",\"data\":{\"username\":\"cuong\",\"password\":\"correct123\"}}");

        Response res = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"LOGIN\",\"data\":{\"username\":\"cuong\",\"password\":\"wrongpass\"}}");

        assertEquals("ERROR", res.getStatus());
        assertNotNull(res.getMessage());
        assertTrue(res.getMessage().toLowerCase().contains("mật khẩu"));
    }

    // ================= TEST 4 =================
    @Test
    @Order(4)
    @DisplayName("Test 4: Login Non-existent User")
    void testLoginNonExistentUser() throws Exception {

        Response res = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"LOGIN\",\"data\":{\"username\":\"ghost\",\"password\":\"12345678\"}}");

        assertEquals("ERROR", res.getStatus());
        assertNotNull(res.getMessage());
        assertTrue(res.getMessage().toLowerCase().contains("không tồn tại"));
    }

    // ================= TEST 5 =================
    @Test
    @Order(5)
    @DisplayName("Test 5: Invalid Request")
    void testInvalidRequest() throws Exception {

        // JSON rác
        Response res1 = TestHelper.sendRawRequest(PORT, "this is not json");
        assertNotNull(res1);
        assertEquals("ERROR", res1.getStatus());

        // JSON hợp lệ nhưng sai format
        Response res2 = TestHelper.sendRawRequest(PORT, "{\"data\":{}}");
        assertEquals("ERROR", res2.getStatus());
    }

    // ================= TEST 6 =================
    @Test
    @Order(6)
    @DisplayName("Test 6: Multiple Clients Login")
    void testMultipleClientsLogin() throws Exception {

        // CLIENT A
        TestHelper.sendRawRequest(PORT,
                "{\"action\":\"REGISTER\",\"data\":{\"username\":\"userA\",\"password\":\"12345678\"}}");
        Response resA = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"LOGIN\",\"data\":{\"username\":\"userA\",\"password\":\"12345678\"}}");

        assertEquals("OK", resA.getStatus());
        String tokenA = ((Map<String, Object>) resA.getData()).get("token").toString();

        // CLIENT B
        TestHelper.sendRawRequest(PORT,
                "{\"action\":\"REGISTER\",\"data\":{\"username\":\"userB\",\"password\":\"12345678\"}}");
        Response resB = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"LOGIN\",\"data\":{\"username\":\"userB\",\"password\":\"12345678\"}}");

        assertEquals("OK", resB.getStatus());
        String tokenB = ((Map<String, Object>) resB.getData()).get("token").toString();

        // Token khác nhau
        assertNotEquals(tokenA, tokenB);

        // Token đều valid
        Response pingA = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"PING\",\"token\":\"" + tokenA + "\"}");
        Response pingB = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"PING\",\"token\":\"" + tokenB + "\"}");

        assertEquals("OK", pingA.getStatus());
        assertEquals("OK", pingB.getStatus());
    }

    // ================= TEST 7 =================
    @Test
    @Order(7)
    @DisplayName("Test 7: Ping No Auth")
    void testPingNoAuth() throws Exception {

        Response res = TestHelper.sendRawRequest(PORT,
                "{\"action\":\"PING\"}");

        assertEquals("OK", res.getStatus());
        assertEquals("PONG", res.getMessage());
    }
}
