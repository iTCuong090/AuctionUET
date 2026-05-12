package com.auctionuet.server.network.controller;

import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.domain.service.ItemService;
import com.auctionuet.server.domain.manager.SessionManager;
import com.auctionuet.server.mapper.ItemMapper;
import com.auctionuet.protocol.dto.request.item.CreateItemRequestDTO;
import com.auctionuet.protocol.dto.response.item.ItemDTO;
import com.auctionuet.protocol.Request;
import com.auctionuet.protocol.Response;
import com.auctionuet.server.persistence.schema.ItemSchema;

import java.util.ArrayList;
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

        ItemSchema created = itemService.createItem(
            user, 
            req.getName(), 
            req.getDescription(), 
            req.getStartingPrice(), 
            req.getType(), 
            req.getImageUrl(), 
            req.getCondition(), 
            req.getExtraFields()
        );
        ItemDTO responseDTO = ItemMapper.toDTO(created, user.getUsername());

        return Response.ok(responseDTO);
    }

    // ──────────────────── GET MY ITEMS ────────────────────

    public Response handleGetMyItems(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());

        List<ItemSchema> items = itemService.getItemsBySellerId(user.getId());

        List<ItemDTO> dtoList = new ArrayList<>();
        for (ItemSchema schema : items) {
            dtoList.add(ItemMapper.toDTO(schema, user.getUsername()));
        }

        return Response.ok(dtoList);
    }
}
