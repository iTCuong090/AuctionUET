package com.auctionuet.protocol;

import com.auctionuet.protocol.util.NetworkGson;

public class PushMessage {
    private final String type = "PUSH";
    private PushActionType pushType;
    private Object data;

    public PushMessage() {}

    public PushMessage(PushActionType pushType, Object data) {
        this.pushType = pushType;
        this.data = data;
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

    public <T> T getDataAs(Class<T> clazz) {
        if (data == null) {
            return null;
        }
        return NetworkGson.create().fromJson(NetworkGson.create().toJson(data), clazz);
    }

    public String toJson() {
        return NetworkGson.create().toJson(this);
    }
}
