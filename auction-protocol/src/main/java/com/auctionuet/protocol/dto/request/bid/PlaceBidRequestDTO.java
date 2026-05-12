package com.auctionuet.protocol.dto.request.bid;

import com.auctionuet.protocol.dto.ValidatableDTO;

public class PlaceBidRequestDTO implements ValidatableDTO {
    private String auctionId;
    private double amount;

    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    @Override
    public void validate() {
        if (auctionId == null || auctionId.isBlank()) {
            throw new IllegalArgumentException("auctionId must not be blank");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }
    }
}
