package com.auctionuet.server.domain.service;

import com.auctionuet.protocol.dto.response.wallet.WalletResponseDTO;
import com.auctionuet.server.persistence.dao.UserDAO;
import com.auctionuet.server.persistence.schema.UserSchema;

public class WalletService {
    private final UserDAO userDAO;
    private final UserService userService;

    public WalletService(UserDAO userDAO, UserService userService) {
        this.userDAO = userDAO;
        this.userService = userService;
    }

    public synchronized WalletResponseDTO deposit(String userId, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("So tien nhap vao phai lon hon 0");
        }
        UserSchema userSchema = requireUser(userId);
        userSchema.setBalance(userSchema.getBalance() + amount);
        userDAO.update(userSchema);

        return toWalletResponseDTO(userSchema);
    }

    public synchronized WalletResponseDTO withdraw(String userId, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("So tien nhap vao phai lon hon 0");
        }
        UserSchema userSchema = requireUser(userId);
        if (userSchema.getBalance() < amount) {
            throw new IllegalArgumentException("So du khong du");
        }
        userSchema.setBalance(userSchema.getBalance() - amount);
        userDAO.update(userSchema);

        return toWalletResponseDTO(userSchema);
    }

    public synchronized void freezeDeposit(String userId, double amount) {
        UserSchema userSchema = requireUser(userId);
        if (userSchema.getBalance() < amount) {
            throw new IllegalArgumentException("So du khong du");
        }
        userSchema.setBalance(userSchema.getBalance() - amount);
        userSchema.setFrozenBalance(userSchema.getFrozenBalance() + amount);
        userDAO.update(userSchema);
    }

    public synchronized void unfreezeDeposit(String userId, double amount) {
        UserSchema userSchema = requireUser(userId);
        if (userSchema.getFrozenBalance() < amount) {
            amount = Math.min(amount, userSchema.getFrozenBalance());
        }
        userSchema.setFrozenBalance(userSchema.getFrozenBalance() - amount);
        userSchema.setBalance(userSchema.getBalance() + amount);
        userDAO.update(userSchema);
    }

    public synchronized void forfeitDeposit(String userId, double amount) {
        UserSchema userSchema = requireUser(userId);
        if (userSchema.getFrozenBalance() < amount) {
            amount = Math.min(amount, userSchema.getFrozenBalance());
        }
        userSchema.setFrozenBalance(userSchema.getFrozenBalance() - amount);
        userDAO.update(userSchema);
    }

    public WalletResponseDTO getWallet(String userId) {
        return toWalletResponseDTO(requireUser(userId));
    }

    private UserSchema requireUser(String userId) {
        UserSchema userSchema = userDAO.findById(userId);
        if (userSchema == null) {
            throw new IllegalArgumentException("Khong tim thay User");
        }
        return userSchema;
    }

    private WalletResponseDTO toWalletResponseDTO(UserSchema userSchema) {
        return new WalletResponseDTO(
                userService.toDTO(userSchema),
                userSchema.getBalance(),
                userSchema.getFrozenBalance());
    }
}
