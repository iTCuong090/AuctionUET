package com.auctionuet.protocol;

public class PushMessage {
    private final String type = "PUSH";
    private PushActionType pushType;
    private Object data;

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

    public String toJson() {
        return com.auctionuet.protocol.util.NetworkGson.create().toJson(this);
    }
}

