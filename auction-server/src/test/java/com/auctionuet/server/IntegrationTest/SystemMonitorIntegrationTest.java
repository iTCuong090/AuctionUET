package com.auctionuet.server.IntegrationTest;

import com.auctionuet.protocol.PushActionType;
import com.auctionuet.protocol.PushMessage;
import com.auctionuet.protocol.Response;
import com.auctionuet.protocol.dto.push.PushEvents;
import com.auctionuet.protocol.dto.response.admin.SystemMonitorDTO;
import com.auctionuet.protocol.enums.AccountStatus;
import com.auctionuet.protocol.enums.AuctionStatus;
import com.auctionuet.protocol.enums.UserRole;
import com.auctionuet.protocol.util.NetworkGson;
import com.auctionuet.server.domain.manager.AuctionManager;
import com.auctionuet.server.persistence.dao.UserDAO;
import com.auctionuet.server.persistence.schema.AuctionSchema;
import com.auctionuet.server.persistence.schema.UserSchema;
import com.auctionuet.server.util.PasswordUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemMonitorIntegrationTest {
    private static final int PORT = 10001;
    private static final String AUCTION_ID = "system-monitor-integration-auction";

    @BeforeAll
    static void setupServer() throws Exception {
        TestHelper.startTestServer(PORT, false);
        Thread.sleep(500);
    }

    @AfterAll
    static void stopServer() {
        AuctionManager.getInstance().removeLiveAuction(AUCTION_ID);
        TestHelper.stopTestServer();
        TestHelper.cleanTestData();
    }

    @BeforeEach
    void setupData() {
        TestHelper.cleanTestData();
        saveAdmin();
        AuctionManager.getInstance().removeLiveAuction(AUCTION_ID);
    }

    @Test
    void testAdminReceivesConnectionAndLiveAuctionPushUpdatesUntilUnsubscribe() throws Exception {
        try (Socket adminSocket = new Socket("localhost", PORT)) {
            adminSocket.setSoTimeout(3000);
            PrintWriter out = new PrintWriter(adminSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(adminSocket.getInputStream()));

            out.println("{\"action\":\"LOGIN\",\"data\":{\"username\":\"monitor_admin\",\"password\":\"monitor@123\"}}");
            Response login = readResponse(in);
            String token = login.getDataAs(com.auctionuet.protocol.dto.response.auth.LoginResponseDTO.class).getToken();

            out.println("{\"action\":\"SUBSCRIBE_SYSTEM_MONITOR\",\"token\":\"" + token + "\"}");
            SystemMonitorDTO initial = readResponse(in).getDataAs(SystemMonitorDTO.class);
            int initialConnectionCount = initial.getActiveConnectionCount();
            int initialLiveAuctionCount = initial.getLiveAuctionCount();
            assertTrue(initialConnectionCount >= 1);

            try (Socket additionalClient = new Socket("localhost", PORT)) {
                PushEvents.SystemMonitorUpdatedPush connected = readMonitorPush(in);
                assertEquals(initialConnectionCount + 1, connected.getActiveConnectionCount());
            }
            PushEvents.SystemMonitorUpdatedPush disconnected = readMonitorPush(in);
            assertEquals(initialConnectionCount, disconnected.getActiveConnectionCount());

            AuctionManager.getInstance().loadAuction(runningAuction(), 100.0);
            PushEvents.SystemMonitorUpdatedPush auctionStarted = readMonitorPush(in);
            assertEquals(initialLiveAuctionCount + 1, auctionStarted.getLiveAuctionCount());

            AuctionManager.getInstance().removeLiveAuction(AUCTION_ID);
            PushEvents.SystemMonitorUpdatedPush auctionRemoved = readMonitorPush(in);
            assertEquals(initialLiveAuctionCount, auctionRemoved.getLiveAuctionCount());

            out.println("{\"action\":\"UNSUBSCRIBE_SYSTEM_MONITOR\",\"token\":\"" + token + "\"}");
            assertEquals("OK", readResponse(in).getStatus());

            try (Socket ignored = new Socket("localhost", PORT)) {
                ignored.getOutputStream().flush();
            }
            assertThrows(SocketTimeoutException.class, in::readLine);
        }
    }

    private Response readResponse(BufferedReader in) throws Exception {
        return NetworkGson.create().fromJson(in.readLine(), Response.class);
    }

    private PushEvents.SystemMonitorUpdatedPush readMonitorPush(BufferedReader in) throws Exception {
        PushMessage push = NetworkGson.create().fromJson(in.readLine(), PushMessage.class);
        assertEquals(PushActionType.SYSTEM_MONITOR_UPDATED, push.getPushType());
        return push.getDataAs(PushEvents.SystemMonitorUpdatedPush.class);
    }

    private void saveAdmin() {
        String salt = PasswordUtils.generateSalt();
        LocalDateTime now = LocalDateTime.now();
        UserSchema admin = new UserSchema(
                "monitor-admin",
                now,
                now,
                "monitor_admin",
                PasswordUtils.hash("monitor@123", salt),
                salt,
                "monitor@uet.vn",
                UserRole.ADMIN);
        admin.setStatus(AccountStatus.ACTIVE);
        admin.setMustChangePassword(false);
        new UserDAO().save(admin);
    }

    private AuctionSchema runningAuction() {
        LocalDateTime now = LocalDateTime.now();
        return new AuctionSchema(
                AUCTION_ID,
                now,
                now,
                "monitor-item",
                "monitor-seller",
                "Phiên monitor",
                "Kiểm thử realtime",
                now.minusMinutes(1),
                now.plusMinutes(5),
                AuctionStatus.RUNNING,
                0,
                null,
                60,
                120);
    }
}
