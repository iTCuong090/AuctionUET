package com.auctionuet.server.network.controller;

import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.domain.service.ItemService;
import com.auctionuet.server.domain.service.SessionManager;
import com.auctionuet.server.exception.AuctionException;
import com.auctionuet.server.exception.AuthenticationException;
import com.auctionuet.server.mapper.ItemMapper;
import com.auctionuet.server.network.dto.ItemDTO;
import com.auctionuet.server.network.protocol.Request;
import com.auctionuet.server.network.protocol.Response;
import com.auctionuet.server.persistence.schema.ItemSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Controller chuyên xử lý các request liên quan tới Item Management.
 * Được tách ra từ AuctionController để tuân thủ Single Responsibility Principle.
 */
public class ItemController {

    private final ItemService itemService;
    private final SessionManager sessionManager;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
        this.sessionManager = SessionManager.getInstance();
    }

    // ──────────────────── CREATE ITEM ────────────────────

    public Response handleCreateItem(Request request) {
        try {
            User user = sessionManager.validateToken(request.getToken());
            Map<String, Object> data = validateRequestData(request);
            ItemDTO itemDTO = ItemMapper.fromRequestData(data);
            ItemSchema created = itemService.createItem(user, itemDTO);
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

    // ──────────────────── GET MY ITEMS ────────────────────

    public Response handleGetMyItems(Request request) {
        try {
            User user = sessionManager.validateToken(request.getToken());
            List<ItemSchema> items = itemService.getItemsBySellerId(user.getId());

            List<ItemDTO> dtoList = new ArrayList<>();
            for (ItemSchema schema : items) {
                dtoList.add(ItemMapper.toDTO(schema, user.getUsername()));
            }

            return Response.ok(dtoList);
        } catch (AuthenticationException e) {
            return Response.error("Token không hợp lệ");
        } catch (Exception e) {
            return Response.error("Lỗi server: " + e.getMessage());
        }
    }

    // ──────────────────── HELPERS ────────────────────

    private Map<String, Object> validateRequestData(Request request) {
        Map<String, Object> data = request.getData();
        if (data == null) {
            throw new IllegalArgumentException("Request thiếu data");
        }
        return data;
    }
}
