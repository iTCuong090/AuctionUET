package com.auctionuet.server.network.controller;

import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.domain.service.AuctionService;
import com.auctionuet.server.domain.service.ItemService;
import com.auctionuet.server.domain.service.SessionManager;
import com.auctionuet.server.mapper.AuctionMapper;
import com.auctionuet.server.mapper.ItemMapper;
import com.auctionuet.server.network.dto.AuctionDTO;
import com.auctionuet.server.network.dto.ItemDTO;
import com.auctionuet.server.network.protocol.Request;
import com.auctionuet.server.network.protocol.Response;
import com.auctionuet.server.persistence.schema.AuctionSchema;
import com.auctionuet.server.persistence.schema.ItemSchema;
import com.auctionuet.server.util.AppLogger;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Controller chuyên xử lý các request liên quan tới Auction Management.
 * Logic về Item đã được tách sang ItemController.
 */
public class AuctionController {

    private final AuctionService auctionService;
    private final ItemService itemService;
    private final SessionManager sessionManager;

    public AuctionController(AuctionService auctionService, ItemService itemService) {
        this.auctionService = auctionService;
        this.itemService = itemService;
        this.sessionManager = SessionManager.getInstance();
    }

    // ──────────────────── CREATE AUCTION ────────────────────

    public Response handleCreateAuction(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        Map<String, Object> data = validateRequestData(request);

        String itemId      = (String) data.get("itemId");
        String title       = (String) data.get("title");
        String description = (String) data.get("description");
        LocalDateTime startTime = parseDateTime(data.get("startTime"));
        LocalDateTime endTime   = parseDateTime(data.get("endTime"));

        int antiSnipingWindowSeconds = data.containsKey("antiSnipingWindowSeconds") ? 
            ((Number) data.get("antiSnipingWindowSeconds")).intValue() : 60;
        int antiSnipingExtensionSeconds = data.containsKey("antiSnipingExtensionSeconds") ? 
            ((Number) data.get("antiSnipingExtensionSeconds")).intValue() : 120;

        AppLogger.logControllerEnter("AuctionController", "handleCreateAuction",
            "user=" + user.getUsername() + " itemId=" + itemId + " title=" + title);

        AuctionSchema auctionSchema = auctionService.createAuction(
                user, itemId, startTime, endTime, title, description,
                antiSnipingWindowSeconds, antiSnipingExtensionSeconds);

        ItemSchema itemSchema = itemService.getItemById(auctionSchema.getItemId());
        ItemDTO itemDTO = null;
        if (itemSchema != null) {
            itemDTO = ItemMapper.toDTO(itemSchema, user.getUsername());
        }
        AuctionDTO auctionDTO = AuctionMapper.toDTO(auctionSchema, itemDTO, user.getUsername(), null);

        AppLogger.logControllerResult("AuctionController", "handleCreateAuction",
            "Created auctionId=" + auctionSchema.getId() + " status=" + auctionSchema.getStatus());
        return Response.ok(auctionDTO);
    }

    // ──────────────────── START AUCTION ────────────────────

    public Response handleStartAuction(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        Map<String, Object> data = validateRequestData(request);

        String auctionId = (String) data.get("auctionId");
        AppLogger.logControllerEnter("AuctionController", "handleStartAuction",
            "user=" + user.getUsername() + " auctionId=" + auctionId);

        auctionService.startAuction(user, auctionId);

        AppLogger.logControllerResult("AuctionController", "handleStartAuction",
            "Auction started | auctionId=" + auctionId);
        return Response.ok("Đã bắt đầu phiên đấu giá");
    }

    // ──────────────────── GET AUCTIONS ────────────────────

    public Response handleGetAuctions(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        AppLogger.logControllerEnter("AuctionController", "handleGetAuctions",
            "user=" + user.getUsername());

        List<AuctionSchema> auctions = auctionService.getAuctions();
        List<AuctionDTO> auctionDTOList = new ArrayList<>();

        for (AuctionSchema auction : auctions) {
            ItemSchema itemSchema = itemService.getItemById(auction.getItemId());
            if (itemSchema == null) {
                // Cảnh báo data integrity — dùng logger WARN thay vì System.err
                AppLogger.logBusinessException(new IllegalStateException(
                    "Bỏ qua đấu giá " + auction.getId() + " do không tìm thấy vật phẩm!"));
                continue;
            }
            String sellerUsername = itemSchema.getSellerId();
            ItemDTO itemDTO = ItemMapper.toDTO(itemSchema, sellerUsername);
            AuctionDTO auctionDTO = AuctionMapper.toDTO(auction, itemDTO, sellerUsername, null);
            auctionDTOList.add(auctionDTO);
        }

        AppLogger.logControllerResult("AuctionController", "handleGetAuctions",
            "Returned " + auctionDTOList.size() + "/" + auctions.size() + " auctions");
        return Response.ok(auctionDTOList);
    }

    // ──────────────────── GET AUCTION DETAIL ────────────────────

    public Response handleGetAuctionDetail(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        Map<String, Object> data = validateRequestData(request);

        String auctionId = (String) data.get("auctionId");
        AppLogger.logControllerEnter("AuctionController", "handleGetAuctionDetail",
            "user=" + user.getUsername() + " auctionId=" + auctionId);

        if (auctionId == null || auctionId.isEmpty()) {
            return Response.error("Thiếu auctionId");
        }

        AuctionSchema schema = auctionService.getAuctionById(auctionId);
        if (schema == null) {
            return Response.error("Không tìm thấy phiên đấu giá");
        }

        ItemSchema itemSchema = itemService.getItemById(schema.getItemId());
        if (itemSchema == null) {
            return Response.error("Lỗi: Không tìm thấy vật phẩm của phiên đấu giá này!");
        }

        ItemDTO itemDTO = ItemMapper.toDTO(itemSchema, user.getUsername());
        AuctionDTO auctionDTO = AuctionMapper.toDTO(schema, itemDTO, user.getUsername(), null);

        AppLogger.logControllerResult("AuctionController", "handleGetAuctionDetail",
            "Found auction=" + schema.getId() + " status=" + schema.getStatus());
        return Response.ok(auctionDTO);
    }

    // ──────────────────── HELPERS ────────────────────

    /**
     * Kiểm tra request có data hay không. Ném IllegalArgumentException nếu data null.
     */
    private Map<String, Object> validateRequestData(Request request) {
        Map<String, Object> data = request.getData();
        if (data == null) {
            throw new IllegalArgumentException("Request thiếu data");
        }
        return data;
    }

    /**
     * Parse chuỗi thời gian từ client (ISO-8601) thành LocalDateTime.
     * Ném IllegalArgumentException nếu giá trị null hoặc sai format.
     */
    private LocalDateTime parseDateTime(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Thiếu thời gian (startTime/endTime)");
        }
        return LocalDateTime.parse(value.toString());
    }
}
