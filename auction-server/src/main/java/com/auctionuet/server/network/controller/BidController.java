package com.auctionuet.server.network.controller;

import com.auctionuet.protocol.Request;
import com.auctionuet.protocol.Response;
import com.auctionuet.protocol.dto.request.bid.AuctionIdRequestDTO;
import com.auctionuet.protocol.dto.request.bid.PlaceBidRequestDTO;
import com.auctionuet.protocol.dto.request.bid.SetAutoBidRequestDTO;
import com.auctionuet.protocol.dto.response.bid.AutoBidConfigDTO;
import com.auctionuet.protocol.dto.response.bid.BidDTO;
import com.auctionuet.protocol.enums.Permission;
import com.auctionuet.server.domain.manager.AuctionManager;
import com.auctionuet.server.domain.manager.SessionManager;
import com.auctionuet.server.domain.model.LiveAuction;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.domain.service.BidService;
import com.auctionuet.server.network.server.ClientHandler;

import java.util.List;

public class BidController {
    private final BidService bidService;
    private final SessionManager sessionManager;

    public BidController(BidService bidService) {
        this.bidService = bidService;
        this.sessionManager = SessionManager.getInstance();
    }

    public Response handlePlaceBid(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        if (!user.hasPermission(Permission.PLACE_BID)) {
            return Response.error("Ban khong co quyen dat gia");
        }
        PlaceBidRequestDTO req = request.getDataAs(PlaceBidRequestDTO.class);

        BidDTO dto = bidService.placeBid(user, req.getAuctionId(), req.getAmount());
        return Response.ok(dto);
    }

    public Response handleGetBidHistory(Request request) throws Exception {
        sessionManager.validateToken(request.getToken());
        AuctionIdRequestDTO req = request.getDataAs(AuctionIdRequestDTO.class);

        List<BidDTO> dtos = bidService.getBidHistoryDTO(req.getAuctionId());
        return Response.ok(dtos);
    }

    public Response handleSetAutoBid(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        if (!user.hasPermission(Permission.SET_AUTO_BID)) {
            return Response.error("Ban khong co quyen cai dat Auto-Bid");
        }
        SetAutoBidRequestDTO req = request.getDataAs(SetAutoBidRequestDTO.class);

        bidService.setAutoBid(user, req.getAuctionId(), req.getMaxBid(), req.getIncrement());
        return Response.ok("Da cai dat Auto-Bid thanh cong");
    }

    public Response handleCancelAutoBid(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        if (!user.hasPermission(Permission.CANCEL_AUTO_BID)) {
            return Response.error("Ban khong co quyen huy Auto-Bid");
        }
        AuctionIdRequestDTO req = request.getDataAs(AuctionIdRequestDTO.class);

        bidService.cancelAutoBid(user, req.getAuctionId());
        return Response.ok("Da huy Auto-Bid");
    }

    public Response handleCheckAutoBid(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        AuctionIdRequestDTO req = request.getDataAs(AuctionIdRequestDTO.class);

        AutoBidConfigDTO config = bidService.getAutoBidConfigDTO(user, req.getAuctionId());
        return Response.ok((Object) config);
    }

    public Response handleSubscribe(Request request, ClientHandler clientHandler) throws Exception {
        sessionManager.validateToken(request.getToken());
        AuctionIdRequestDTO req = request.getDataAs(AuctionIdRequestDTO.class);

        LiveAuction auction = AuctionManager.getInstance().getAuction(req.getAuctionId());
        if (auction == null) {
            return Response.error("Phien dau gia khong ton tai hoac chua RUNNING");
        }

        auction.addObserver(clientHandler);
        return Response.ok("Da subscribe phien " + req.getAuctionId());
    }

    public Response handleUnsubscribe(Request request, ClientHandler clientHandler) throws Exception {
        sessionManager.validateToken(request.getToken());
        AuctionIdRequestDTO req = request.getDataAs(AuctionIdRequestDTO.class);

        LiveAuction auction = AuctionManager.getInstance().getAuction(req.getAuctionId());
        if (auction != null) {
            auction.removeObserver(clientHandler);
        }
        return Response.ok("Da unsubscribe phien " + req.getAuctionId());
    }
}
