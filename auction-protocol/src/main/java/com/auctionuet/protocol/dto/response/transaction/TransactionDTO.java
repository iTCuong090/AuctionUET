package com.auctionuet.protocol.dto.response.transaction;

import com.auctionuet.protocol.dto.ValidatableDTO;
import com.auctionuet.protocol.enums.TransactionType;

import java.time.LocalDateTime;

public class TransactionDTO implements ValidatableDTO {
    private final String id;
    private final String userId;
    private final String usernameSnapshot;
    private final TransactionType type;
    private final double amount;
    private final String auctionId;
    private final String relatedTransactionId;
    private final Double balanceBefore;
    private final Double balanceAfter;
    private final Double frozenBalanceBefore;
    private final Double frozenBalanceAfter;
    private final LocalDateTime createdAt;
    private final String description;

    public TransactionDTO(
            String id,
            String userId,
            String usernameSnapshot,
            TransactionType type,
            double amount,
            String auctionId,
            String relatedTransactionId,
            Double balanceBefore,
            Double balanceAfter,
            Double frozenBalanceBefore,
            Double frozenBalanceAfter,
            LocalDateTime createdAt,
            String description) {
        this.id = id;
        this.userId = userId;
        this.usernameSnapshot = usernameSnapshot;
        this.type = type;
        this.amount = amount;
        this.auctionId = auctionId;
        this.relatedTransactionId = relatedTransactionId;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.frozenBalanceBefore = frozenBalanceBefore;
        this.frozenBalanceAfter = frozenBalanceAfter;
        this.createdAt = createdAt;
        this.description = description;
        validate();
    }

    @Override
    public void validate() {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        if (usernameSnapshot == null || usernameSnapshot.isBlank()) {
            throw new IllegalArgumentException("usernameSnapshot must not be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getUsernameSnapshot() { return usernameSnapshot; }
    public TransactionType getType() { return type; }
    public double getAmount() { return amount; }
    public String getAuctionId() { return auctionId; }
    public String getRelatedTransactionId() { return relatedTransactionId; }
    public Double getBalanceBefore() { return balanceBefore; }
    public Double getBalanceAfter() { return balanceAfter; }
    public Double getFrozenBalanceBefore() { return frozenBalanceBefore; }
    public Double getFrozenBalanceAfter() { return frozenBalanceAfter; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getDescription() { return description; }
}
