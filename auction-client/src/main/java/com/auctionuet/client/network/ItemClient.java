package com.auctionuet.client.network;

import com.auctionuet.client.model.ItemDTO;
import com.auctionuet.client.network.protocol.Request;
import com.auctionuet.client.network.protocol.Response;
import com.auctionuet.client.network.protocol.ActionType;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class ItemClient {
    private Gson gson = new Gson();

    public ItemDTO createItem(String token, Map<String, Object> itemData) throws Exception {
        Request request = new Request(ActionType.CREATE_ITEM, itemData);
        request.setToken(token);

        Response response = ServerConnection.getInstance().sendRequest(request);

        if ("OK".equals(response.getStatus())) {
            // ĐÃ FIX: Ép cục data thành chuỗi JSON chuẩn (có dấu ngoặc kép)
            String jsonCorrect = gson.toJson(response.getData());

            // Sau đó mới cho máy Gson đổ khuôn
            return gson.fromJson(jsonCorrect, ItemDTO.class);
        } else {
            throw new Exception(response.getMessage());
        }
    }

    public List<ItemDTO> getMyItems(String token) throws Exception {
        Request request = new Request(ActionType.GET_MY_ITEMS, null);
        request.setToken(token);

        Response response = ServerConnection.getInstance().sendRequest(request);

        if ("OK".equals(response.getStatus())) {
            // ĐÃ FIX TƯƠNG TỰ Ở ĐÂY
            String jsonCorrect = gson.toJson(response.getData());

            Type listType = new TypeToken<List<ItemDTO>>(){}.getType();
            return gson.fromJson(jsonCorrect, listType);
        } else {
            throw new Exception(response.getMessage());
        }
    }
}