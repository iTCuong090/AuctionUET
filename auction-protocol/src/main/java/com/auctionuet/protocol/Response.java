package com.auctionuet.protocol;

import com.auctionuet.protocol.util.NetworkGson;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Response {
    private String type, status, event, message;
    private Object data;

    public Response() {}

    private Response(String type, String status, Object data, String event, String message) {
        this.type = type;
        this.status = status;
        this.data = data;
        this.event = event;
        this.message = message;
    }

    public static Response ok(Object data) {
        return new Response("RESPONSE", "OK", data, null, null);
    }

    public static Response ok(String message) {
        return new Response("RESPONSE", "OK", null, null, message);
    }

    public static Response ok(String message, Object data) {
        return new Response("RESPONSE", "OK", data, null, message);
    }

    public static Response error(String message) {
        return new Response("RESPONSE", "ERROR", null, null, message);
    }

    public static Response push(String event, Object data) {
        return new Response("RESPONSE", "OK", data, event, null);
    }

    public Object getData() {
        return data;
    }

    public String getEvent() {
        return event;
    }

    public String getMessage() {
        return message;
    }

    public String getStatus() {
        return status;
    }

    public String getType() {
        return type;
    }

    public <T> T getDataAs(Class<T> clazz) {
        if (data == null) {
            return null;
        }

        Gson gson = NetworkGson.create();
        return gson.fromJson(gson.toJson(data), clazz);
    }

    public <T> List<T> getDataListAs(Class<T> clazz) {
        if (data == null) {
            return Collections.emptyList();
        }

        Gson gson = NetworkGson.create();
        JsonElement element = JsonParser.parseString(gson.toJson(data));
        if (!element.isJsonArray()) {
            throw new IllegalArgumentException("Response data is not a list");
        }

        JsonArray array = element.getAsJsonArray();
        List<T> result = new ArrayList<>(array.size());
        for (JsonElement item : array) {
            result.add(gson.fromJson(item, clazz));
        }
        return result;
    }

    public String toJson() {
        return NetworkGson.create().toJson(this);
    }
}
