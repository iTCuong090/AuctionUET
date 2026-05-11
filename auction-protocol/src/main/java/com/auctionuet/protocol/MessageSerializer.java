package com.auctionuet.protocol;

import com.auctionuet.protocol.util.NetworkGson;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class MessageSerializer {
    private static Gson gson=NetworkGson.create();
    public static String serialize(Response response){
        if (response== null) {
            return "";
        }
        return gson.toJson(response);
    }
    public static Request deserialize(String json){
        if (json ==null || json.trim().isEmpty()) {
        return null;
        }
        try {
            return gson.fromJson(json, Request.class);
        }
        catch(JsonSyntaxException e){
            return null;
        }
    }
}


