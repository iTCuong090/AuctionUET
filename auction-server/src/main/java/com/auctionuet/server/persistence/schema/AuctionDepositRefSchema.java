package com.auctionuet.server.persistence.schema;

import java.time.LocalDateTime;

public class AuctionDepositRefSchema {
    private String bidderId;
    private String usernameSnapshot;
    private double amount;
    private String depositTransactionId;
    private LocalDateTime depositedAt;

    public AuctionDepositRefSchema() {
    }

    public AuctionDepositRefSchema(
            String bidderId,
            String usernameSnapshot,
            double amount,
            String depositTransactionId,
            LocalDateTime depositedAt) {
        this.bidderId = bidderId;
        this.usernameSnapshot = usernameSnapshot;
        this.amount = amount;
        this.depositTransactionId = depositTransactionId;
        this.depositedAt = depositedAt;
    }

    public String getBidderId() { return bidderId; }
    public void setBidderId(String bidderId) { this.bidderId = bidderId; }
    public String getUsernameSnapshot() { return usernameSnapshot; }
    public void setUsernameSnapshot(String usernameSnapshot) { this.usernameSnapshot = usernameSnapshot; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getDepositTransactionId() { return depositTransactionId; }
    public void setDepositTransactionId(String depositTransactionId) { this.depositTransactionId = depositTransactionId; }
    public LocalDateTime getDepositedAt() { return depositedAt; }
    public void setDepositedAt(LocalDateTime depositedAt) { this.depositedAt = depositedAt; }
}
