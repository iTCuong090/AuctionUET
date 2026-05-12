package com.auctionuet.protocol;

import com.auctionuet.protocol.dto.ValidatableDTO;

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
        T dto = com.auctionuet.protocol.util.NetworkGson.create().fromJson(json, clazz);
        if (dto instanceof ValidatableDTO validatableDTO) {
            validatableDTO.validate();
        }
        return dto;
    }
    public String getDataString(String key) {
        Object value = data.get(key); // Lấy giá trị ra dưới dạng Object

        if (value == null) {
            return null;
        }
        // Dù giá trị gốc là số (Double), boolean, hay chuỗi, nó đều convert về dạng String hết.
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
            // Trả về null nếu dữ liệu bị sai định dạng (ví dụ gửi lên "Tám tám tám tám")
            return null;
        }
    }

    public String toJson() {
        return com.auctionuet.protocol.util.NetworkGson.create().toJson(this);
    }
}
