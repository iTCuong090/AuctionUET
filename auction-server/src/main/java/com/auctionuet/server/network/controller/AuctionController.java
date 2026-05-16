package com.auctionuet.server.network.controller;

import com.auctionuet.protocol.Request;
import com.auctionuet.protocol.Response;
import com.auctionuet.protocol.dto.request.auction.AuctionIdRequestDTO;
import com.auctionuet.protocol.dto.request.auction.CreateAuctionRequestDTO;
import com.auctionuet.protocol.dto.response.auction.AuctionDTO;
import com.auctionuet.server.domain.manager.SessionManager;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.domain.service.AuctionService;
import com.auctionuet.server.domain.service.ItemService;

import java.util.List;

public class AuctionController {

    private final AuctionService auctionService;
    private final SessionManager sessionManager;

    public AuctionController(AuctionService auctionService, ItemService itemService) {
        this.auctionService = auctionService;
        this.sessionManager = SessionManager.getInstance();
    }

    public Response handleCreateAuction(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        CreateAuctionRequestDTO req = request.getDataAs(CreateAuctionRequestDTO.class);
        int antiSnipingWindowSeconds =
                req.getAntiSnipingWindowSeconds() != null ? req.getAntiSnipingWindowSeconds() : 60;
        int antiSnipingExtensionSeconds =
                req.getAntiSnipingExtensionSeconds() != null ? req.getAntiSnipingExtensionSeconds() : 120;

        AuctionDTO auctionDTO = auctionService.createAuction(
                user,
                req.getItemId(),
                req.getStartTime(),
                req.getEndTime(),
                req.getTitle(),
                req.getDescription(),
                antiSnipingWindowSeconds,
                antiSnipingExtensionSeconds);

        return Response.ok(auctionDTO);
    }

    public Response handleStartAuction(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        AuctionIdRequestDTO req = request.getDataAs(AuctionIdRequestDTO.class);

        auctionService.startAuction(user, req.getAuctionId());

        return Response.ok("Da bat dau phien dau gia");
    }

    public Response handleGetAuctions(Request request) throws Exception {
        sessionManager.validateToken(request.getToken());
        List<AuctionDTO> auctions = auctionService.getAuctions();
        return Response.ok(auctions);
    }

    public Response handleGetAuctionDetail(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        AuctionIdRequestDTO req = request.getDataAs(AuctionIdRequestDTO.class);

        if (req.getAuctionId() == null || req.getAuctionId().isEmpty()) {
            return Response.error("Thieu auctionId");
        }

        AuctionDTO auctionDTO = auctionService.getAuctionById(req.getAuctionId(), user.getId());
        if (auctionDTO == null) {
            return Response.error("Khong tim thay phien dau gia");
        }

        return Response.ok(auctionDTO);
    }

    public Response handlePayAuction(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        AuctionIdRequestDTO req = request.getDataAs(AuctionIdRequestDTO.class);

        auctionService.payAuction(user, req.getAuctionId());
        return Response.ok("Thanh toan thanh cong! San pham da thuoc ve ban.");
    }
}
