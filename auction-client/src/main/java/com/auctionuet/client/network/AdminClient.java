package com.auctionuet.client.network;

import com.auctionuet.protocol.ActionType;
import com.auctionuet.protocol.Request;
import com.auctionuet.protocol.Response;
import com.auctionuet.protocol.dto.request.admin.GetAllUsersRequestDTO;
import com.auctionuet.protocol.dto.request.admin.UpdateUserStatusRequestDTO;
import com.auctionuet.protocol.dto.response.admin.AdminUserDTO;
import com.auctionuet.protocol.enums.AccountStatus;
import com.auctionuet.protocol.enums.UserRole;

import java.util.List;

public class AdminClient {

    public List<AdminUserDTO> getUsers(
            String token,
            String usernameQuery,
            UserRole role,
            AccountStatus status) throws Exception {
        GetAllUsersRequestDTO filter = new GetAllUsersRequestDTO();
        filter.setUsernameQuery(usernameQuery);
        filter.setRole(role);
        filter.setStatus(status);

        Response response = ServerConnection.getInstance()
                .sendRequest(Request.fromDto(ActionType.GET_ALL_USERS, filter, token));
        if ("OK".equals(response.getStatus())) {
            return response.getDataListAs(AdminUserDTO.class);
        }
        throw new Exception(response.getMessage());
    }

    public AdminUserDTO updateUserStatus(String token, String userId, AccountStatus status) throws Exception {
        UpdateUserStatusRequestDTO update = new UpdateUserStatusRequestDTO();
        update.setUserId(userId);
        update.setStatus(status);

        Response response = ServerConnection.getInstance()
                .sendRequest(Request.fromDto(ActionType.UPDATE_USER_STATUS, update, token));
        if ("OK".equals(response.getStatus())) {
            return response.getDataAs(AdminUserDTO.class);
        }
        throw new Exception(response.getMessage());
    }
}
