package com.auctionuet.client.network;

import com.auctionuet.client.model.ItemDTO;
import com.auctionuet.protocol.ActionType;
import com.auctionuet.protocol.Request;
import com.auctionuet.protocol.Response;
import com.auctionuet.protocol.contract.Dto;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class ItemClient {
    private final Gson gson = new Gson();

    public ItemDTO createItem(String token, Map<String, Object> itemData) throws Exception {
        Dto<?> data = ActionType.CREATE_ITEM.parseRequest(itemData);
        data.validate();

        Request request = new Request(ActionType.CREATE_ITEM, data);
        request.setToken(token);

        Response response = ServerConnection.getInstance().sendRequest(request);
        if ("OK".equals(response.getStatus())) {
            String jsonCorrect = gson.toJson(response.getData());
            return gson.fromJson(jsonCorrect, ItemDTO.class);
        }
        throw new Exception(response.getMessage());
    }

    public List<ItemDTO> getMyItems(String token) throws Exception {
        Request request = new Request(ActionType.GET_MY_ITEMS, (Dto<?>) null);
        request.setToken(token);

        Response response = ServerConnection.getInstance().sendRequest(request);
        if ("OK".equals(response.getStatus())) {
            String jsonCorrect = gson.toJson(response.getData());
            Type listType = new TypeToken<List<ItemDTO>>() {}.getType();
            return gson.fromJson(jsonCorrect, listType);
        }
        throw new Exception(response.getMessage());
    }
}
