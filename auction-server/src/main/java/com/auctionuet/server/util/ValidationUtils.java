package com.auctionuet.server.util;

import java.util.regex.*;

public class ValidationUtils {
    public static void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username phải có ít nhất 3 ký tự");
        }
        if (username.length() < 3) {
            throw new IllegalArgumentException("Username phải có ít nhất 3 ký tự");
        }
        if (!username.matches("^\\w+$")) {
            throw new IllegalArgumentException("Username chỉ được chứa chữ cái, chữ số và dấu gạch dưới");
        }
    }

    public static void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email không được để trống");
        }
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            throw new IllegalArgumentException("Email không hợp lệ");
        }
    }

    public static void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password phải có ít nhất 8 ký tự");
        }
        if (password.length()<8) {
            throw new IllegalArgumentException("Password phải có ít nhất 8 ký tự");
        }
    }
}

