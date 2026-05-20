package com.auctionuet.server.persistence.dao;

import com.auctionuet.protocol.enums.TransactionType;
import com.auctionuet.server.persistence.schema.TransactionSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TransactionDAOTest {
    private final String filePath = "data/test_transactions_dao.json";
    private final TransactionDAO transactionDAO = new TransactionDAO(filePath);

    @AfterEach
    void cleanup() {
        new File(filePath).delete();
    }

    @Test
    void testSaveAndFindByUserAndAuction() {
        LocalDateTime now = LocalDateTime.now();
        TransactionSchema first = new TransactionSchema(
                "tx1",
                now,
                now,
                "user1",
                "alice",
                TransactionType.WALLET_DEPOSIT,
                100.0,
                null,
                null,
                0.0,
                100.0,
                0.0,
                0.0,
                "Nap tien");
        TransactionSchema second = new TransactionSchema(
                "tx2",
                now,
                now,
                "user1",
                "alice",
                TransactionType.AUCTION_DEPOSIT_HOLD,
                10.0,
                "auction1",
                null,
                100.0,
                90.0,
                0.0,
                10.0,
                "Giu coc");

        transactionDAO.save(first);
        transactionDAO.save(second);

        assertNotNull(transactionDAO.findById("tx1"));
        assertEquals(2, transactionDAO.findByUserId("user1").size());
        assertEquals(1, transactionDAO.findByAuctionId("auction1").size());
    }
}
