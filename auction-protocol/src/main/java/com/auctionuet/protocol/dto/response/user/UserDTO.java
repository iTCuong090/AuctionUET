package com.auctionuet.protocol.dto.response.user;

import com.auctionuet.protocol.dto.ValidatableDTO;
import com.auctionuet.protocol.enums.UserRole;

public class UserDTO implements ValidatableDTO {
    private final String id;
    private final String username;
    private final UserRole role;

    public UserDTO(String id, String username, UserRole role) {
        this.id = id;
        this.username = username;
        this.role = role;
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
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public UserRole getRole() { return role; }
}
