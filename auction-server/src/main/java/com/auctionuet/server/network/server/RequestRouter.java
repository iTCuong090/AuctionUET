package com.auctionuet.server.network.server;

import com.auctionuet.server.domain.manager.SessionManager;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.domain.service.UserService;
import com.auctionuet.server.network.controller.*;
import com.auctionuet.protocol.Request;
import com.auctionuet.protocol.Response;

public class RequestRouter {
    private final BidController bidController;
    private final WalletController walletController;
    private final AuthController authController;
    private final ItemController itemController;
    private final AuctionController auctionController;
    private final UserController userController;
    private final AdminController adminController;
    private final UserService userService;

    public RequestRouter(BidController bidController, WalletController walletController, AuthController authController, ItemController itemController,
                         AuctionController auctionController, UserController userController,
                         AdminController adminController, UserService userService) {
        this.bidController = bidController;
        this.walletController = walletController;
        this.authController = authController;
        this.itemController = itemController;
        this.auctionController = auctionController;
        this.userController = userController;
        this.adminController = adminController;
        this.userService = userService;
    }

    public Response route(Request request,ClientHandler clientHandler) {
        if (request == null || request.getAction() == null) {
            return Response.error("Invalid request: missing action");
        }

        try {
            return internalRoute(request,clientHandler);
        } catch (Exception e) {
            // Exception được log bởi GlobalExceptionHandler
            return GlobalExceptionHandler.handle(e);
        }
    }

    private Response internalRoute(Request request,ClientHandler clientHandler) throws Exception {
        String action = request.getAction().name();
        requirePasswordChangedForProtectedAction(request);

        switch (request.getAction()) {

            // ── Auth ──
            case LOGIN:
                return authController.handleLogin(request);

            case REGISTER:
                return authController.handleRegister(request);

            case LOGOUT:
                return authController.handleLogout(request);

            case GET_PROFILE:
                return userController.handleGetProfile(request);

            case UPDATE_PROFILE:
                return userController.handleUpdateProfile(request);

            // ── Item Management ──
            case CREATE_ITEM:
                return itemController.handleCreateItem(request);

            case GET_MY_ITEMS:
                return itemController.handleGetMyItems(request);
            case DELETE_ITEM:
                return itemController.handleDeleteItem(request);
            case GET_ITEM_AUCTION_HISTORY:
                return auctionController.handleGetItemAuctionHistory(request);

            // ── Auction Management ──
            case CREATE_AUCTION:
                return auctionController.handleCreateAuction(request);

            case START_AUCTION:
                return auctionController.handleStartAuction(request);

            case GET_AUCTIONS:
                return auctionController.handleGetAuctions(request);

            case GET_AUCTION_DETAIL:
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
            case CHECK_AUTO_BID:
                return bidController.handleCheckAutoBid(request);
            // ── Subscribe ──

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
            case GET_MY_TRANSACTIONS:
                return walletController.handleGetMyTransactions(request);

            // ── Payment ──
            case PAY_AUCTION:
                return auctionController.handlePayAuction(request);
            case GET_MY_PENDING_PAYMENTS:
                return auctionController.handleGetMyPendingPayments(request);
            // ── Admin ──
            case GET_ALL_USERS:
                return adminController.handleGetAllUsers(request);
            case UPDATE_USER_STATUS:
                return adminController.handleUpdateUserStatus(request);
            case ADMIN_CANCEL_AUCTION:
                return adminController.handleCancelAuction(request);
            case GET_ITEM_APPROVAL_SETTINGS:
                return adminController.handleGetItemApprovalSettings(request);
            case UPDATE_ITEM_APPROVAL_SETTINGS:
                return adminController.handleUpdateItemApprovalSettings(request);
            case GET_ITEMS_FOR_APPROVAL:
                return adminController.handleGetItemsForApproval(request);
            case UPDATE_ITEM_APPROVAL_STATUS:
                return adminController.handleUpdateItemApprovalStatus(request);
            // ── Utils ──
            case PING:
                if (request.getToken() != null && !request.getToken().isEmpty()) {
                    SessionManager.getInstance().validateToken(request.getToken());
                }
                return Response.ok("PONG");

            default:
                return Response.error("Unknown action: " + action);
        }
    }

    private void requirePasswordChangedForProtectedAction(Request request) {
        switch (request.getAction()) {
            case LOGIN, REGISTER, LOGOUT, PING, GET_PROFILE, UPDATE_PROFILE:
                return;
            default:
                break;
        }

        if (request.getToken() == null || request.getToken().isBlank()) {
            return;
        }
        User user = SessionManager.getInstance().validateToken(request.getToken());
        if (userService.mustChangePassword(user.getId())) {
            throw new IllegalArgumentException("Bạn phải đổi mật khẩu trước khi tiếp tục");
        }
    }
}

