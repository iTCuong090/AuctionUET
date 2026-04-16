package com.auctionuet.client.view;

import com.auctionuet.client.model.ClientSession;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

public class AuctionDetailController {

    @FXML private Label nameLabel, sellerLabel, typeLabel, specLabel, conditionLabel, descriptionLabel;
    @FXML private Label currentPriceLabel, leaderLabel, timeLeftLabel, statusLabel;
    @FXML private Button btnStartAuction;
    @FXML private ImageView productImageView;

    // Giả sử ông sẽ có một hàm để nhận dữ liệu từ màn hình danh sách truyền sang
    public void setAuctionData(String auctionId) {
        // Tuần 4: Gọi AuctionClient.getAuctionDetail(token, auctionId)
        // Hiện tại: Hiển thị Mock Data

        String currentUser = ClientSession.getInstance().getUsername();
        String sellerOfThisAuction = "seller_cuong"; // Giả định
        String currentStatus = "OPEN";

        // 1. Hiển thị thông tin (Mock)
        nameLabel.setText("📱 iPhone 15 Pro Max");
        sellerLabel.setText("Seller: " + sellerOfThisAuction);
        currentPriceLabel.setText("💰 Giá hiện tại: 25.500.000 VNĐ");
        statusLabel.setText("📊 Trạng thái: " + currentStatus);
        timeLeftLabel.setText("⏰ Thời gian còn lại: 01:45:30");

        // 2. Logic ẩn hiện nút Start
        // Nếu tôi là chủ món hàng VÀ phiên đang ở trạng thái OPEN thì mới cho bấm Start
        if (currentUser.equals(sellerOfThisAuction) && "OPEN".equals(currentStatus)) {
            btnStartAuction.setVisible(true);
            btnStartAuction.setManaged(true);
        }
    }

    @FXML
    private void handleStartAuction() {
        System.out.println("🚀 Gửi yêu cầu START_AUCTION lên server...");
        // Tuần 4: AuctionClient.startAuction(token, auctionId)

        btnStartAuction.setDisable(true);
        statusLabel.setText("📊 Trạng thái: 🟢 RUNNING");
        btnStartAuction.setText("Đã bắt đầu phiên");
    }
}