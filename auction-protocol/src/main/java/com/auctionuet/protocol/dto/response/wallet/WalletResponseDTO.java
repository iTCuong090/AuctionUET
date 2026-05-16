package com.auctionuet.protocol.dto.response.wallet;

import com.auctionuet.protocol.dto.ValidatableDTO;
import com.auctionuet.protocol.dto.response.user.UserDTO;

public class WalletResponseDTO implements ValidatableDTO {
    private final UserDTO owner;
    private final double balance;
    private final double frozenBalance;
    private final double totalBalance;

    public WalletResponseDTO(UserDTO owner, double balance, double frozenBalance) {
        this.owner = owner;
        this.balance = balance;
        this.frozenBalance = frozenBalance;
        this.totalBalance = balance + frozenBalance;
        validate();
    }

    @Override
    public void validate() {
        if (owner == null) {
            throw new IllegalArgumentException("owner must not be null");
        }
        if (balance < 0) {
            throw new IllegalArgumentException("balance must be greater than or equal to 0");
        }
        if (frozenBalance < 0) {
            throw new IllegalArgumentException("frozenBalance must be greater than or equal to 0");
        }
    }

    public UserDTO getOwner() { return owner; }
    public double getBalance() { return balance; }
    public double getFrozenBalance() { return frozenBalance; }
    public double getTotalBalance() { return totalBalance; }
}
