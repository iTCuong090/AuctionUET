package com.auctionuet.protocol;

import java.util.Map;

public class Request {
    private ActionType action;
    private Map<String,Object> data;
    private String token;
    public Request(){}
    public Request(ActionType action,Map<String,Object> data,String token){
        this.action=action;
        this.data=data;
        this.token=token;
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
        if (data == null) return null;
        String json = com.auctionuet.protocol.util.NetworkGson.create().toJson(this.data);
        return com.auctionuet.protocol.util.NetworkGson.create().fromJson(json, clazz);
    }
    public String getDataString(String key) {
        Object value = data.get(key); // Láº¥y giÃ¡ trá»‹ ra dÆ°á»›i dáº¡ng Object

        if (value == null) {
            return null;
        }
        // DÃ¹ giÃ¡ trá»‹ gá»‘c lÃ  sá»‘ (Double), boolean, hay chuá»—i, nÃ³ Ä‘á»u convert vá» dáº¡ng String háº¿t.
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
            // Tráº£ vá»  null náº¿u dá»¯ liá»‡u bá»‹ sai Ä‘á»‹nh dáº¡ng (vÃ­ dá»¥ gá»­i lÃªn "TÃ¡m tÃ¡m tÃ¡m tÃ¡m")
            return null;
        }
    }

    public String toJson() {
        return com.auctionuet.protocol.util.NetworkGson.create().toJson(this);
    }
}
