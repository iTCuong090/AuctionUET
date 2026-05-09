package com.auctionuet.server.domain.service;

import com.auctionuet.server.persistence.dao.UserDAO;
import com.auctionuet.server.persistence.schema.UserSchema;
import java.util.HashMap;
import java.util.Map;

public class WalletService {
    private final UserDAO userDAO;

    public WalletService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public synchronized Map<String, Double> deposit(String userId, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền nhập vào phải lớn hơn 0");
        }
        UserSchema userSchema = userDAO.findById(userId);
        if (userSchema == null) {
            throw new IllegalArgumentException("Không tìm thấy User");
        }
        userSchema.setBalance(userSchema.getBalance() + amount);
        userDAO.update(userSchema);
        
        Map<String, Double> result = new HashMap<>();
        result.put("balance", userSchema.getBalance());
        result.put("frozenBalance", userSchema.getFrozenBalance());
        return result;
    }

    public synchronized Map<String, Double> withdraw(String userId, double amount) {
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
        
        Map<String, Double> result = new HashMap<>();
        result.put("balance", userSchema.getBalance());
        result.put("frozenBalance", userSchema.getFrozenBalance());
        return result;
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

    public Map<String, Double> getWallet(String userId) {
        UserSchema userSchema = userDAO.findById(userId);
        if (userSchema == null) {
            throw new IllegalArgumentException("Không tìm thấy User");
        }
        Map<String, Double> result = new HashMap<>();
        result.put("balance", userSchema.getBalance());
        result.put("frozenBalance", userSchema.getFrozenBalance());
        return result;
    }
}
