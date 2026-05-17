package com.auctionuet.server.network.controller;

import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.domain.service.ItemService;
import com.auctionuet.server.domain.manager.SessionManager;
import com.auctionuet.protocol.dto.request.item.CreateItemRequestDTO;
import com.auctionuet.protocol.dto.response.item.ItemDTO;
import com.auctionuet.protocol.Request;
import com.auctionuet.protocol.Response;
import java.util.List;

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

        CreateItemRequestDTO req = request.getDataAs(CreateItemRequestDTO.class);

        ItemDTO responseDTO = itemService.createItem(
            user, 
            req.getName(), 
            req.getDescription(), 
            req.getStartingPrice(), 
            req.getType(), 
            req.getImageUrl(), 
            req.getCondition(), 
            req.getExtraFields()
        );

        return Response.ok(responseDTO);
    }

    // ──────────────────── GET MY ITEMS ────────────────────

    public Response handleGetMyItems(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        List<ItemDTO> items = itemService.getItemsBySellerId(user.getId());
        return Response.ok(items);
    }
}
