package com.auctionuet.protocol.dto.response.wallet;

import com.auctionuet.protocol.dto.ValidatableDTO;

public class WalletResponseDTO implements ValidatableDTO {
    private final double balance;
    private final double frozenBalance;

    public WalletResponseDTO(double balance, double frozenBalance) {
        this.balance = balance;
        this.frozenBalance = frozenBalance;
        validate();
    }

    @Override
    public void validate() {
        if (balance < 0) {
            throw new IllegalArgumentException("balance must be greater than or equal to 0");
        }
        if (frozenBalance < 0) {
            throw new IllegalArgumentException("frozenBalance must be greater than or equal to 0");
        }
    }

    public double getBalance() { return balance; }
    public double getFrozenBalance() { return frozenBalance; }
}
