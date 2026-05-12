package com.auctionuet.server.network.controller;

import com.auctionuet.protocol.Response;
import com.auctionuet.protocol.contract.Dto;
import com.auctionuet.protocol.enums.UserRole;
import com.auctionuet.server.domain.manager.SessionManager;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.domain.service.AuthService;

public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public Response handleLogin(Dto<?> requestData) throws Exception {
        String username = requestData.getString("username");
        String password = requestData.getString("password");
        Dto<?> responseData = authService.login(username, password);
        return Response.ok(responseData);
    }

    public Response handleRegister(Dto<?> requestData) throws Exception {
        String username = requestData.getString("username");
        String password = requestData.getString("password");
        String email = requestData.getString("email");
        String roleStr = requestData.getString("role");
        UserRole role = roleStr != null ? UserRole.valueOf(roleStr) : UserRole.BIDDER;
        authService.register(username, password, email, role);
        return Response.ok("Dang ky thanh cong");
    }

    public Response handleLogout(User user) throws Exception {
        SessionManager.getInstance().invalidateByUserId(user.getId());
        return Response.ok("Da dang xuat");
    }
}
