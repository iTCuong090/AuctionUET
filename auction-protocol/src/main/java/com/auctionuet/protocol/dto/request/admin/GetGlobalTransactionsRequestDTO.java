package com.auctionuet.protocol.dto.request.admin;

import com.auctionuet.protocol.dto.ValidatableDTO;
import com.auctionuet.protocol.enums.TransactionType;

public class GetGlobalTransactionsRequestDTO implements ValidatableDTO {
    private String userId;
    private TransactionType type;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    @Override
    public void validate() {
        if (userId != null) {
            userId = userId.trim();
            if (userId.isEmpty()) {
                userId = null;
            }
        }
    }
}
