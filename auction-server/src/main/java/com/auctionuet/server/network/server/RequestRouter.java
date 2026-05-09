package com.auctionuet.server.network.server;

import com.auctionuet.server.domain.service.SessionManager;
import com.auctionuet.server.network.controller.*;
import com.auctionuet.server.network.protocol.Request;
import com.auctionuet.server.network.protocol.Response;
import com.auctionuet.server.util.AppLogger;

public class RequestRouter {
    private final BidController bidController;
    private final WalletController walletController;
    private final AuthController authController;
    private final ItemController itemController;
    private final AuctionController auctionController;

    public RequestRouter(BidController bidController, WalletController walletController, AuthController authController, ItemController itemController,
                         AuctionController auctionController) {
        this.bidController = bidController;
        this.walletController = walletController;
        this.authController = authController;
        this.itemController = itemController;
        this.auctionController = auctionController;
    }

    public Response route(Request request) {
        if (request == null || request.getAction() == null) {
            AppLogger.logUnknownAction("null");
            return Response.error("Invalid request: missing action");
        }

        try {
            return internalRoute(request);
        } catch (Exception e) {
            // Exception được log bởi GlobalExceptionHandler
            return GlobalExceptionHandler.handle(e);
        }
    }

    private Response internalRoute(Request request) throws Exception {
        String action = request.getAction().name();

        switch (request.getAction()) {

            // ── Auth ──
            case LOGIN:
                AppLogger.logRouting(action, "AuthController", "handleLogin");
                return authController.handleLogin(request);

            case REGISTER:
                AppLogger.logRouting(action, "AuthController", "handleRegister");
                return authController.handleRegister(request);

            case LOGOUT:
                AppLogger.logRouting(action, "AuthController", "handleLogout");
                return authController.handleLogout(request);

            // ── Item Management ──
            case CREATE_ITEM:
                AppLogger.logRouting(action, "ItemController", "handleCreateItem");
                return itemController.handleCreateItem(request);

            case GET_MY_ITEMS:
                AppLogger.logRouting(action, "ItemController", "handleGetMyItems");
                return itemController.handleGetMyItems(request);

            // ── Auction Management ──
            case CREATE_AUCTION:
                AppLogger.logRouting(action, "AuctionController", "handleCreateAuction");
                return auctionController.handleCreateAuction(request);

            case START_AUCTION:
                AppLogger.logRouting(action, "AuctionController", "handleStartAuction");
                return auctionController.handleStartAuction(request);

            case GET_AUCTIONS:
                AppLogger.logRouting(action, "AuctionController", "handleGetAuctions");
                return auctionController.handleGetAuctions(request);

            case GET_AUCTION_DETAIL:
                AppLogger.logRouting(action, "AuctionController", "handleGetAuctionDetail");
                return auctionController.handleGetAuctionDetail(request);
            // ── Bidding ──
            case PLACE_BID:
                return bidController.handlePlaceBid(request);
            case GET_BID_HISTORY:
                return bidController.handleGetBidHistory(request);
            case SET_AUTO_BID:
                return bidController.handleSetAutoBid(request);
            case CANCEL_AUTO_BID:
                return bidController.handleCancelAutoBid(request);
            // Subcribe──

            case SUBSCRIBE:
                return bidController.handleSubscribe(request, clientHandler);
            case UNSUBSCRIBE:
                return bidController.handleUnsubscribe(request, clientHandler);

            // ── Wallet ──
            case DEPOSIT:
                return walletController.handleDeposit(request);
            case WITHDRAW:
                return walletController.handleWithdraw(request);
            case GET_WALLET:
                return walletController.handleGetWallet(request);

            // ── Payment ──
            case PAY_AUCTION:
                return auctionController.handlePayAuction(request);
            // ── Utils ──
            case PING:
                AppLogger.logRouting(action, "RequestRouter", "PING");
                if (request.getToken() != null && !request.getToken().isEmpty()) {
                    SessionManager.getInstance().validateToken(request.getToken());
                }
                return Response.ok("PONG");

            default:
                AppLogger.logUnknownAction(action);
                return Response.error("Unknown action: " + action);
        }
    }
}
