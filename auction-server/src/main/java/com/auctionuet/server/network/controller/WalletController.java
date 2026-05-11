package com.auctionuet.server.network.controller;

import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.domain.manager.SessionManager;
import com.auctionuet.server.domain.service.WalletService;
import com.auctionuet.protocol.Request;
import com.auctionuet.protocol.Response;
import com.auctionuet.protocol.dto.request.WalletRequests;
import com.auctionuet.protocol.dto.response.WalletResponseDTO;

public class WalletController {
    private final WalletService walletService;
    private final SessionManager sessionManager;

    public WalletController(WalletService walletService) {
        this.walletService=walletService;
        this.sessionManager=SessionManager.getInstance();
    }

    public Response handleDeposit(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        WalletRequests.AmountReq req = request.getDataAs(WalletRequests.AmountReq.class);

        WalletResponseDTO wallet = walletService.deposit(user.getId(), req.getAmount());
        return Response.ok(wallet);
    }

    public Response handleWithdraw(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        WalletRequests.AmountReq req = request.getDataAs(WalletRequests.AmountReq.class);

        WalletResponseDTO wallet = walletService.withdraw(user.getId(), req.getAmount());
        return Response.ok(wallet);
    }

    public Response handleGetWallet(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        WalletResponseDTO wallet = walletService.getWallet(user.getId());
        return Response.ok(wallet);
    }
}

