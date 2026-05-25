package com.auctionuet.server.network.controller;

import com.auctionuet.protocol.Request;
import com.auctionuet.protocol.Response;
import com.auctionuet.protocol.dto.request.admin.GetAllUsersRequestDTO;
import com.auctionuet.protocol.dto.request.admin.UpdateUserStatusRequestDTO;
import com.auctionuet.protocol.dto.response.admin.AdminUserDTO;
import com.auctionuet.protocol.enums.Permission;
import com.auctionuet.server.domain.manager.SessionManager;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.domain.service.AdminService;

import java.util.List;

public class AdminController {
    private final AdminService adminService;
    private final SessionManager sessionManager;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
        this.sessionManager = SessionManager.getInstance();
    }

    public Response handleGetAllUsers(Request request) {
        requirePermission(request, Permission.GET_ALL_USERS);
        GetAllUsersRequestDTO filter = request.getData() == null
                ? new GetAllUsersRequestDTO()
                : request.getDataAs(GetAllUsersRequestDTO.class);
        List<AdminUserDTO> users = adminService.getUsers(
                filter.getUsernameQuery(),
                filter.getRole(),
                filter.getStatus());
        return Response.ok(users);
    }

    public Response handleUpdateUserStatus(Request request) {
        User admin = requirePermission(request, Permission.UPDATE_USER_STATUS);
        UpdateUserStatusRequestDTO update = request.getDataAs(UpdateUserStatusRequestDTO.class);
        return Response.ok(adminService.updateUserStatus(admin, update.getUserId(), update.getStatus()));
    }

    private User requirePermission(Request request, Permission permission) {
        User user = sessionManager.validateToken(request.getToken());
        if (!user.hasPermission(permission)) {
            throw new IllegalArgumentException("Bạn không có quyền quản trị người dùng");
        }
        return user;
    }
}

