package com.auctionuet.server.network.controller;

import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.domain.manager.SessionManager;
import com.auctionuet.server.domain.service.WalletService;
import com.auctionuet.server.network.protocol.Request;
import com.auctionuet.server.network.protocol.Response;

import java.util.Map;

public class WalletController {
    private final WalletService walletService;
    private final SessionManager sessionManager;

    public WalletController(WalletService walletService) {
        this.walletService=walletService;
        this.sessionManager=SessionManager.getInstance();
    }

    public Response handleDeposit(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        Map<String, Object> data = request.getData();
        double amount = ((Number) data.get("amount")).doubleValue();

        Map<String, Double> wallet = walletService.deposit(user.getId(), amount);
        return Response.ok(wallet);
    }

    public Response handleWithdraw(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        Map<String, Object> data = request.getData();
        double amount = ((Number) data.get("amount")).doubleValue();

        Map<String, Double> wallet = walletService.withdraw(user.getId(), amount);
        return Response.ok(wallet);
    }

    public Response handleGetWallet(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        Map<String, Double> wallet = walletService.getWallet(user.getId());
        return Response.ok(wallet);
    }
}
