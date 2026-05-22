package com.auctionuet.protocol.dto.request.item;

import com.auctionuet.protocol.dto.ValidatableDTO;

public class ItemIdRequestDTO implements ValidatableDTO {
    private String itemId;

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    @Override
    public void validate() {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId must not be blank");
        }
    }
}
