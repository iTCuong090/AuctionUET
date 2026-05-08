package com.auctionuet.server.network.server;

import com.auctionuet.server.exception.AuctionException;
import com.auctionuet.server.exception.AuthenticationException;
import com.auctionuet.server.exception.DuplicateUserException;
import com.auctionuet.server.exception.UserNotFoundException;
import com.auctionuet.server.network.protocol.Response;

import java.time.format.DateTimeParseException;

public class GlobalExceptionHandler {

    public static Response handle(Exception e) {
        if (e instanceof AuthenticationException) {
            return Response.error(e.getMessage() != null && !e.getMessage().isEmpty() ? e.getMessage() : "Token không hợp lệ hoặc sai thông tin đăng nhập");
        }
        if (e instanceof UserNotFoundException) {
            return Response.error("User không tồn tại");
        }
        if (e instanceof DuplicateUserException) {
            return Response.error("Username đã tồn tại");
        }
        if (e instanceof AuctionException) {
            return Response.error(e.getMessage());
        }
        if (e instanceof IllegalArgumentException) {
            return Response.error("Dữ liệu không hợp lệ: " + e.getMessage());
        }
        if (e instanceof DateTimeParseException) {
            return Response.error("Định dạng thời gian không hợp lệ");
        }

        // Lỗi hệ thống không lường trước
        e.printStackTrace(); // Log ra console ở Server
        return Response.error("Lỗi Server: " + e.getMessage());
    }
}
