package com.auctionuet.protocol.dto.response.admin;

import com.auctionuet.protocol.dto.ValidatableDTO;
import com.auctionuet.protocol.enums.AccountStatus;
import com.auctionuet.protocol.enums.UserRole;

public class AdminUserDTO implements ValidatableDTO {
    private final String id;
    private final String username;
    private final UserRole role;
    private final AccountStatus status;

    public AdminUserDTO(String id, String username, UserRole role, AccountStatus status) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.status = status;
        validate();
    }

    @Override
    public void validate() {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public UserRole getRole() { return role; }
    public AccountStatus getStatus() { return status; }
}
