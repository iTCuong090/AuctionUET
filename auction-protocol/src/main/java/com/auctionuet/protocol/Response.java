package com.auctionuet.protocol;

import com.auctionuet.protocol.contract.Dto;
import com.auctionuet.protocol.contract.DtoContract;

public class Response<T extends DtoContract> {
    private String type, status, event, message;
    private Object data;

    private Response(String type, String status, Object data, String event, String message) {
        this.type = type;
        this.status = status;
        this.data = data;
        this.event = event;
        this.message = message;
    }

    public static <T extends DtoContract> Response<T> ok(Dto<T> dto) {
        return new Response<>("RESPONSE", "OK", dto != null ? dto.toMap() : null, null, null);
    }
    
    @SuppressWarnings("unchecked")
    public static <T extends DtoContract> Response<T> ok(Object data) {
        if (data instanceof Dto) {
            return ok((Dto<T>) data);
        }
        return new Response<>("RESPONSE", "OK", data, null, null);
    }

    public static <T extends DtoContract> Response<T> ok(String message) {
        return new Response<>("RESPONSE", "OK", null, null, message);
    }

    public static <T extends DtoContract> Response<T> ok(String message, Dto<T> dto) {
        return new Response<>("RESPONSE", "OK", dto != null ? dto.toMap() : null, null, message);
    }

    public static <T extends DtoContract> Response<T> error(String message) {
        return new Response<>("RESPONSE", "ERROR", null, null, message);
    }

    public static <T extends DtoContract> Response<T> push(String event, Dto<T> dto) {
        return new Response<>("RESPONSE", "OK", dto != null ? dto.toMap() : null, event, null);
    }
    
    @SuppressWarnings("unchecked")
    public static <T extends DtoContract> Response<T> push(String event, Object data) {
        if (data instanceof Dto) {
            return push(event, (Dto<T>) data);
        }
        return new Response<>("RESPONSE", "OK", data, event, null);
    }

    public Object getData() {
        return data;
    }
    
    @SuppressWarnings("unchecked")
    public Dto<T> getDataObject(ActionType actionType) {
        if (actionType != null && data instanceof java.util.Map) {
            return (Dto<T>) actionType.parseResponse((java.util.Map<String, Object>) data);
        } else if (data instanceof java.util.Map) {
            return (Dto<T>) Dto.untyped((java.util.Map<String, Object>) data);
        }
        return (Dto<T>) Dto.untyped(data);
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

    public String toJson() {
        return com.auctionuet.protocol.util.NetworkGson.create().toJson(this);
    }
}
