package com.auctionuet.server.network.controller;

import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.domain.service.AuctionService;
import com.auctionuet.server.domain.service.ItemService;
import com.auctionuet.server.domain.manager.SessionManager;
import com.auctionuet.server.mapper.AuctionMapper;
import com.auctionuet.protocol.dto.request.auction.AuctionIdRequestDTO;
import com.auctionuet.protocol.dto.request.auction.CreateAuctionRequestDTO;
import com.auctionuet.protocol.dto.response.auction.AuctionDTO;
import com.auctionuet.protocol.dto.response.item.ItemDTO;
import com.auctionuet.protocol.Request;
import com.auctionuet.protocol.Response;
import com.auctionuet.server.persistence.schema.AuctionSchema;
import com.auctionuet.server.persistence.schema.ItemSchema;

import java.util.ArrayList;
import java.util.List;

public class AuctionController {

    private final AuctionService auctionService;
    private final ItemService itemService;
    private final SessionManager sessionManager;

    public AuctionController(AuctionService auctionService, ItemService itemService) {
        this.auctionService = auctionService;
        this.itemService = itemService;
        this.sessionManager = SessionManager.getInstance();
    }

    // ———————————————————— CREATE AUCTION ————————————————————

    public Response handleCreateAuction(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        CreateAuctionRequestDTO req = request.getDataAs(CreateAuctionRequestDTO.class);

        int antiSnipingWindowSeconds = req.getAntiSnipingWindowSeconds() != null ? req.getAntiSnipingWindowSeconds() : 60;
        int antiSnipingExtensionSeconds = req.getAntiSnipingExtensionSeconds() != null ? req.getAntiSnipingExtensionSeconds() : 120;

        AuctionSchema auctionSchema = auctionService.createAuction(
                user, req.getItemId(), req.getStartTime(), req.getEndTime(), req.getTitle(), req.getDescription(),
                antiSnipingWindowSeconds, antiSnipingExtensionSeconds);

        ItemSchema itemSchema = itemService.getItemById(auctionSchema.getItemId());
        ItemDTO itemDTO = null;
        if (itemSchema != null) {
            itemDTO = itemService.toItemDTO(itemSchema, user.getUsername());
        }
        AuctionDTO auctionDTO = AuctionMapper.toDTO(auctionSchema, itemDTO, user.getUsername(), null);

        return Response.ok(auctionDTO);
    }

    // ———————————————————— START AUCTION ————————————————————

    public Response handleStartAuction(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        AuctionIdRequestDTO req = request.getDataAs(AuctionIdRequestDTO.class);

        auctionService.startAuction(user, req.getAuctionId());

        return Response.ok("Đã bắt đầu phiên đấu giá");
    }

    // ———————————————————— GET AUCTIONS ————————————————————

    public Response handleGetAuctions(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());

        List<AuctionSchema> auctions = auctionService.getAuctions();
        List<AuctionDTO> auctionDTOList = new ArrayList<>();

        for (AuctionSchema auction : auctions) {
            ItemSchema itemSchema = itemService.getItemById(auction.getItemId());
            if (itemSchema == null) {
                continue;
            }
            String sellerUsername = itemSchema.getSellerId();
            ItemDTO itemDTO = itemService.toItemDTO(itemSchema, sellerUsername);
            AuctionDTO auctionDTO = AuctionMapper.toDTO(auction, itemDTO, sellerUsername, null);
            auctionDTOList.add(auctionDTO);
        }

        return Response.ok(auctionDTOList);
    }

    // ———————————————————— GET AUCTION DETAIL ————————————————————

    public Response handleGetAuctionDetail(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        AuctionIdRequestDTO req = request.getDataAs(AuctionIdRequestDTO.class);

        if (req.getAuctionId() == null || req.getAuctionId().isEmpty()) {
            return Response.error("Thiếu auctionId");
        }

        AuctionSchema schema = auctionService.getAuctionById(req.getAuctionId());
        if (schema == null) {
            return Response.error("Không tìm thấy phiên đấu giá");
        }

        ItemSchema itemSchema = itemService.getItemById(schema.getItemId());
        if (itemSchema == null) {
            return Response.error("Lỗi: Không tìm thấy vật phẩm của phiên đấu giá này!");
        }

        ItemDTO itemDTO = itemService.toItemDTO(itemSchema, user.getUsername());
        AuctionDTO auctionDTO = AuctionMapper.toDTO(schema, itemDTO, user.getUsername(), null);

        return Response.ok(auctionDTO);
    }
    // ———————————————————— PAY AUCTION  ————————————————————
    public Response handlePayAuction(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        AuctionIdRequestDTO req = request.getDataAs(AuctionIdRequestDTO.class);

        auctionService.payAuction(user, req.getAuctionId());
        return Response.ok("Thành toán thành công! Sản phẩm đã thuộc về bạn.");
    }
}
