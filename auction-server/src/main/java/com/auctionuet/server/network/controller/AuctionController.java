package com.auctionuet.server.network.controller;

import com.auctionuet.protocol.ActionType;
import com.auctionuet.protocol.Response;
import com.auctionuet.protocol.contract.Dto;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.domain.service.AuctionService;
import com.auctionuet.server.domain.service.ItemService;
import com.auctionuet.server.persistence.schema.AuctionSchema;
import com.auctionuet.server.persistence.schema.ItemSchema;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuctionController {

    private final AuctionService auctionService;
    private final ItemService itemService;

    public AuctionController(AuctionService auctionService, ItemService itemService) {
        this.auctionService = auctionService;
        this.itemService = itemService;
    }

    public Response handleCreateAuction(Dto<?> requestData, User user) throws Exception {
        Integer antiSnipingWindowSeconds = requestData.getInt("antiSnipingWindowSeconds");
        if (antiSnipingWindowSeconds == null) antiSnipingWindowSeconds = 60;

        Integer antiSnipingExtensionSeconds = requestData.getInt("antiSnipingExtensionSeconds");
        if (antiSnipingExtensionSeconds == null) antiSnipingExtensionSeconds = 120;

        AuctionSchema auctionSchema = auctionService.createAuction(
                user,
                requestData.getString("itemId"),
                LocalDateTime.parse(requestData.getString("startTime")),
                LocalDateTime.parse(requestData.getString("endTime")),
                requestData.getString("title"),
                requestData.getString("description"),
                antiSnipingWindowSeconds,
                antiSnipingExtensionSeconds
        );

        ItemSchema itemSchema = itemService.getItemById(auctionSchema.getItemId());
        Map<String, Object> itemMap = null;
        if (itemSchema != null) {
            itemMap = ItemController.itemToMap(itemSchema, user.getUsername());
        }

        Map<String, Object> auctionMap = auctionToMap(auctionSchema, itemMap, user.getUsername(), null);
        return Response.ok(Dto.untyped(auctionMap));
    }

    public Response handleStartAuction(Dto<?> requestData, User user) throws Exception {
        auctionService.startAuction(user, requestData.getString("auctionId"));
        return Response.ok("Da bat dau phien dau gia");
    }

    public Response handleGetAuctions() throws Exception {
        List<AuctionSchema> auctions = auctionService.getAuctions();
        List<Map<String, Object>> auctionList = new ArrayList<>();

        for (AuctionSchema auction : auctions) {
            ItemSchema itemSchema = itemService.getItemById(auction.getItemId());
            if (itemSchema == null) continue;
            String sellerUsername = itemSchema.getSellerId();
            Map<String, Object> itemMap = ItemController.itemToMap(itemSchema, sellerUsername);
            auctionList.add(auctionToMap(auction, itemMap, sellerUsername, null));
        }

        return Response.ok(ActionType.GET_AUCTIONS.createResponseDto().set("auctions", auctionList));
    }

    public Response handleGetAuctionDetail(Dto<?> requestData) throws Exception {
        String auctionId = requestData.getString("auctionId");
        AuctionSchema schema = auctionService.getAuctionById(auctionId);
        if (schema == null) {
            return Response.error("Khong tim thay phien dau gia");
        }

        ItemSchema itemSchema = itemService.getItemById(schema.getItemId());
        if (itemSchema == null) {
            return Response.error("Khong tim thay vat pham cua phien dau gia nay");
        }

        String sellerUsername = itemSchema.getSellerId();
        Map<String, Object> itemMap = ItemController.itemToMap(itemSchema, sellerUsername);
        Map<String, Object> auctionMap = auctionToMap(schema, itemMap, sellerUsername, null);
        return Response.ok(Dto.untyped(auctionMap));
    }

    public Response handlePayAuction(Dto<?> requestData, User user) throws Exception {
        auctionService.payAuction(user, requestData.getString("auctionId"));
        return Response.ok("Thanh toan thanh cong! San pham da thuoc ve ban.");
    }

    public static Map<String, Object> auctionToMap(
            AuctionSchema schema,
            Map<String, Object> itemMap,
            String sellerUsername,
            String currentWinnerUsername
    ) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", schema.getId());
        map.put("item", itemMap);
        map.put("sellerUsername", sellerUsername);
        map.put("title", schema.getTitle());
        map.put("description", schema.getDescription());
        map.put("startTime", schema.getStartTime().toString());
        map.put("endTime", schema.getEndTime().toString());
        map.put("status", schema.getStatus().name());
        map.put("currentHighestBid", schema.getHighestBid());
        map.put("currentWinnerUsername", currentWinnerUsername);
        return map;
    }
}
