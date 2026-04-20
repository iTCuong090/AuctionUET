package com.auctionuet.client.model;

// Import đúng đường dẫn Enum
import com.auctionuet.client.network.protocol.AuctionStatus;
// Không cần import ItemDTO vì nó nằm chung thư mục model rồi

public class AuctionDTO {
    private String id;
    private ItemDTO item;
    private String sellerUsername;
    private String title;
    private String description;

    // Đã đổi thành String để Client đọc JSON ngon ơ
    private String startTime;
    private String endTime;

    private AuctionStatus status;
    private double currentHighestBid;
    private String currentWinnerUsername;

    public AuctionDTO() {}

    public AuctionDTO(String id, ItemDTO item, String sellerUsername, String title,
                      String description, String startTime, String endTime,
                      AuctionStatus status, double currentHighestBid, String currentWinnerUsername) {
        this.id = id;
        this.item = item;
        this.sellerUsername = sellerUsername;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.currentHighestBid = currentHighestBid;
        this.currentWinnerUsername = currentWinnerUsername;
    }

    // --- Getters & Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public ItemDTO getItem() { return item; }
    public void setItem(ItemDTO item) { this.item = item; }

    public String getSellerUsername() { return sellerUsername; }
    public void setSellerUsername(String sellerUsername) { this.sellerUsername = sellerUsername; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }

    public double getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(double currentHighestBid) { this.currentHighestBid = currentHighestBid; }

    public String getCurrentWinnerUsername() { return currentWinnerUsername; }
    public void setCurrentWinnerUsername(String currentWinnerUsername) { this.currentWinnerUsername = currentWinnerUsername; }
}