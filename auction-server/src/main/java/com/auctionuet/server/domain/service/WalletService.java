package com.auctionuet.server.domain.service;

import com.auctionuet.protocol.ActionType;
import com.auctionuet.protocol.contract.Dto;
import com.auctionuet.server.persistence.dao.UserDAO;
import com.auctionuet.server.persistence.schema.UserSchema;

public class WalletService {
    private final UserDAO userDAO;

    public WalletService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public synchronized Dto<?> deposit(String userId, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền nhập vào phải lớn hơn 0");
        }
        UserSchema userSchema = userDAO.findById(userId);
        if (userSchema == null) {
            throw new IllegalArgumentException("Không tìm thấy User");
        }
        userSchema.setBalance(userSchema.getBalance() + amount);
        userDAO.update(userSchema);
        
        return ActionType.DEPOSIT.createResponseDto()
                .set("balance", userSchema.getBalance())
                .set("frozenBalance", userSchema.getFrozenBalance());
    }

    public synchronized Dto<?> withdraw(String userId, double amount) {
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
        
        return ActionType.WITHDRAW.createResponseDto()
                .set("balance", userSchema.getBalance())
                .set("frozenBalance", userSchema.getFrozenBalance());
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

    public Dto<?> getWallet(String userId) {
        UserSchema userSchema = userDAO.findById(userId);
        if (userSchema == null) {
            throw new IllegalArgumentException("Không tìm thấy User");
        }
        
        return ActionType.GET_WALLET.createResponseDto()
                .set("balance", userSchema.getBalance())
                .set("frozenBalance", userSchema.getFrozenBalance());
    }
}
