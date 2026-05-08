package com.auctionuet.client.model;

import com.auctionuet.client.network.protocol.UserRole;

public class UserDTO {

    private String id;
    private String username;
    private UserRole role;

    // Bắt buộc phải có Constructor rỗng cho Gson
    public UserDTO() {}

    public UserDTO(String id, String username, UserRole role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }

    // --- Getters & Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
}