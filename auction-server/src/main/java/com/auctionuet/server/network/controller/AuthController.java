package com.auctionuet.server.network.controller;

import com.auctionuet.server.domain.enums.UserRole;
import com.auctionuet.server.domain.service.AuthService;
import com.auctionuet.server.domain.service.SessionManager;
import com.auctionuet.server.exception.AuthenticationException;
import com.auctionuet.server.exception.DuplicateUserException;
import com.auctionuet.server.exception.UserNotFoundException;
import com.auctionuet.server.mapper.UserMapper;
import com.auctionuet.server.network.dto.UserDTO;
import com.auctionuet.server.network.protocol.Request;
import com.auctionuet.server.network.protocol.Response;
import com.auctionuet.server.domain.service.LoginResult;

import java.util.Map;

public class AuthController {
    AuthService authService;
    public AuthController(AuthService authService){
        this.authService=authService;
    }
    public Response handleLogin(Request request) throws Exception {
        String username = (String) request.getData().get("username");
        String password = (String) request.getData().get("password");
        LoginResult result = authService.login(username, password);
        UserDTO dto = UserMapper.toDTO(result.getUser());
        Map<String, Object> responseData = Map.of("token", result.getToken(), "user", dto);
        return Response.ok(responseData);
    }

    public Response handleRegister(Request request) throws Exception {
        Map<String, Object> data = request.getData();
        String username = request.getDataString("username");
        String password = request.getDataString("password");
        String email = request.getDataString("email");
        String roleStr = request.getDataString("role");
        UserRole role = (roleStr != null) ? UserRole.valueOf(roleStr) : UserRole.BIDDER;
        authService.register(username, password, email, role);
        return Response.ok("Đăng ký thành công");
    }

    public Response handleLogout(Request request) throws Exception {
        String token = request.getToken();
        SessionManager.getInstance().validateToken(token);
        SessionManager.getInstance().removeSession(token);
        return Response.ok("Đã đăng xuất");
    }
}

