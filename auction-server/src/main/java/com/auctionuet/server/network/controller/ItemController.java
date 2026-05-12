package com.auctionuet.server.network.controller;

import com.auctionuet.protocol.ActionType;
import com.auctionuet.protocol.Response;
import com.auctionuet.protocol.contract.Dto;
import com.auctionuet.protocol.enums.ItemType;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.domain.service.ItemService;
import com.auctionuet.server.persistence.schema.ItemSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    public Response handleCreateItem(Dto<?> requestData, User user) throws Exception {
        String typeStr = requestData.getString("type");
        ItemType type = ItemType.valueOf(typeStr);

        Map<String, Object> extraFields = requestData.toMap();
        extraFields.remove("name");
        extraFields.remove("description");
        extraFields.remove("startingPrice");
        extraFields.remove("type");
        extraFields.remove("imageUrl");
        extraFields.remove("condition");

        ItemSchema created = itemService.createItem(
                user,
                requestData.getString("name"),
                requestData.getString("description"),
                requestData.getDouble("startingPrice"),
                type,
                requestData.getString("imageUrl"),
                requestData.getString("condition"),
                extraFields
        );

        Dto<?> responseDto = type.parseData(itemToMap(created, user.getUsername()));
        return Response.ok(responseDto);
    }

    public Response handleGetMyItems(User user) throws Exception {
        List<ItemSchema> items = itemService.getItemsBySellerId(user.getId());
        List<Map<String, Object>> itemsData = new ArrayList<>();
        for (ItemSchema schema : items) {
            itemsData.add(itemToMap(schema, user.getUsername()));
        }

        return Response.ok(ActionType.GET_MY_ITEMS.createResponseDto().set("items", itemsData));
    }

    public static Map<String, Object> itemToMap(ItemSchema schema, String sellerUsername) {
        String json = com.auctionuet.protocol.util.NetworkGson.create().toJson(schema);
        Map<String, Object> map = com.auctionuet.protocol.util.NetworkGson.create().fromJson(json, Map.class);
        map.put("sellerUsername", sellerUsername);
        return map;
    }
}
