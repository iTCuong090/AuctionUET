package com.auctionuet.server.network.controller;

import com.auctionuet.protocol.ActionType;
import com.auctionuet.protocol.Request;
import com.auctionuet.protocol.Response;
import com.auctionuet.protocol.dto.request.wallet.AmountRequestDTO;
import com.auctionuet.protocol.enums.UserRole;
import com.auctionuet.server.domain.manager.SessionManager;
import com.auctionuet.server.domain.model.Admin;
import com.auctionuet.server.domain.model.Bidder;
import com.auctionuet.server.domain.service.TransactionService;
import com.auctionuet.server.domain.service.UserService;
import com.auctionuet.server.domain.service.WalletService;
import com.auctionuet.server.persistence.dao.TransactionDAO;
import com.auctionuet.server.persistence.dao.UserDAO;
import com.auctionuet.server.persistence.schema.UserSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WalletControllerTest {
    private static final String USER_FILE = "data/test_users_wallet_controller.json";
    private static final String TRANSACTION_FILE = "data/test_transactions_wallet_controller.json";

    private final List<String> tokens = new ArrayList<>();
    private WalletController controller;
    private UserDAO userDAO;

    @BeforeEach
    void setup() {
        cleanupFiles();
        userDAO = new UserDAO(USER_FILE);
        TransactionDAO transactionDAO = new TransactionDAO(TRANSACTION_FILE);
        UserService userService = new UserService(userDAO);
        controller = new WalletController(
                new WalletService(userDAO, userService, new TransactionService(transactionDAO)));
    }

    @AfterEach
    void cleanup() {
        for (String token : tokens) {
            SessionManager.getInstance().removeSession(token);
        }
        cleanupFiles();
    }

    @Test
    void testAdminCannotAccessPersonalWalletActions() throws Exception {
        String token = sessionForAdmin();

        assertEquals("ERROR", controller.handleDeposit(amountRequest(ActionType.DEPOSIT, token)).getStatus());
        assertEquals("ERROR", controller.handleWithdraw(amountRequest(ActionType.WITHDRAW, token)).getStatus());
        assertEquals("ERROR", controller.handleGetWallet(new Request(ActionType.GET_WALLET, null, token)).getStatus());
        assertEquals("ERROR", controller.handleGetMyTransactions(
                new Request(ActionType.GET_MY_TRANSACTIONS, null, token)).getStatus());
    }

    @Test
    void testBidderCanStillDepositAndViewWallet() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        userDAO.save(new UserSchema(
                "bidder1", now, now, "alice", "hash", "salt", "alice@uet.vn", UserRole.BIDDER));
        String token = SessionManager.getInstance().createSession(new Bidder("bidder1", "alice"));
        tokens.add(token);

        Response deposit = controller.handleDeposit(amountRequest(ActionType.DEPOSIT, token));
        Response wallet = controller.handleGetWallet(new Request(ActionType.GET_WALLET, null, token));

        assertEquals("OK", deposit.getStatus());
        assertEquals("OK", wallet.getStatus());
    }

    private String sessionForAdmin() {
        String token = SessionManager.getInstance().createSession(new Admin("admin1", "admin"));
        tokens.add(token);
        return token;
    }

    private Request amountRequest(ActionType action, String token) {
        AmountRequestDTO dto = new AmountRequestDTO();
        dto.setAmount(100);
        return Request.fromDto(action, dto, token);
    }

    private void cleanupFiles() {
        new File(USER_FILE).delete();
        new File(TRANSACTION_FILE).delete();
    }
}
