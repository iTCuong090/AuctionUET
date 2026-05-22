package com.auctionuet.protocol.dto.response.auction;

import com.auctionuet.protocol.dto.ValidatableDTO;
import com.auctionuet.protocol.dto.response.bid.BidDTO;
import com.auctionuet.protocol.dto.response.item.ItemDTO;
import com.auctionuet.protocol.dto.response.user.UserDTO;
import com.auctionuet.protocol.enums.AuctionStatus;

import java.time.LocalDateTime;

public class AuctionDTO implements ValidatableDTO {
    private final String id;
    private final ItemDTO item;
    private final UserDTO seller;
    private final UserDTO winner;
    private final BidDTO currentHighestBid;
    private final double currentPrice;
    private final String title;
    private final String description;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final LocalDateTime paymentDeadlineAt;
    private final AuctionStatus status;
    private final int antiSnipingWindowSeconds;
    private final int antiSnipingExtensionSeconds;
    private final double depositAmount;
    private final boolean currentUserDeposited;

    public AuctionDTO(
            String id,
            ItemDTO item,
            UserDTO seller,
            UserDTO winner,
            BidDTO currentHighestBid,
            double currentPrice,
            String title,
            String description,
            LocalDateTime startTime,
            LocalDateTime endTime,
            AuctionStatus status,
            int antiSnipingWindowSeconds,
            int antiSnipingExtensionSeconds) {
        this(id, item, seller, winner, currentHighestBid, currentPrice, title, description,
                startTime, endTime, null, status, antiSnipingWindowSeconds, antiSnipingExtensionSeconds,
                item != null ? item.getStartingPrice() * 0.10 : 0,
                false);
    }

    public AuctionDTO(
            String id,
            ItemDTO item,
            UserDTO seller,
            UserDTO winner,
            BidDTO currentHighestBid,
            double currentPrice,
            String title,
            String description,
            LocalDateTime startTime,
            LocalDateTime endTime,
            AuctionStatus status,
            int antiSnipingWindowSeconds,
            int antiSnipingExtensionSeconds,
            double depositAmount,
            boolean currentUserDeposited) {
        this(id, item, seller, winner, currentHighestBid, currentPrice, title, description,
                startTime, endTime, null, status, antiSnipingWindowSeconds, antiSnipingExtensionSeconds,
                depositAmount, currentUserDeposited);
    }

    public AuctionDTO(
            String id,
            ItemDTO item,
            UserDTO seller,
            UserDTO winner,
            BidDTO currentHighestBid,
            double currentPrice,
            String title,
            String description,
            LocalDateTime startTime,
            LocalDateTime endTime,
            LocalDateTime paymentDeadlineAt,
            AuctionStatus status,
            int antiSnipingWindowSeconds,
            int antiSnipingExtensionSeconds,
            double depositAmount,
            boolean currentUserDeposited) {
        this.id = id;
        this.item = item;
        this.seller = seller;
        this.winner = winner;
        this.currentHighestBid = currentHighestBid;
        this.currentPrice = currentPrice;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.paymentDeadlineAt = paymentDeadlineAt;
        this.status = status;
        this.antiSnipingWindowSeconds = antiSnipingWindowSeconds;
        this.antiSnipingExtensionSeconds = antiSnipingExtensionSeconds;
        this.depositAmount = depositAmount;
        this.currentUserDeposited = currentUserDeposited;
        validate();
    }

    @Override
    public void validate() {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (item == null) {
            throw new IllegalArgumentException("item must not be null");
        }
        if (seller == null) {
            throw new IllegalArgumentException("seller must not be null");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("startTime and endTime must not be null");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (currentPrice < 0) {
            throw new IllegalArgumentException("currentPrice must be greater than or equal to 0");
        }
        if (antiSnipingWindowSeconds <= 0) {
            throw new IllegalArgumentException("antiSnipingWindowSeconds must be greater than 0");
        }
        if (antiSnipingExtensionSeconds <= 0) {
            throw new IllegalArgumentException("antiSnipingExtensionSeconds must be greater than 0");
        }
        if (depositAmount < 0) {
            throw new IllegalArgumentException("depositAmount must be greater than or equal to 0");
        }
    }

    public String getId() { return id; }
    public ItemDTO getItem() { return item; }
    public UserDTO getSeller() { return seller; }
    public UserDTO getWinner() { return winner; }
    public BidDTO getCurrentHighestBid() { return currentHighestBid; }
    public double getCurrentPrice() { return currentPrice; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public LocalDateTime getPaymentDeadlineAt() { return paymentDeadlineAt; }
    public AuctionStatus getStatus() { return status; }
    public int getAntiSnipingWindowSeconds() { return antiSnipingWindowSeconds; }
    public int getAntiSnipingExtensionSeconds() { return antiSnipingExtensionSeconds; }
    public double getDepositAmount() { return depositAmount; }
    public boolean isCurrentUserDeposited() { return currentUserDeposited; }
}
