package com.auctionuet.server.domain.service;

import com.auctionuet.protocol.dto.response.admin.FinancialSummaryDTO;
import com.auctionuet.protocol.dto.response.transaction.TransactionDTO;
import com.auctionuet.protocol.enums.AuctionStatus;
import com.auctionuet.protocol.enums.TransactionType;
import com.auctionuet.protocol.enums.UserRole;
import com.auctionuet.server.persistence.dao.AuctionDAO;
import com.auctionuet.server.persistence.dao.TransactionDAO;
import com.auctionuet.server.persistence.dao.UserDAO;
import com.auctionuet.server.persistence.schema.AuctionSchema;
import com.auctionuet.server.persistence.schema.TransactionSchema;
import com.auctionuet.server.persistence.schema.UserSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FinancialAuditServiceTest {
    private static final String USER_FILE = "data/test_users_financial_audit.json";
    private static final String TRANSACTION_FILE = "data/test_transactions_financial_audit.json";
    private static final String AUCTION_FILE = "data/test_auctions_financial_audit.json";

    private UserDAO userDAO;
    private TransactionDAO transactionDAO;
    private AuctionDAO auctionDAO;
    private FinancialAuditService service;

    @BeforeEach
    void setup() {
        cleanupFiles();
        userDAO = new UserDAO(USER_FILE);
        transactionDAO = new TransactionDAO(TRANSACTION_FILE);
        auctionDAO = new AuctionDAO(AUCTION_FILE);
        service = new FinancialAuditService(
                userDAO,
                transactionDAO,
                auctionDAO,
                new TransactionService(transactionDAO));
    }

    @AfterEach
    void cleanup() {
        cleanupFiles();
    }

    @Test
    void testSummaryAndFiltersUseOnlyPenaltyRevenue() {
        saveUser("user1", "alice", 120.0, 30.0);
        saveUser("user2", "bob", 250.0, 20.0);
        saveTransaction("tx1", "user1", "alice", TransactionType.AUCTION_DEPOSIT_FORFEIT, 15.0, "auction1", 1);
        saveTransaction("tx2", "user1", "alice", TransactionType.AUCTION_DEPOSIT_APPLIED, 30.0, "auction2", 2);
        saveTransaction("tx3", "user2", "bob", TransactionType.WALLET_DEPOSIT, 250.0, null, 3);

        FinancialSummaryDTO summary = service.getSummary();
        assertEquals(370.0, summary.getAvailableBalanceTotal());
        assertEquals(50.0, summary.getFrozenBalanceTotal());
        assertEquals(15.0, summary.getPenaltyRevenueTotal());

        List<TransactionDTO> forfeitTransactions =
                service.getTransactions(null, TransactionType.AUCTION_DEPOSIT_FORFEIT);
        assertEquals(1, forfeitTransactions.size());
        assertEquals("tx1", forfeitTransactions.get(0).getId());

        List<TransactionDTO> userTransactions = service.getTransactions("user1", null);
        assertEquals(2, userTransactions.size());
        assertEquals("tx2", userTransactions.get(0).getId());

        List<TransactionDTO> combined =
                service.getTransactions("user1", TransactionType.AUCTION_DEPOSIT_APPLIED);
        assertEquals(1, combined.size());
        assertEquals("tx2", combined.get(0).getId());
    }

    @Test
    void testMigrationOnlyConvertsPaidAuctionDepositAndIsIdempotent() {
        saveAuction("paid-auction", AuctionStatus.PAID);
        saveAuction("canceled-auction", AuctionStatus.CANCELED);
        saveTransaction(
                "paid-deposit",
                "user1",
                "alice",
                TransactionType.AUCTION_DEPOSIT_FORFEIT,
                30.0,
                "paid-auction",
                1);
        saveTransaction(
                "penalty",
                "user2",
                "bob",
                TransactionType.AUCTION_DEPOSIT_FORFEIT,
                20.0,
                "canceled-auction",
                2);

        assertEquals(1, service.migratePaidAuctionDepositTransactions());
        assertEquals(TransactionType.AUCTION_DEPOSIT_APPLIED, transactionDAO.findById("paid-deposit").getType());
        assertEquals(TransactionType.AUCTION_DEPOSIT_FORFEIT, transactionDAO.findById("penalty").getType());
        assertEquals(0, service.migratePaidAuctionDepositTransactions());
    }

    private void saveUser(String id, String username, double balance, double frozenBalance) {
        LocalDateTime now = LocalDateTime.now();
        UserSchema user = new UserSchema(id, now, now, username, "hash", "salt", username + "@uet.vn", UserRole.BIDDER);
        user.setBalance(balance);
        user.setFrozenBalance(frozenBalance);
        userDAO.save(user);
    }

    private void saveTransaction(
            String id,
            String userId,
            String username,
            TransactionType type,
            double amount,
            String auctionId,
            int seconds) {
        LocalDateTime time = LocalDateTime.now().plusSeconds(seconds);
        transactionDAO.save(new TransactionSchema(
                id,
                time,
                time,
                userId,
                username,
                type,
                amount,
                auctionId,
                null,
                null,
                null,
                null,
                null,
                "Giao dich kiem thu"));
    }

    private void saveAuction(String id, AuctionStatus status) {
        LocalDateTime now = LocalDateTime.now();
        auctionDAO.save(new AuctionSchema(
                id,
                now,
                now,
                "item-" + id,
                "seller1",
                "Phien " + id,
                "Mo ta",
                now.minusMinutes(2),
                now.minusMinutes(1),
                status,
                0,
                null,
                60,
                120));
    }

    private void cleanupFiles() {
        new File(USER_FILE).delete();
        new File(TRANSACTION_FILE).delete();
        new File(AUCTION_FILE).delete();
    }
}
