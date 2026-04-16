package com.auctionuet.server.network.controller;

import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.domain.service.AuctionService;
import com.auctionuet.server.domain.service.SessionManager;
import com.auctionuet.server.exception.AuthenticationException;
import com.auctionuet.server.network.protocol.Request;
import com.auctionuet.server.network.protocol.Response;

public class AuctionController {
    private AuctionService auctionService;
    private SessionManager sessionManager;
    public AuctionController(AuctionService auctionService){
        this.auctionService=auctionService;
    }
    public Response handleCreateItem(Request request) {
        try {
            User user = sessionManager.validateToken(request.getToken());
            if (!user.hasPermission("CREATE_ITEM")) {
                return Response.error("Chỉ Seller mới được đăng sản phẩm");
            }
            return Response.error("Chưa implement");
        } catch (AuthenticationException e) {
            return Response.error("Token không hợp lệ");
        }
    }
    public Response handleCreateAuction(Request request) {
        try {
            User user = sessionManager.validateToken(request.getToken());
            if (!user.hasPermission("CREATE_AUCTION")) {
                return Response.error("Chỉ Seller mới được tạo cuộc đấu giá");
            }
            return Response.error("Chưa implement");
        } catch (AuthenticationException e) {
            return Response.error("Token không hợp lệ");
        }
    }
    public Response handleStartAuction(Request request) {
        try {
            User user = sessionManager.validateToken(request.getToken());
            if (!user.hasPermission("START_AUCTION")) {
                return Response.error("Chỉ Seller mới được bắt đầu cuộc đấu giá");
            }
            return Response.error("Chưa implement");
        } catch (AuthenticationException e) {
            return Response.error("Token không hợp lệ");
        }
    }
    public Response handleGetAuctions(Request request) {
        try {
            User user = sessionManager.validateToken(request.getToken());
            if (!user.hasPermission("GET_AUCTIONS")) {
                return Response.error("Không có quyền lấy danh sách đấu giá");
            }
            return Response.error("Chưa implement");
        } catch (AuthenticationException e) {
            return Response.error("Token không hợp lệ");
        }
    }
    public Response handleGetAuctionDetail(Request request) {
        try {
            User user = sessionManager.validateToken(request.getToken());
            if (!user.hasPermission("GET_AUCTION_DETAIL")) {
                return Response.error("Không có quyền xem thông tin đấu giá");
            }
            return Response.error("Chưa implement");
        } catch (AuthenticationException e) {
            return Response.error("Token không hợp lệ");
        }
    }

}

