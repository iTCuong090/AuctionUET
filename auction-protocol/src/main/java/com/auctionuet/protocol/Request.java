package com.auctionuet.protocol;

import com.auctionuet.protocol.dto.ValidatableDTO;
import com.auctionuet.protocol.util.NetworkGson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

public class Request {
    private ActionType action;
    private Map<String, Object> data;
    private String token;

    public Request() {}

    public Request(ActionType action, Map<String, Object> data) {
        this(action, data, null);
    }

    public Request(ActionType action, Map<String, Object> data, String token) {
        this.action = action;
        this.data = data;
        this.token = token;
    }

    public static Request fromDto(ActionType action, ValidatableDTO dto, String token) {
        if (dto == null) {
            return new Request(action, null, token);
        }

        dto.validate();
        String json = NetworkGson.create().toJson(dto);
        Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> data = NetworkGson.create().fromJson(json, mapType);
        return new Request(action, data, token);
    }

    public ActionType getAction() {
        return action;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public String getToken() {
        return token;
    }

    public void setAction(ActionType action) {
        this.action = action;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public <T> T getDataAs(Class<T> clazz) {
        if (data == null) {
            throw new IllegalArgumentException("Request thieu data");
        }
        String json = NetworkGson.create().toJson(this.data);
        T dto = NetworkGson.create().fromJson(json, clazz);
        if (dto instanceof ValidatableDTO validatableDTO) {
            validatableDTO.validate();
        }
        return dto;
    }

    public String getDataString(String key) {
        Object value = data.get(key);

        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    public Integer getDataInt(String key) {
        Object value = data.get(key);

        if (value == null) {
            return null;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String toJson() {
        return NetworkGson.create().toJson(this);
    }
}
