package com.auctionuet.protocol.dto.request.auth;

import com.auctionuet.protocol.dto.ValidatableDTO;

public class RegisterRequestDTO implements ValidatableDTO {
    private String username;
    private String password;
    private String email;
    private String role;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    @Override
    public void validate() {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("password must not be blank");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        if (!email.contains("@")) {
            throw new IllegalArgumentException("email is invalid");
        }
        if (role != null && role.isBlank()) {
            throw new IllegalArgumentException("role must not be blank when provided");
        }
    }
}
