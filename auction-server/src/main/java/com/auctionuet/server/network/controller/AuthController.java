package com.auctionuet.server.network.controller;

import com.auctionuet.protocol.enums.UserRole;
import com.auctionuet.server.domain.service.AuthService;
import com.auctionuet.server.domain.manager.SessionManager;
import com.auctionuet.protocol.dto.request.auth.LoginRequestDTO;
import com.auctionuet.protocol.dto.request.auth.RegisterRequestDTO;
import com.auctionuet.protocol.dto.response.auth.LoginResponseDTO;
import com.auctionuet.protocol.Request;
import com.auctionuet.protocol.Response;

public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public Response handleLogin(Request request) throws Exception {
        LoginRequestDTO req = request.getDataAs(LoginRequestDTO.class);
        String username = req.getUsername();

        LoginResponseDTO responseDTO = authService.login(username, req.getPassword());

        return Response.ok(responseDTO);
    }

    public Response handleRegister(Request request) throws Exception {
        RegisterRequestDTO req = request.getDataAs(RegisterRequestDTO.class);
        String username = req.getUsername();
        String email = req.getEmail();
        String roleStr = req.getRole();
        UserRole role = (roleStr != null) ? UserRole.valueOf(roleStr) : UserRole.BIDDER;

        authService.register(username, req.getPassword(), email, role);

        return Response.ok("Đăng ký thành công");
    }

    public Response handleLogout(Request request) throws Exception {
        String token = request.getToken();

        SessionManager.getInstance().validateToken(token);
        SessionManager.getInstance().removeSession(token);

        return Response.ok("Đã đăng xuất");
    }
}
