package com.auctionuet.server.persistence.schema;

import com.auctionuet.protocol.enums.TransactionType;

import java.time.LocalDateTime;

public class TransactionSchema extends BaseSchema {
    private String userId;
    private String usernameSnapshot;
    private TransactionType type;
    private double amount;
    private String auctionId;
    private String relatedTransactionId;
    private Double balanceBefore;
    private Double balanceAfter;
    private Double frozenBalanceBefore;
    private Double frozenBalanceAfter;
    private String description;

    public TransactionSchema() {
    }

    public TransactionSchema(
            String id,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
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
            String description) {
        super(id, createdAt, updatedAt);
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
        this.description = description;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUsernameSnapshot() { return usernameSnapshot; }
    public void setUsernameSnapshot(String usernameSnapshot) { this.usernameSnapshot = usernameSnapshot; }
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }
    public String getRelatedTransactionId() { return relatedTransactionId; }
    public void setRelatedTransactionId(String relatedTransactionId) { this.relatedTransactionId = relatedTransactionId; }
    public Double getBalanceBefore() { return balanceBefore; }
    public void setBalanceBefore(Double balanceBefore) { this.balanceBefore = balanceBefore; }
    public Double getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(Double balanceAfter) { this.balanceAfter = balanceAfter; }
    public Double getFrozenBalanceBefore() { return frozenBalanceBefore; }
    public void setFrozenBalanceBefore(Double frozenBalanceBefore) { this.frozenBalanceBefore = frozenBalanceBefore; }
    public Double getFrozenBalanceAfter() { return frozenBalanceAfter; }
    public void setFrozenBalanceAfter(Double frozenBalanceAfter) { this.frozenBalanceAfter = frozenBalanceAfter; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
