package com.auctionuet.client.network;

import com.auctionuet.protocol.ActionType;
import com.auctionuet.protocol.Request;
import com.auctionuet.protocol.Response;
import com.auctionuet.protocol.dto.request.user.UpdateProfileRequestDTO;
import com.auctionuet.protocol.dto.response.user.UserDTO;

public class ProfileClient {

    public UserDTO getProfile(String token) throws Exception {
        Response response = ServerConnection.getInstance()
                .sendRequest(new Request(ActionType.GET_PROFILE, null, token));

        if ("OK".equals(response.getStatus())) {
            return response.getDataAs(UserDTO.class);
        }
        throw new Exception(response.getMessage());
    }

    public UserDTO updateProfile(
            String token,
            String username,
            String currentPassword,
            String newPassword) throws Exception {
        UpdateProfileRequestDTO data = new UpdateProfileRequestDTO();
        data.setUsername(username);
        data.setCurrentPassword(currentPassword);
        data.setNewPassword(newPassword);

        Response response = ServerConnection.getInstance()
                .sendRequest(Request.fromDto(ActionType.UPDATE_PROFILE, data, token));

        if ("OK".equals(response.getStatus())) {
            return response.getDataAs(UserDTO.class);
        }
        throw new Exception(response.getMessage());
    }
}
