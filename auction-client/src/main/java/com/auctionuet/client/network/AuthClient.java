package com.auctionuet.client.network;

import com.auctionuet.protocol.ActionType;
import com.auctionuet.protocol.Request;
import com.auctionuet.protocol.Response;
import com.auctionuet.protocol.dto.request.auth.LoginRequestDTO;
import com.auctionuet.protocol.dto.request.auth.RegisterRequestDTO;
import com.auctionuet.protocol.dto.response.auth.LoginResponseDTO;

public class AuthClient {

    public LoginResponseDTO login(String username, String password) throws Exception {
        LoginRequestDTO data = new LoginRequestDTO();
        data.setUsername(username);
        data.setPassword(password);

        Response response = ServerConnection.getInstance()
                .sendRequest(Request.fromDto(ActionType.LOGIN, data, null));

        if ("OK".equals(response.getStatus())) {
            return response.getDataAs(LoginResponseDTO.class);
        }
        throw new Exception(response.getMessage());
    }

    public void register(String username, String password, String email, String role) throws Exception {
        RegisterRequestDTO data = new RegisterRequestDTO();
        data.setUsername(username);
        data.setPassword(password);
        data.setEmail(email);
        data.setRole(role);

        Response response = ServerConnection.getInstance()
                .sendRequest(Request.fromDto(ActionType.REGISTER, data, null));

        if (!"OK".equals(response.getStatus())) {
            throw new Exception(response.getMessage());
        }
    }

    public void logout(String token) throws Exception {
        Response response = ServerConnection.getInstance()
                .sendRequest(new Request(ActionType.LOGOUT, null, token));

        if (!"OK".equals(response.getStatus())) {
            throw new Exception(response.getMessage());
        }
    }
}
