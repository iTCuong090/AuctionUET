package com.auctionuet.server.domain.service;

import com.auctionuet.protocol.dto.response.transaction.TransactionDTO;
import com.auctionuet.protocol.enums.TransactionType;
import com.auctionuet.server.persistence.dao.TransactionDAO;
import com.auctionuet.server.persistence.schema.TransactionSchema;
import com.auctionuet.server.persistence.schema.UserSchema;
import com.auctionuet.server.util.IdGenerator;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public class TransactionService {
    private static final int DEFAULT_HISTORY_LIMIT = 100;

    private final TransactionDAO transactionDAO;

    public TransactionService(TransactionDAO transactionDAO) {
        this.transactionDAO = transactionDAO;
    }

    public TransactionSchema record(
            UserSchema user,
            TransactionType type,
            double amount,
            String auctionId,
            String relatedTransactionId,
            Double balanceBefore,
            Double balanceAfter,
            Double frozenBalanceBefore,
            Double frozenBalanceAfter,
            String description) {
        if (user == null) {
            throw new IllegalArgumentException("Khong tim thay User");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("So tien giao dich phai lon hon 0");
        }

        LocalDateTime now = LocalDateTime.now();
        TransactionSchema schema = new TransactionSchema(
                IdGenerator.generate(),
                now,
                now,
                user.getId(),
                user.getUsername(),
                type,
                amount,
                auctionId,
                relatedTransactionId,
                balanceBefore,
                balanceAfter,
                frozenBalanceBefore,
                frozenBalanceAfter,
                description);
        transactionDAO.save(schema);
        return schema;
    }

    public List<TransactionDTO> getTransactionsForUser(String userId) {
        return transactionDAO.findByUserId(userId).stream()
                .sorted(Comparator.comparing(TransactionSchema::getCreatedAt).reversed())
                .limit(DEFAULT_HISTORY_LIMIT)
                .map(this::toDTO)
                .toList();
    }

    public List<TransactionDTO> getTransactionsForAuction(String auctionId) {
        return transactionDAO.findByAuctionId(auctionId).stream()
                .sorted(Comparator.comparing(TransactionSchema::getCreatedAt).reversed())
                .map(this::toDTO)
                .toList();
    }

    public List<TransactionDTO> getAllTransactions() {
        return transactionDAO.findAll().stream()
                .sorted(Comparator.comparing(TransactionSchema::getCreatedAt).reversed())
                .map(this::toDTO)
                .toList();
    }

    public TransactionDTO toDTO(TransactionSchema schema) {
        return new TransactionDTO(
                schema.getId(),
                schema.getUserId(),
                schema.getUsernameSnapshot(),
                schema.getType(),
                schema.getAmount(),
                schema.getAuctionId(),
                schema.getRelatedTransactionId(),
                schema.getBalanceBefore(),
                schema.getBalanceAfter(),
                schema.getFrozenBalanceBefore(),
                schema.getFrozenBalanceAfter(),
                schema.getCreatedAt(),
                schema.getDescription());
    }
}
