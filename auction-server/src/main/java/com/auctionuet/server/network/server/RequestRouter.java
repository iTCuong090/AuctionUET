package com.auctionuet.server.network.server;

import com.auctionuet.server.domain.service.AuctionService;
import com.auctionuet.server.domain.service.SessionManager;
import com.auctionuet.server.exception.AuthenticationException;
import com.auctionuet.server.network.controller.AuctionController;
import com.auctionuet.server.network.controller.AuthController;
import com.auctionuet.server.network.protocol.Request;
import com.auctionuet.server.network.protocol.Response;

public class RequestRouter {
    private AuthController authController;
    private AuctionController auctionController;

    public RequestRouter(AuthController authController, AuctionController auctionController) {
        this.authController = authController;
        this.auctionController = auctionController;
    }

    public Response route(Request request) {
        if (request == null || request.getAction() == null) {
            return Response.error("Invalid request: missing action");
        }

        switch (request.getAction()) {
            case LOGIN:
                return authController.handleLogin(request);
            case REGISTER:
                return authController.handleRegister(request);
            case LOGOUT:
                return authController.handleLogout(request);

            // Item (tuần 4 — MỚI)
            case CREATE_ITEM:
                return auctionController.handleCreateItem(request);
//            case GET_MY_ITEMS:
//                return auctionController.handleGetMyItems(request);

            // Auction (tuần 4 — MỚI)
            case CREATE_AUCTION:
                return auctionController.handleCreateAuction(request);
            case START_AUCTION:
                return auctionController.handleStartAuction(request);
            case GET_AUCTIONS:
                return auctionController.handleGetAuctions(request);
            case GET_AUCTION_DETAIL:
                return auctionController.handleGetAuctionDetail(request);

            // Utils
            case PING:
                // If token is provided, validate it (used by client to check session)
                if (request.getToken() != null && !request.getToken().isEmpty()) {
                    try {
                        SessionManager.getInstance().validateToken(request.getToken());
                    } catch (AuthenticationException e) {
                        return Response.error("Token không hợp lệ hoặc đã hết hạn");
                    }
                }
                return Response.ok("PONG");

            default:
                return Response.error("Unknown action: " + request.getAction());
        }
    }
}
