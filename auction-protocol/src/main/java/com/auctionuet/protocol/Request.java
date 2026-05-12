package com.auctionuet.protocol;

import com.auctionuet.protocol.contract.Dto;
import com.auctionuet.protocol.contract.DtoContract;

import java.util.Map;

public class Request<T extends DtoContract> {
    private ActionType action;
    private Map<String, Object> data;
    private String token;

    public Request() {}

    public Request(ActionType action, Map<String, Object> data, String token) {
        this.action = action;
        this.data = data;
        this.token = token;
    }

    public Request(ActionType action, Map<String, Object> data) {
        this.action = action;
        this.data = data;
    }
    
    public Request(ActionType action, Dto<T> dto, String token) {
        this.action = action;
        this.data = dto != null ? dto.toMap() : null;
        this.token = token;
    }

    public Request(ActionType action, Dto<T> dto) {
        this.action = action;
        this.data = dto != null ? dto.toMap() : null;
    }

    public ActionType getAction() {
        return action;
    }

    public void setAction(ActionType action) {
        this.action = action;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
    
    @SuppressWarnings("unchecked")
    public Dto<T> getDataObject() {
        if (action == null) return (Dto<T>) Dto.untyped(data);
        return (Dto<T>) action.parseRequest(data);
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String toJson() {
        return com.auctionuet.protocol.util.NetworkGson.create().toJson(this);
    }
}
