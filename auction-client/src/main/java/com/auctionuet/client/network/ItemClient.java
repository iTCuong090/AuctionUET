package com.auctionuet.client.network;

import com.auctionuet.client.model.ItemDTO;
import com.auctionuet.client.network.protocol.Request;
import com.auctionuet.client.network.protocol.Response;
// IMPORT CÁI ENUM NÀY VÀO LÀ HẾT ĐỎ NÈ
import com.auctionuet.client.network.protocol.ActionType;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class ItemClient {
    private Gson gson = new Gson();

    public ItemDTO createItem(String token, Map<String, Object> itemData) throws Exception {
        // THAY VÌ DÙNG "CREATE_ITEM", TA DÙNG ENUM ActionType.CREATE_ITEM
        Request request = new Request(ActionType.CREATE_ITEM, itemData);
        request.setToken(token);

        Response response = ServerConnection.getInstance().sendRequest(request);

        if ("OK".equals(response.getStatus())) {
            return gson.fromJson(response.getData().toString(), ItemDTO.class);
        } else {
            throw new Exception(response.getMessage());
        }
    }

    public List<ItemDTO> getMyItems(String token) throws Exception {
        // TƯƠNG TỰ BÊN NÀY
        Request request = new Request(ActionType.GET_MY_ITEMS, null);
        request.setToken(token);

        Response response = ServerConnection.getInstance().sendRequest(request);

        if ("OK".equals(response.getStatus())) {
            Type listType = new TypeToken<List<ItemDTO>>(){}.getType();
            return gson.fromJson(response.getData().toString(), listType);
        } else {
            throw new Exception(response.getMessage());
        }
    }
}