package com.auctionuet.server.network.controller;

import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.domain.service.AuctionService;
import com.auctionuet.server.domain.service.ItemService;
import com.auctionuet.server.domain.service.SessionManager;
import com.auctionuet.server.exception.AuctionException;
import com.auctionuet.server.exception.AuthenticationException;
import com.auctionuet.server.mapper.AuctionMapper;
import com.auctionuet.server.mapper.ItemMapper;
import com.auctionuet.server.network.dto.AuctionDTO;
import com.auctionuet.server.network.dto.ItemDTO;
import com.auctionuet.server.network.protocol.Request;
import com.auctionuet.server.network.protocol.Response;
import com.auctionuet.server.persistence.schema.AuctionSchema;
import com.auctionuet.server.persistence.schema.ItemSchema;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
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

            AuctionSchema auctionSchema = auctionService.createAuction(
                    user, itemId, startTime, endTime, title, description
            );

            // Convert to DTO for response
            ItemSchema itemSchema = itemService.getItemById(auctionSchema.getItemId());
            ItemDTO itemDTO = null;
            if (itemSchema != null) {
                itemDTO = ItemMapper.toDTO(itemSchema, user.getUsername());
            }
            AuctionDTO auctionDTO = AuctionMapper.toDTO(auctionSchema, itemDTO, user.getUsername(), null);
            return Response.ok(auctionDTO);
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
            List<AuctionDTO> auctionDTOList = new ArrayList<>();
            for (AuctionSchema auction : auctions) {
                ItemSchema itemSchema = itemService.getItemById(auction.getItemId());
                if (itemSchema == null) {
                    System.err.println("Cảnh báo: Bỏ qua đấu giá " + auction.getId() + " do không tìm thấy vật phẩm!");
                    continue;
                }
                String sellerUsername = itemSchema.getSellerId();
                ItemDTO itemDTO = ItemMapper.toDTO(itemSchema, sellerUsername);
                AuctionDTO auctionDTO = AuctionMapper.toDTO(auction, itemDTO, sellerUsername, null);
                auctionDTOList.add(auctionDTO);
            }
            return Response.ok(auctionDTOList);
        } catch (AuthenticationException e) {
            return Response.error("Token không hợp lệ");
        } catch (Exception e) {
            return Response.error("Lỗi server: " + e.getMessage());
        }
    }

    public Response handleGetAuctionDetail(Request request) {
        try {
            User user = sessionManager.validateToken(request.getToken());
            Map<String, Object> data = validateRequestData(request);

            String auctionId = (String) data.get("auctionId");
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
            return Response.ok(auctionDTO);
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
