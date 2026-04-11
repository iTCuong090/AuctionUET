package com.auctionuet.client.network.protocol;

import java.util.Map;

public class Request {
    private ActionType action; // Đã đổi sang Enum
    private Map<String, Object> data; // Đã đổi sang Map
    private String token;

    public Request(ActionType action, Map<String, Object> data) {
        this.action = action;
        this.data = data;
    }

    public ActionType getAction() { return action; }
    public Map<String, Object> getData() { return data; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}