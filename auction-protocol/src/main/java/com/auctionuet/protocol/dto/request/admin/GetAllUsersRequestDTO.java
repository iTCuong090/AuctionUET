package com.auctionuet.protocol.dto.request.admin;

import com.auctionuet.protocol.dto.ValidatableDTO;
import com.auctionuet.protocol.enums.AccountStatus;
import com.auctionuet.protocol.enums.UserRole;

public class GetAllUsersRequestDTO implements ValidatableDTO {
    private String usernameQuery;
    private UserRole role;
    private AccountStatus status;

    public String getUsernameQuery() {
        return usernameQuery;
    }

    public void setUsernameQuery(String usernameQuery) {
        this.usernameQuery = usernameQuery;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    @Override
    public void validate() {
        if (usernameQuery != null) {
            usernameQuery = usernameQuery.trim();
        }
    }
}
