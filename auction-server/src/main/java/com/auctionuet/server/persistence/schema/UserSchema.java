package com.auctionuet.server.persistence.schema;

import com.auctionuet.server.domain.enums.UserRole;
import java.time.LocalDateTime;

public class UserSchema {
    private final String id;
    private final String username;
    private final UserRole role;
    private final String salt;
    private final String hashedPassword;
    private final String email;
    public LocalDateTime creatTimeNow;
    public LocalDateTime updateTimeNow;

    public UserSchema(String id, LocalDateTime creatTimeNow, LocalDateTime updateTimeNow, String username,
                      String hashedPassword, String salt, String email, UserRole role) {
        this.id = id;
        this.creatTimeNow = creatTimeNow;    // creatTime = UpdateTime = now
        this.updateTimeNow = updateTimeNow;
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.salt = salt;
        this.email = email;
        this.role = role;
    }

    public String getUsername() {
        return this.username;
    }
    public String getId() {
        return this.id;
    }
    public UserRole getRole() {
        return role;
    }
}

