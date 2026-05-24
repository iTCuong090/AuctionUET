package com.auctionuet.server.network.controller;

import com.auctionuet.protocol.Request;
import com.auctionuet.protocol.Response;
import com.auctionuet.protocol.dto.request.user.UpdateProfileRequestDTO;
import com.auctionuet.protocol.dto.response.user.UserDTO;
import com.auctionuet.protocol.enums.Permission;
import com.auctionuet.server.domain.manager.SessionManager;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.domain.service.UserService;
import com.auctionuet.server.persistence.schema.UserSchema;

public class UserController {
    private final UserService userService;
    private final SessionManager sessionManager;

    public UserController(UserService userService) {
        this.userService = userService;
        this.sessionManager = SessionManager.getInstance();
    }

    public Response handleGetProfile(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        if (!user.hasPermission(Permission.GET_PROFILE)) {
            return Response.error("Bạn không có quyền xem profile");
        }

        UserSchema schema = userService.getUserSchemaById(user.getId());
        return Response.ok(userService.toDTO(schema));
    }

    public Response handleUpdateProfile(Request request) throws Exception {
        User user = sessionManager.validateToken(request.getToken());
        if (!user.hasPermission(Permission.UPDATE_PROFILE)) {
            return Response.error("Bạn không có quyền cập nhật profile");
        }

        UpdateProfileRequestDTO req = request.getDataAs(UpdateProfileRequestDTO.class);
        UserDTO dto = userService.updateProfile(
                user.getId(),
                req.getUsername(),
                req.getCurrentPassword(),
                req.getNewPassword());
        sessionManager.updateSessionUser(request.getToken(), userService.toDomainUser(
                userService.getUserSchemaById(dto.getId())));
        return Response.ok("Cập nhật profile thành công", dto);
    }
}
