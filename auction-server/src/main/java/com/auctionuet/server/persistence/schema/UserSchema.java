package com.auctionuet.server.persistence.schema;

import com.auctionuet.protocol.enums.UserRole;
import com.auctionuet.protocol.enums.AccountStatus;
import java.time.LocalDateTime;

public class UserSchema extends BaseSchema {
    private String username;
    private String hashedPassword;
    private String passwordSalt;
    private String email;
    private UserRole role;
    private AccountStatus status = AccountStatus.ACTIVE;
    private boolean mustChangePassword;
    private double balance = 0;
    private double frozenBalance = 0;

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

    public AccountStatus getStatus() {
        return status != null ? status : AccountStatus.ACTIVE;
    }

    public void setStatus(AccountStatus status) {
        this.status = status != null ? status : AccountStatus.ACTIVE;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public double getBalance() { return balance; }
    
    public void setBalance(double balance) { this.balance = balance; }
    
    public double getFrozenBalance() { return frozenBalance; }
    
    public void setFrozenBalance(double frozenBalance) { this.frozenBalance = frozenBalance; }
}
