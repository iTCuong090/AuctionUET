package com.auctionuet.protocol.dto.response;

public class WalletResponseDTO {
    private double balance;
    private double frozenBalance;

    public WalletResponseDTO(double balance, double frozenBalance) {
        this.balance = balance;
        this.frozenBalance = frozenBalance;
    }

    public double getBalance() { return balance; }
    public double getFrozenBalance() { return frozenBalance; }
}

