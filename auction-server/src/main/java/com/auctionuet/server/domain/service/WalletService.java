package com.auctionuet.server.domain.service;

import com.auctionuet.protocol.dto.response.wallet.WalletResponseDTO;
import com.auctionuet.protocol.dto.response.transaction.TransactionDTO;
import com.auctionuet.protocol.enums.TransactionType;
import com.auctionuet.server.persistence.dao.TransactionDAO;
import com.auctionuet.server.persistence.dao.UserDAO;
import com.auctionuet.server.persistence.schema.TransactionSchema;
import com.auctionuet.server.persistence.schema.UserSchema;

import java.util.List;

public class WalletService {
    private final UserDAO userDAO;
    private final UserService userService;
    private final TransactionService transactionService;

    public WalletService(UserDAO userDAO, UserService userService) {
        this(userDAO, userService, new TransactionService(new TransactionDAO()));
    }

    public WalletService(UserDAO userDAO, UserService userService, TransactionService transactionService) {
        this.userDAO = userDAO;
        this.userService = userService;
        this.transactionService = transactionService;
    }

    public synchronized WalletResponseDTO deposit(String userId, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("So tien nhap vao phai lon hon 0");
        }
        UserSchema userSchema = requireUser(userId);
        double balanceBefore = userSchema.getBalance();
        double frozenBefore = userSchema.getFrozenBalance();
        userSchema.setBalance(userSchema.getBalance() + amount);
        userDAO.update(userSchema);
        recordTransaction(userSchema, TransactionType.WALLET_DEPOSIT, amount, null, null,
                balanceBefore, userSchema.getBalance(), frozenBefore, userSchema.getFrozenBalance(),
                "Nap tien vao vi");

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
        double balanceBefore = userSchema.getBalance();
        double frozenBefore = userSchema.getFrozenBalance();
        userSchema.setBalance(userSchema.getBalance() - amount);
        userDAO.update(userSchema);
        recordTransaction(userSchema, TransactionType.WALLET_WITHDRAW, amount, null, null,
                balanceBefore, userSchema.getBalance(), frozenBefore, userSchema.getFrozenBalance(),
                "Rut tien khoi vi");

        return toWalletResponseDTO(userSchema);
    }

    public synchronized void freezeDeposit(String userId, double amount) {
        holdAuctionDeposit(userId, amount, null, "Giu tien coc dau gia");
    }

    public synchronized String holdAuctionDeposit(String userId, double amount, String auctionId, String description) {
        UserSchema userSchema = requireUser(userId);
        if (userSchema.getBalance() < amount) {
            throw new IllegalArgumentException("So du khong du");
        }
        double balanceBefore = userSchema.getBalance();
        double frozenBefore = userSchema.getFrozenBalance();
        userSchema.setBalance(userSchema.getBalance() - amount);
        userSchema.setFrozenBalance(userSchema.getFrozenBalance() + amount);
        userDAO.update(userSchema);
        TransactionSchema transaction = recordTransaction(
                userSchema,
                TransactionType.AUCTION_DEPOSIT_HOLD,
                amount,
                auctionId,
                null,
                balanceBefore,
                userSchema.getBalance(),
                frozenBefore,
                userSchema.getFrozenBalance(),
                description != null ? description : "Giu tien coc dau gia");
        return transaction.getId();
    }

    public synchronized String recordLegacyAuctionDeposit(
            String userId,
            double amount,
            String auctionId,
            String description) {
        UserSchema userSchema = requireUser(userId);
        TransactionSchema transaction = recordTransaction(
                userSchema,
                TransactionType.AUCTION_DEPOSIT_HOLD,
                amount,
                auctionId,
                null,
                userSchema.getBalance(),
                userSchema.getBalance(),
                userSchema.getFrozenBalance(),
                userSchema.getFrozenBalance(),
                description != null ? description : "Khoi phuc ban ghi coc tu du lieu cu");
        return transaction.getId();
    }

    public synchronized void unfreezeDeposit(String userId, double amount) {
        refundAuctionDeposit(userId, amount, null, null, "Hoan tien coc dau gia");
    }

    public synchronized void refundAuctionDeposit(
            String userId,
            double amount,
            String auctionId,
            String relatedTransactionId,
            String description) {
        UserSchema userSchema = requireUser(userId);
        if (userSchema.getFrozenBalance() < amount) {
            amount = Math.min(amount, userSchema.getFrozenBalance());
        }
        if (amount <= 0) {
            return;
        }
        double balanceBefore = userSchema.getBalance();
        double frozenBefore = userSchema.getFrozenBalance();
        userSchema.setFrozenBalance(userSchema.getFrozenBalance() - amount);
        userSchema.setBalance(userSchema.getBalance() + amount);
        userDAO.update(userSchema);
        recordTransaction(userSchema, TransactionType.AUCTION_DEPOSIT_REFUND, amount, auctionId, relatedTransactionId,
                balanceBefore, userSchema.getBalance(), frozenBefore, userSchema.getFrozenBalance(),
                description != null ? description : "Hoan tien coc dau gia");
    }

    public synchronized void forfeitDeposit(String userId, double amount) {
        forfeitAuctionDeposit(userId, amount, null, null, "Tich thu tien coc dau gia");
    }

    public synchronized void forfeitAuctionDeposit(
            String userId,
            double amount,
            String auctionId,
            String relatedTransactionId,
            String description) {
        UserSchema userSchema = requireUser(userId);
        if (userSchema.getFrozenBalance() < amount) {
            amount = Math.min(amount, userSchema.getFrozenBalance());
        }
        if (amount <= 0) {
            return;
        }
        double balanceBefore = userSchema.getBalance();
        double frozenBefore = userSchema.getFrozenBalance();
        userSchema.setFrozenBalance(userSchema.getFrozenBalance() - amount);
        userDAO.update(userSchema);
        recordTransaction(userSchema, TransactionType.AUCTION_DEPOSIT_FORFEIT, amount, auctionId, relatedTransactionId,
                balanceBefore, userSchema.getBalance(), frozenBefore, userSchema.getFrozenBalance(),
                description != null ? description : "Tich thu tien coc dau gia");
    }

    public synchronized WalletResponseDTO payAuctionRemaining(
            String userId,
            double amount,
            String auctionId,
            String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("So tien nhap vao phai lon hon 0");
        }
        UserSchema userSchema = requireUser(userId);
        if (userSchema.getBalance() < amount) {
            throw new IllegalArgumentException("So du khong du");
        }
        double balanceBefore = userSchema.getBalance();
        double frozenBefore = userSchema.getFrozenBalance();
        userSchema.setBalance(userSchema.getBalance() - amount);
        userDAO.update(userSchema);
        recordTransaction(userSchema, TransactionType.AUCTION_PAYMENT, amount, auctionId, null,
                balanceBefore, userSchema.getBalance(), frozenBefore, userSchema.getFrozenBalance(),
                description != null ? description : "Thanh toan phien dau gia");
        return toWalletResponseDTO(userSchema);
    }

    public synchronized WalletResponseDTO payoutSeller(
            String sellerId,
            double amount,
            String auctionId,
            String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("So tien nhap vao phai lon hon 0");
        }
        UserSchema userSchema = requireUser(sellerId);
        double balanceBefore = userSchema.getBalance();
        double frozenBefore = userSchema.getFrozenBalance();
        userSchema.setBalance(userSchema.getBalance() + amount);
        userDAO.update(userSchema);mvn
        recordTransaction(userSchema, TransactionType.SELLER_PAYOUT, amount, auctionId, null,
                balanceBefore, userSchema.getBalance(), frozenBefore, userSchema.getFrozenBalance(),
                description != null ? description : "Nhan tien tu phien dau gia");
        return toWalletResponseDTO(userSchema);
    }

    public WalletResponseDTO getWallet(String userId) {
        return toWalletResponseDTO(requireUser(userId));
    }

    public List<TransactionDTO> getTransactions(String userId) {
        requireUser(userId);
        return transactionService.getTransactionsForUser(userId);
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

    private TransactionSchema recordTransaction(
            UserSchema userSchema,
            TransactionType type,
            double amount,
            String auctionId,
            String relatedTransactionId,
            Double balanceBefore,
            Double balanceAfter,
            Double frozenBalanceBefore,
            Double frozenBalanceAfter,
            String description) {
        return transactionService.record(
                userSchema,
                type,
                amount,
                auctionId,
                relatedTransactionId,
                balanceBefore,
                balanceAfter,
                frozenBalanceBefore,
                frozenBalanceAfter,
                description);
    }
}
