package com.auctionuet.protocol;

import com.auctionuet.protocol.contract.Dto;

public class PushMessage {
    private final String type = "PUSH";
    private PushActionType pushType;
    private Object data;

    public PushMessage(PushActionType pushType, Object data) {
        this.pushType = pushType;
        if (data instanceof Dto) {
            this.data = ((Dto<?>) data).toMap();
        } else {
            this.data = data;
        }
    }
    
    public PushMessage(PushActionType pushType, Dto<?> dto) {
        this.pushType = pushType;
        this.data = dto != null ? dto.toMap() : null;
    }

    public String getType() {
        return type;
    }

    public PushActionType getPushType() {
        return pushType;
    }

    public Object getData() {
        return data;
    }
    
    public Dto<?> getDataObject() {
        if (pushType != null && data instanceof java.util.Map) {
            return pushType.parseData((java.util.Map<String, Object>) data);
        } else if (data instanceof java.util.Map) {
            return Dto.untyped((java.util.Map<String, Object>) data);
        }
        return Dto.untyped(data);
    }

    public String toJson() {
        return com.auctionuet.protocol.util.NetworkGson.create().toJson(this);
    }
}
