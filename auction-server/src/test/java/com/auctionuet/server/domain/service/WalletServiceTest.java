package com.auctionuet.server.domain.service;

import com.auctionuet.protocol.enums.TransactionType;
import com.auctionuet.protocol.enums.UserRole;
import com.auctionuet.server.persistence.dao.TransactionDAO;
import com.auctionuet.server.persistence.dao.UserDAO;
import com.auctionuet.server.persistence.schema.TransactionSchema;
import com.auctionuet.server.persistence.schema.UserSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WalletServiceTest {
    private final String userFile = "data/test_users_wallet_service.json";
    private final String transactionFile = "data/test_transactions_wallet_service.json";

    private UserDAO userDAO;
    private TransactionDAO transactionDAO;
    private WalletService walletService;

    @BeforeEach
    void setup() {
        cleanup();
        userDAO = new UserDAO(userFile);
        transactionDAO = new TransactionDAO(transactionFile);
        UserService userService = new UserService(userDAO);
        TransactionService transactionService = new TransactionService(transactionDAO);
        walletService = new WalletService(userDAO, userService, transactionService);

        LocalDateTime now = LocalDateTime.now();
        userDAO.save(new UserSchema("user1", now, now, "alice", "hash", "salt", "alice@uet.vn", UserRole.BIDDER));
    }

    @AfterEach
    void cleanup() {
        new File(userFile).delete();
        new File(transactionFile).delete();
    }

    @Test
    void testDepositAndWithdrawCreateTransactions() {
        walletService.deposit("user1", 500.0);
        walletService.withdraw("user1", 120.0);

        UserSchema user = userDAO.findById("user1");
        assertEquals(380.0, user.getBalance());

        List<TransactionSchema> transactions = transactionDAO.findByUserId("user1");
        assertEquals(2, transactions.size());
        assertEquals(TransactionType.WALLET_DEPOSIT, transactions.get(0).getType());
        assertEquals(TransactionType.WALLET_WITHDRAW, transactions.get(1).getType());
    }

    @Test
    void testAuctionDepositHoldRefundAndForfeitCreateTransactions() {
        walletService.deposit("user1", 500.0);
        String holdId = walletService.holdAuctionDeposit("user1", 50.0, "auction1", "Giu coc");
        walletService.refundAuctionDeposit("user1", 50.0, "auction1", holdId, "Hoan coc");
        String secondHoldId = walletService.holdAuctionDeposit("user1", 50.0, "auction1", "Giu coc lan hai");
        walletService.forfeitAuctionDeposit("user1", 50.0, "auction1", secondHoldId, "Tich thu coc");

        UserSchema user = userDAO.findById("user1");
        assertEquals(450.0, user.getBalance());
        assertEquals(0.0, user.getFrozenBalance());

        List<TransactionSchema> transactions = transactionDAO.findByUserId("user1");
        assertFalse(transactions.stream().noneMatch(t -> t.getType() == TransactionType.AUCTION_DEPOSIT_HOLD));
        assertFalse(transactions.stream().noneMatch(t -> t.getType() == TransactionType.AUCTION_DEPOSIT_REFUND));
        assertFalse(transactions.stream().noneMatch(t -> t.getType() == TransactionType.AUCTION_DEPOSIT_FORFEIT));
    }
}
