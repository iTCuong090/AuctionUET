package com.auctionuet.server;

import com.auctionuet.protocol.Request;
import com.auctionuet.protocol.Response;
import com.auctionuet.server.util.json.GsonFactory;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class MessageSerializerTest {

    private static Gson gson = GsonFactory.createForNetwork();

    // 1. Hàm serialize cho Response (Server gửi đi)
    public static String serialize(Response response){
        if (response == null) {
            return "";
        }
        return gson.toJson(response);
    }

    // [BỔ SUNG] 2. Hàm serialize cho Request (Dùng để Client gửi lên, hoặc Server test round-trip)
    public static String serialize(Request request){
        if (request == null) {
            return "";
        }
        return gson.toJson(request);
    }

    // 3. Hàm deserialize JSON thành Request (Server nhận về)
    public static Request deserialize(String json){
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return gson.fromJson(json, Request.class);
        } catch(JsonSyntaxException e){
            return null;
        }
    }
}
