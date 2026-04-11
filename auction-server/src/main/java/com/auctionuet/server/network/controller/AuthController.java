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

import java.util.Map;

public class AuthController {
    AuthService authService;
    public AuthController(AuthService authService){
        this.authService=authService;
    }
    public Response handleLogin(Request request){
        try{ String username = (String) request.getData().get("username");
        String password = (String) request.getData().get("password");
        LoginResult result = authService.login(username, password);
        UserDTO dto = UserMapper.toDTO(result.getUser());
        Map<String, Object> responseData = Map.of("token", result.getToken(), "user", dto);
        return Response.ok(responseData);}
        catch(UserNotFoundException e){
            return Response.error("User không tồn tại");
        }
        catch (AuthenticationException e){
            return Response.error("Sai mật khẩu");
        }
        catch (Exception e){
            return Response.error("Lỗi Server:"+e.getMessage());
        }
    }
    public Response handleRegister(Request request) {
        try {
            Map<String, Object> data = request.getData();
            String username = request.getDataString("username");
            String password = request.getDataString("password");
            String email = request.getDataString("email");
            String roleStr = request.getDataString("role");
            UserRole role = UserRole.valueOf(roleStr);
            authService.register(username, password, email, role);
            return Response.ok("Đăng ký thành công");
        }
        catch (DuplicateUserException e){
            return Response.error("Username đã tồn tại");
        }
        catch (IllegalArgumentException e){
            return Response.error("Dữ liệu không hợp lệ:"+e.getMessage());
        }
        catch (Exception e){
            return Response.error("Lỗi server:"+e.getMessage());
        }
    }
    public Response handleLogout(Request request){
        try{
            String token = request.getToken();
            SessionManager.getInstance().validateToken(token);
            SessionManager.getInstance().removeSession(token);
            return Response.ok("Đã đăng xuất");
        }
        catch (AuthenticationException e){
            return Response.error("Token không hợp lệ");
        }

    }

}

