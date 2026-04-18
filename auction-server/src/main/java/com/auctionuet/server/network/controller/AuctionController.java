package com.auctionuet.server.network.controller;

import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.domain.service.AuctionService;
import com.auctionuet.server.domain.service.SessionManager;
import com.auctionuet.server.exception.AuctionException;
import com.auctionuet.server.exception.AuthenticationException;
import com.auctionuet.server.mapper.ItemMapper;
import com.auctionuet.server.network.dto.ItemDTO;
import com.auctionuet.server.network.protocol.Request;
import com.auctionuet.server.network.protocol.Response;
import com.auctionuet.server.persistence.schema.AuctionSchema;
import com.auctionuet.server.persistence.schema.ItemSchema;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

public class AuctionController {

    private final AuctionService auctionService;
    private final SessionManager sessionManager;

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
        this.sessionManager = SessionManager.getInstance();
    }

    // ──────────────────── ITEM ────────────────────

    public Response handleCreateItem(Request request) {
        try {
            User user = sessionManager.validateToken(request.getToken());
            Map<String, Object> data = validateRequestData(request);
            ItemDTO itemDTO = ItemMapper.fromRequestData(data);
            ItemSchema created = auctionService.createItem(user, itemDTO);
            ItemDTO responseDTO = ItemMapper.toDTO(created, user.getUsername());
            return Response.ok(responseDTO);
        } catch (AuthenticationException e) {
            return Response.error("Token không hợp lệ");
        } catch (AuctionException e) {
            return Response.error(e.getMessage());
        } catch (IllegalArgumentException e) {
            return Response.error("Dữ liệu không hợp lệ: " + e.getMessage());
        } catch (Exception e) {
            return Response.error("Lỗi server: " + e.getMessage());
        }
    }

    // ──────────────────── AUCTION ────────────────────

    public Response handleCreateAuction(Request request) {
        try {
            User user = sessionManager.validateToken(request.getToken());
            Map<String, Object> data = validateRequestData(request);

            String itemId = (String) data.get("itemId");
            String title = (String) data.get("title");
            String description = (String) data.get("description");
            LocalDateTime startTime = parseDateTime(data.get("startTime"));
            LocalDateTime endTime = parseDateTime(data.get("endTime"));

            AuctionSchema auction = auctionService.createAuction(
                    user, itemId, startTime, endTime, title, description
            );
            return Response.ok(auction);
        } catch (AuthenticationException e) {
            return Response.error("Token không hợp lệ");
        } catch (AuctionException e) {
            return Response.error(e.getMessage());
        } catch (IllegalArgumentException | DateTimeParseException e) {
            return Response.error("Dữ liệu không hợp lệ: " + e.getMessage());
        } catch (Exception e) {
            return Response.error("Lỗi server: " + e.getMessage());
        }
    }

    public Response handleStartAuction(Request request) {
        try {
            User user = sessionManager.validateToken(request.getToken());
            Map<String, Object> data = validateRequestData(request);

            String auctionId = (String) data.get("auctionId");
            auctionService.startAuction(user, auctionId);
            return Response.ok("Đã bắt đầu phiên đấu giá");
        } catch (AuthenticationException e) {
            return Response.error("Token không hợp lệ");
        } catch (AuctionException e) {
            return Response.error(e.getMessage());
        } catch (Exception e) {
            return Response.error("Lỗi server: " + e.getMessage());
        }
    }

    public Response handleGetAuctions(Request request) {
        try {
            sessionManager.validateToken(request.getToken());
            List<AuctionSchema> auctions = auctionService.getAuctions();
            return Response.ok(auctions);
        } catch (AuthenticationException e) {
            return Response.error("Token không hợp lệ");
        } catch (Exception e) {
            return Response.error("Lỗi server: " + e.getMessage());
        }
    }

    public Response handleGetAuctionDetail(Request request) {
        try {
            sessionManager.validateToken(request.getToken());
            Map<String, Object> data = validateRequestData(request);

            String auctionId = (String) data.get("auctionId");
            if (auctionId == null || auctionId.isEmpty()) {
                return Response.error("Thiếu auctionId");
            }
            // TODO: Cần thêm service.getAuctionById(auctionId) để lấy chi tiết
            return Response.error("Chưa implement: cần thêm AuctionService.getAuctionById()");
        } catch (AuthenticationException e) {
            return Response.error("Token không hợp lệ");
        } catch (Exception e) {
            return Response.error("Lỗi server: " + e.getMessage());
        }
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
