package com.auctionuet.server.network.controller;

import com.auctionuet.protocol.ActionType;
import com.auctionuet.protocol.Request;
import com.auctionuet.protocol.Response;
import com.auctionuet.server.domain.manager.SessionManager;
import com.auctionuet.server.domain.model.Admin;
import com.auctionuet.server.domain.model.Bidder;
import com.auctionuet.server.domain.service.AdminService;
import com.auctionuet.server.domain.service.FinancialAuditService;
import com.auctionuet.server.domain.service.TransactionService;
import com.auctionuet.server.persistence.dao.AuctionDAO;
import com.auctionuet.server.persistence.dao.TransactionDAO;
import com.auctionuet.server.persistence.dao.UserDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminControllerFinancialAuditTest {
    private static final String USER_FILE = "data/test_users_admin_financial_controller.json";
    private static final String TRANSACTION_FILE = "data/test_transactions_admin_financial_controller.json";
    private static final String AUCTION_FILE = "data/test_auctions_admin_financial_controller.json";

    private final List<String> tokens = new ArrayList<>();
    private AdminController controller;

    @BeforeEach
    void setup() {
        cleanupFiles();
        UserDAO userDAO = new UserDAO(USER_FILE);
        TransactionDAO transactionDAO = new TransactionDAO(TRANSACTION_FILE);
        AuctionDAO auctionDAO = new AuctionDAO(AUCTION_FILE);
        FinancialAuditService financialAuditService = new FinancialAuditService(
                userDAO,
                transactionDAO,
                auctionDAO,
                new TransactionService(transactionDAO));
        controller = new AdminController(new AdminService(userDAO), null, null, financialAuditService);
    }

    @AfterEach
    void cleanup() {
        for (String token : tokens) {
            SessionManager.getInstance().removeSession(token);
        }
        cleanupFiles();
    }

    @Test
    void testAdminCanViewFinancialAuditActions() {
        String token = SessionManager.getInstance().createSession(new Admin("admin1", "admin"));
        tokens.add(token);

        Response summary = controller.handleGetFinancialSummary(
                new Request(ActionType.GET_FINANCIAL_SUMMARY, null, token));
        Response transactions = controller.handleGetGlobalTransactions(
                new Request(ActionType.GET_GLOBAL_TRANSACTIONS, null, token));

        assertEquals("OK", summary.getStatus());
        assertEquals("OK", transactions.getStatus());
    }

    @Test
    void testBidderCannotViewFinancialAuditActions() {
        String token = SessionManager.getInstance().createSession(new Bidder("bidder1", "bidder"));
        tokens.add(token);

        assertThrows(IllegalArgumentException.class, () -> controller.handleGetFinancialSummary(
                new Request(ActionType.GET_FINANCIAL_SUMMARY, null, token)));
        assertThrows(IllegalArgumentException.class, () -> controller.handleGetGlobalTransactions(
                new Request(ActionType.GET_GLOBAL_TRANSACTIONS, null, token)));
    }

    private void cleanupFiles() {
        new File(USER_FILE).delete();
        new File(TRANSACTION_FILE).delete();
        new File(AUCTION_FILE).delete();
    }
}
