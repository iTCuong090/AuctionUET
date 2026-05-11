package com.auctionuet.server.domain.service;

import com.auctionuet.server.persistence.dao.UserDAO;
import com.auctionuet.server.persistence.schema.UserSchema;
import com.auctionuet.protocol.dto.response.WalletResponseDTO;

public class WalletService {
    private final UserDAO userDAO;

    public WalletService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public synchronized WalletResponseDTO deposit(String userId, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền nhập vào phải lớn hơn 0");
        }
        UserSchema userSchema = userDAO.findById(userId);
        if (userSchema == null) {
            throw new IllegalArgumentException("Không tìm thấy User");
        }
        userSchema.setBalance(userSchema.getBalance() + amount);
        userDAO.update(userSchema);
        
        return new WalletResponseDTO(userSchema.getBalance(), userSchema.getFrozenBalance());
    }

    public synchronized WalletResponseDTO withdraw(String userId, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền nhập vào phải lớn hơn 0");
        }
        UserSchema userSchema = userDAO.findById(userId);
        if (userSchema == null) {
            throw new IllegalArgumentException("Không tìm thấy User");
        }
        if (userSchema.getBalance() < amount) {
            throw new IllegalArgumentException("Số dư không đủ");
        }
        userSchema.setBalance(userSchema.getBalance() - amount);
        userDAO.update(userSchema);
        
        return new WalletResponseDTO(userSchema.getBalance(), userSchema.getFrozenBalance());
    }

    public synchronized void freezeDeposit(String userId, double amount) {
        UserSchema userSchema = userDAO.findById(userId);
        if (userSchema == null) {
            throw new IllegalArgumentException("Không tìm thấy User");
        }
        if (userSchema.getBalance() < amount) {
            throw new IllegalArgumentException("Số dư không đủ");
        }
        userSchema.setBalance(userSchema.getBalance() - amount);
        userSchema.setFrozenBalance(userSchema.getFrozenBalance() + amount);
        userDAO.update(userSchema);
    }

    public synchronized void unfreezeDeposit(String userId, double amount) {
        UserSchema userSchema = userDAO.findById(userId);
        if (userSchema == null) {
            throw new IllegalArgumentException("Không tìm thấy User");
        }
        if (userSchema.getFrozenBalance() < amount) {
            amount = Math.min(amount, userSchema.getFrozenBalance());
        }
        userSchema.setFrozenBalance(userSchema.getFrozenBalance() - amount);
        userSchema.setBalance(userSchema.getBalance() + amount);
        userDAO.update(userSchema);
    }

    public synchronized void forfeitDeposit(String userId, double amount) {
        UserSchema userSchema = userDAO.findById(userId);
        if (userSchema == null) {
            throw new IllegalArgumentException("Không tìm thấy User");
        }
        if (userSchema.getFrozenBalance() < amount) {
            amount = Math.min(amount, userSchema.getFrozenBalance());
        }
        userSchema.setFrozenBalance(userSchema.getFrozenBalance() - amount);
        userDAO.update(userSchema);
    }

    public WalletResponseDTO getWallet(String userId) {
        UserSchema userSchema = userDAO.findById(userId);
        if (userSchema == null) {
            throw new IllegalArgumentException("Không tìm thấy User");
        }
        
        return new WalletResponseDTO(userSchema.getBalance(), userSchema.getFrozenBalance());
    }
}
