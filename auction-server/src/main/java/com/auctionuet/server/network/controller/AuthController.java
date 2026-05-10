package com.auctionuet.server.network.controller;

import com.auctionuet.server.domain.enums.UserRole;
import com.auctionuet.server.domain.service.AuthService;
import com.auctionuet.server.domain.manager.SessionManager;
import com.auctionuet.server.exception.AuthenticationException;
import com.auctionuet.server.exception.DuplicateUserException;
import com.auctionuet.server.exception.UserNotFoundException;
import com.auctionuet.server.mapper.UserMapper;
import com.auctionuet.server.network.dto.UserDTO;
import com.auctionuet.server.network.protocol.Request;
import com.auctionuet.server.network.protocol.Response;
import com.auctionuet.server.domain.service.LoginResult;
import com.auctionuet.server.util.AppLogger;

import java.util.Map;

public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public Response handleLogin(Request request) throws Exception {
        String username = (String) request.getData().get("username");
        AppLogger.logControllerEnter("AuthController", "handleLogin",
            "username=" + username);

        LoginResult result = authService.login(username, (String) request.getData().get("password"));

        UserDTO dto = UserMapper.toDTO(result.getUser());
        Map<String, Object> responseData = Map.of("token", result.getToken(), "user", dto);

        AppLogger.logControllerResult("AuthController", "handleLogin",
            "Login OK | user=" + username + " role=" + result.getUser().getRole());
        return Response.ok(responseData);
    }

    public Response handleRegister(Request request) throws Exception {
        String username = request.getDataString("username");
        String email    = request.getDataString("email");
        String roleStr  = request.getDataString("role");
        UserRole role   = (roleStr != null) ? UserRole.valueOf(roleStr) : UserRole.BIDDER;

        AppLogger.logControllerEnter("AuthController", "handleRegister",
            "username=" + username + " email=" + email + " role=" + role);

        authService.register(username, request.getDataString("password"), email, role);

        AppLogger.logControllerResult("AuthController", "handleRegister",
            "Register OK | user=" + username);
        return Response.ok("Đăng ký thành công");
    }

    public Response handleLogout(Request request) throws Exception {
        String token = request.getToken();
        AppLogger.logControllerEnter("AuthController", "handleLogout",
            "token=" + AppLogger.maskToken(token));

        SessionManager.getInstance().validateToken(token);
        SessionManager.getInstance().removeSession(token);

        AppLogger.logControllerResult("AuthController", "handleLogout", "Logout OK");
        return Response.ok("Đã đăng xuất");
    }
}
