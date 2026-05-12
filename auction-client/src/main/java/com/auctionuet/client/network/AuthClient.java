package com.auctionuet.client.network;

import com.auctionuet.protocol.ActionType;
import com.auctionuet.protocol.Request;
import com.auctionuet.protocol.Response;
import com.auctionuet.protocol.contract.Dto;

public class AuthClient {

    public Response login(String username, String password) throws Exception {

        Dto<?> data = ActionType.LOGIN.createRequestDto()
                .set("username", username)
                .set("password", password);

        // Gói hàng lại, gán nhãn LOGIN
        Request req = new Request(ActionType.LOGIN, data);

        // Ném vào ống nước và chờ Server trả kết quả về
        return ServerConnection.getInstance().sendRequest(req);
    }

    public Response register(String username, String password, String email, String role) throws Exception {
        Dto<?> data = ActionType.REGISTER.createRequestDto()
                .set("username", username)
                .set("password", password)
                .set("email", email)
                .set("role", role);

        Request req = new Request(ActionType.REGISTER, data);
        return ServerConnection.getInstance().sendRequest(req);
    }

    public Response logout(String token) throws Exception {
        Request req = new Request(ActionType.LOGOUT, (Dto<?>) null);
        req.setToken(token);
        return ServerConnection.getInstance().sendRequest(req);
    }
}
