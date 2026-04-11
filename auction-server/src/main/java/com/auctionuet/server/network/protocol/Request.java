package com.auctionuet.server.network.protocol;

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
}

