package com.auctionuet.server.domain.model;
import com.auctionuet.server.domain.enums.UserRole;

public abstract class User {
    private final String username;
    private final String id;
    private final UserRole role;

    protected User(String id, String username, UserRole role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public UserRole getRole() { return role; }

    public abstract boolean hasPermission(String action);
    public abstract String getDisplayInfo();
}