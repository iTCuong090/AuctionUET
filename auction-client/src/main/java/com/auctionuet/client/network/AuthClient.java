package com.auctionuet.client.network;

import com.auctionuet.client.network.protocol.ActionType;
import com.auctionuet.client.network.protocol.Request;
import com.auctionuet.client.network.protocol.Response;

import java.util.HashMap;
import java.util.Map;

public class AuthClient {

    public Response login(String username, String password) throws Exception {
        // Nhét dữ liệu vào một cái Map (chuẩn ý thằng Khánh)
        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        data.put("password", password);

        // Gói hàng lại, gán nhãn LOGIN
        Request req = new Request(ActionType.LOGIN, data);

        // Ném vào ống nước và chờ Server trả kết quả về
        return ServerConnection.getInstance().sendRequest(req);
    }

    public Response register(String username, String password, String email, String role) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        data.put("password", password);
        data.put("email", email);
        data.put("role", role);

        Request req = new Request(ActionType.REGISTER, data);
        return ServerConnection.getInstance().sendRequest(req);
    }

    public Response logout(String token) throws Exception {
        Request req = new Request(ActionType.LOGOUT, null);
        req.setToken(token);
        return ServerConnection.getInstance().sendRequest(req);
    }
}