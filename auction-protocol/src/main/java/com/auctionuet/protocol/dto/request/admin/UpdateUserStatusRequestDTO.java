package com.auctionuet.protocol.dto.request.admin;

import com.auctionuet.protocol.dto.ValidatableDTO;
import com.auctionuet.protocol.enums.AccountStatus;

public class UpdateUserStatusRequestDTO implements ValidatableDTO {
    private String userId;
    private AccountStatus status;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    @Override
    public void validate() {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
    }
}
