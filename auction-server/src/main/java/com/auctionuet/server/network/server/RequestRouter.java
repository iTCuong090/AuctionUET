package com.auctionuet.server.network.server;

import com.auctionuet.protocol.ActionType;
import com.auctionuet.protocol.Request;
import com.auctionuet.protocol.Response;
import com.auctionuet.protocol.contract.Dto;
import com.auctionuet.server.domain.manager.SessionManager;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.network.controller.AuctionController;
import com.auctionuet.server.network.controller.AuthController;
import com.auctionuet.server.network.controller.BidController;
import com.auctionuet.server.network.controller.ItemController;
import com.auctionuet.server.network.controller.WalletController;

import java.util.HashMap;
import java.util.Map;

public class RequestRouter {
    @FunctionalInterface
    public interface ActionHandler {
        Response<?> handle(Dto<?> requestData, User user, ClientHandler ctx) throws Exception;
    }

    private final Map<ActionType, ActionHandler> handlers = new HashMap<>();

    public RequestRouter(
            BidController bidController,
            WalletController walletController,
            AuthController authController,
            ItemController itemController,
            AuctionController auctionController
    ) {
        register(ActionType.PING, (data, user, ctx) -> Response.ok("PONG"));

        register(ActionType.LOGIN, (data, user, ctx) -> authController.handleLogin(data));
        register(ActionType.REGISTER, (data, user, ctx) -> authController.handleRegister(data));
        register(ActionType.LOGOUT, (data, user, ctx) -> authController.handleLogout(user));

        register(ActionType.CREATE_ITEM, (data, user, ctx) -> itemController.handleCreateItem(data, user));
        register(ActionType.GET_MY_ITEMS, (data, user, ctx) -> itemController.handleGetMyItems(user));

        register(ActionType.CREATE_AUCTION, (data, user, ctx) -> auctionController.handleCreateAuction(data, user));
        register(ActionType.START_AUCTION, (data, user, ctx) -> auctionController.handleStartAuction(data, user));
        register(ActionType.GET_AUCTIONS, (data, user, ctx) -> auctionController.handleGetAuctions());
        register(ActionType.GET_AUCTION_DETAIL, (data, user, ctx) -> auctionController.handleGetAuctionDetail(data));

        register(ActionType.PLACE_BID, (data, user, ctx) -> bidController.handlePlaceBid(data, user));
        register(ActionType.GET_BID_HISTORY, (data, user, ctx) -> bidController.handleGetBidHistory(data));
        register(ActionType.SET_AUTO_BID, (data, user, ctx) -> bidController.handleSetAutoBid(data, user));
        register(ActionType.CANCEL_AUTO_BID, (data, user, ctx) -> bidController.handleCancelAutoBid(data, user));
        register(ActionType.CHECK_AUTO_BID, (data, user, ctx) -> bidController.handleCheckAutoBid(data, user));
        register(ActionType.SUBSCRIBE, (data, user, ctx) -> bidController.handleSubscribe(data, user, ctx));
        register(ActionType.UNSUBSCRIBE, (data, user, ctx) -> bidController.handleUnsubscribe(data, user, ctx));

        register(ActionType.DEPOSIT, (data, user, ctx) -> walletController.handleDeposit(data, user));
        register(ActionType.WITHDRAW, (data, user, ctx) -> walletController.handleWithdraw(data, user));
        register(ActionType.GET_WALLET, (data, user, ctx) -> walletController.handleGetWallet(user));
    }

    private void register(ActionType type, ActionHandler handler) {
        handlers.put(type, handler);
    }

    public Response<?> route(Request<?> request, ClientHandler clientHandler) {
        ActionType action = request.getAction();
        if (action == null) {
            return Response.error("Invalid request: missing action");
        }

        try {
            ActionHandler handler = handlers.get(action);
            if (handler == null) {
                return Response.error("Unknown or unhandled action: " + action.name());
            }

            Dto<?> requestData = request.getDataObject();
            action.validateRequest(requestData);

            User user = null;
            if (action.isRequiresAuth()) {
                if (request.getToken() == null || request.getToken().isEmpty()) {
                    return Response.error("Yeu cau dang nhap");
                }
                user = SessionManager.getInstance().validateToken(request.getToken());
            }

            ClientHandler ctx = action.isNeedsClientHandler() ? clientHandler : null;
            return handler.handle(requestData, user, ctx);
        } catch (Exception e) {
            return GlobalExceptionHandler.handle(e);
        }
    }
}
