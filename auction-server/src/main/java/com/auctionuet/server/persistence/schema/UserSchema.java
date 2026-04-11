package com.auctionuet.server.persistence.schema;

//Khi merge với mọi người thì cần import thêm Enums Userrole.

import java.time.LocalDateTime;

public class UserSchema extends BaseSchema {
    private String username;
    private String hashedPassword;
    private String passwordSalt;
    private String email;
    private UserRole role;

    protected UserSchema() {
        super();
    }

    public UserSchema(String id, LocalDateTime createdAt, LocalDateTime updatedAt,
            String username, String hashedPassword, String passwordSalt,
            String email, UserRole role) {
        super(id, createdAt, updatedAt);
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.passwordSalt = passwordSalt;
        this.email = email;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public void setPasswordSalt(String passwordSalt) {
        this.passwordSalt = passwordSalt;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}
