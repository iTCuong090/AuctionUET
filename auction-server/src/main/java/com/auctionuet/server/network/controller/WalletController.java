package com.auctionuet.server.network.controller;

import com.auctionuet.protocol.Response;
import com.auctionuet.protocol.contract.Dto;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.domain.service.WalletService;

public class WalletController {
    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    public Response handleDeposit(Dto<?> requestData, User user) throws Exception {
        Double amount = requestData.getDouble("amount");
        Dto<?> wallet = walletService.deposit(user.getId(), amount);
        return Response.ok(wallet);
    }

    public Response handleWithdraw(Dto<?> requestData, User user) throws Exception {
        Double amount = requestData.getDouble("amount");
        Dto<?> wallet = walletService.withdraw(user.getId(), amount);
        return Response.ok(wallet);
    }

    public Response handleGetWallet(User user) throws Exception {
        Dto<?> wallet = walletService.getWallet(user.getId());
        return Response.ok(wallet);
    }
}
