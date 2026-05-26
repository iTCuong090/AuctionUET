package com.auctionuet.server.domain.service;

import com.auctionuet.protocol.dto.response.admin.FinancialSummaryDTO;
import com.auctionuet.protocol.dto.response.transaction.TransactionDTO;
import com.auctionuet.protocol.enums.AuctionStatus;
import com.auctionuet.protocol.enums.TransactionType;
import com.auctionuet.server.persistence.dao.AuctionDAO;
import com.auctionuet.server.persistence.dao.TransactionDAO;
import com.auctionuet.server.persistence.dao.UserDAO;
import com.auctionuet.server.persistence.schema.TransactionSchema;
import com.auctionuet.server.persistence.schema.UserSchema;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FinancialAuditService {
    private final UserDAO userDAO;
    private final TransactionDAO transactionDAO;
    private final AuctionDAO auctionDAO;
    private final TransactionService transactionService;

    public FinancialAuditService(
            UserDAO userDAO,
            TransactionDAO transactionDAO,
            AuctionDAO auctionDAO,
            TransactionService transactionService) {
        this.userDAO = userDAO;
        this.transactionDAO = transactionDAO;
        this.auctionDAO = auctionDAO;
        this.transactionService = transactionService;
    }

    public FinancialSummaryDTO getSummary() {
        List<UserSchema> users = userDAO.findAll();
        double availableBalanceTotal = users.stream()
                .mapToDouble(user -> user.getBalance())
                .sum();
        double frozenBalanceTotal = users.stream()
                .mapToDouble(user -> user.getFrozenBalance())
                .sum();
        double penaltyRevenueTotal = transactionDAO.findAll().stream()
                .filter(transaction -> transaction.getType() == TransactionType.AUCTION_DEPOSIT_FORFEIT)
                .mapToDouble(TransactionSchema::getAmount)
                .sum();
        return new FinancialSummaryDTO(availableBalanceTotal, frozenBalanceTotal, penaltyRevenueTotal);
    }

    public List<TransactionDTO> getTransactions(String userId, TransactionType type) {
        return transactionDAO.findAll().stream()
                .filter(transaction -> userId == null || userId.equals(transaction.getUserId()))
                .filter(transaction -> type == null || type == transaction.getType())
                .sorted(Comparator.comparing(TransactionSchema::getCreatedAt).reversed())
                .map(transactionService::toDTO)
                .toList();
    }

    public synchronized int migratePaidAuctionDepositTransactions() {
        Set<String> paidAuctionIds = auctionDAO.findByStatus(AuctionStatus.PAID).stream()
                .map(auction -> auction.getId())
                .collect(Collectors.toSet());
        int migratedCount = 0;
        for (TransactionSchema transaction : transactionDAO.findAll()) {
            if (transaction.getType() == TransactionType.AUCTION_DEPOSIT_FORFEIT
                    && paidAuctionIds.contains(transaction.getAuctionId())) {
                transaction.setType(TransactionType.AUCTION_DEPOSIT_APPLIED);
                transactionDAO.update(transaction);
                migratedCount++;
            }
        }
        return migratedCount;
    }
}
