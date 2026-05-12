package com.auctionuet.protocol.dto.request.wallet;

import com.auctionuet.protocol.dto.ValidatableDTO;

public class AmountRequestDTO implements ValidatableDTO {
    private double amount;

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    @Override
    public void validate() {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }
    }
}
