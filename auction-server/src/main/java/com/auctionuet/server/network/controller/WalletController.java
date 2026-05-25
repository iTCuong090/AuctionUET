package com.auctionuet.server.network.controller;

import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.domain.manager.SessionManager;
import com.auctionuet.server.domain.service.WalletService;
import com.auctionuet.protocol.Request;
import com.auctionuet.protocol.Response;
import com.auctionuet.protocol.dto.request.wallet.AmountRequestDTO;
import com.auctionuet.protocol.dto.response.transaction.TransactionDTO;
import com.auctionuet.protocol.dto.response.wallet.WalletResponseDTO;
import com.auctionuet.protocol.enums.Permission;

import java.util.List;

public class WalletController {
    private final WalletService walletService;
    private final SessionManager sessionManager;

    public WalletController(WalletService walletService) {
        this.walletService=walletService;
        this.sessionManager=SessionManager.getInstance();
    }

    public Response handleDeposit(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        if (!user.hasPermission(Permission.DEPOSIT)) {
            return Response.error("Ban khong co quyen nap tien");
        }
        AmountRequestDTO req = request.getDataAs(AmountRequestDTO.class);

        WalletResponseDTO wallet = walletService.deposit(user.getId(), req.getAmount());
        return Response.ok(wallet);
    }

    public Response handleWithdraw(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        if (!user.hasPermission(Permission.WITHDRAW)) {
            return Response.error("Ban khong co quyen rut tien");
        }
        AmountRequestDTO req = request.getDataAs(AmountRequestDTO.class);

        WalletResponseDTO wallet = walletService.withdraw(user.getId(), req.getAmount());
        return Response.ok(wallet);
    }

    public Response handleGetWallet(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        if (!user.hasPermission(Permission.GET_WALLET)) {
            return Response.error("Ban khong co quyen xem vi");
        }
        WalletResponseDTO wallet = walletService.getWallet(user.getId());
        return Response.ok(wallet);
    }

    public Response handleGetMyTransactions(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        if (!user.hasPermission(Permission.GET_MY_TRANSACTIONS)) {
            return Response.error("Ban khong co quyen xem lich su giao dich");
        }
        List<TransactionDTO> transactions = walletService.getTransactions(user.getId());
        return Response.ok(transactions);
    }
}

