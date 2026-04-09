package com.auctionuet.server.network.dto;

import com.auctionuet.server.domain.enums.UserRole;

public class UserDTO {
    private final String id;
    private final String username;
    private final UserRole role;

    public UserDTO(String id,String username,UserRole role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public UserRole getRole() { return role; }
}