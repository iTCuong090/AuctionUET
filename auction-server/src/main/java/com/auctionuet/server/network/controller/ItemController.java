package com.auctionuet.server.network.controller;

import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.domain.service.ItemService;
import com.auctionuet.server.domain.manager.SessionManager;
import com.auctionuet.server.mapper.ItemMapper;
import com.auctionuet.server.network.dto.ItemDTO;
import com.auctionuet.server.network.protocol.Request;
import com.auctionuet.server.network.protocol.Response;
import com.auctionuet.server.persistence.schema.ItemSchema;
import com.auctionuet.server.util.AppLogger;

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

    public Response handleCreateItem(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        AppLogger.logControllerEnter("ItemController", "handleCreateItem",
            "user=" + user.getUsername() + " role=" + user.getRole());

        Map<String, Object> data = validateRequestData(request);
        ItemDTO itemDTO = ItemMapper.fromRequestData(data);
        ItemSchema created = itemService.createItem(user, itemDTO);
        ItemDTO responseDTO = ItemMapper.toDTO(created, user.getUsername());

        AppLogger.logControllerResult("ItemController", "handleCreateItem",
            "Created item id=" + created.getId() + " name=" + created.getName());
        return Response.ok(responseDTO);
    }

    // ──────────────────── GET MY ITEMS ────────────────────

    public Response handleGetMyItems(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        AppLogger.logControllerEnter("ItemController", "handleGetMyItems",
            "user=" + user.getUsername());

        List<ItemSchema> items = itemService.getItemsBySellerId(user.getId());

        List<ItemDTO> dtoList = new ArrayList<>();
        for (ItemSchema schema : items) {
            dtoList.add(ItemMapper.toDTO(schema, user.getUsername()));
        }

        AppLogger.logControllerResult("ItemController", "handleGetMyItems",
            "Returned " + dtoList.size() + " items for user=" + user.getUsername());
        return Response.ok(dtoList);
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
