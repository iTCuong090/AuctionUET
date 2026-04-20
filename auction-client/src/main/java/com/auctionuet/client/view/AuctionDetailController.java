package com.auctionuet.client.view;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.model.UserDTO;
import com.auctionuet.client.model.AuctionDTO;
import com.auctionuet.client.network.AuctionClient;

public class AuctionDetailController {

    @FXML private Label nameLabel, sellerLabel, typeLabel, specLabel, conditionLabel, descriptionLabel;
    @FXML private Label currentPriceLabel, leaderLabel, timeLeftLabel, statusLabel;
    @FXML private Button btnStartAuction;
    @FXML private ImageView productImageView;

    private String currentAuctionId; // Lưu lại ID phiên đang xem

    // Hàm này sẽ được Controller khác (ví dụ AuctionList) gọi và truyền ID vào
    public void setAuctionData(String auctionId) {
        this.currentAuctionId = auctionId;
        String token = ClientSession.getInstance().getToken();

        // 1. Tạm thời ẩn nút Start đi cho chắc
        btnStartAuction.setVisible(false);
        btnStartAuction.setManaged(false);

        // 2. Gọi mạng lấy thông tin chi tiết
        new Thread(() -> {
            try {
                AuctionClient client = new AuctionClient();
                AuctionDTO dto = client.getAuctionDetail(token, auctionId);

                Platform.runLater(() -> updateUI(dto));
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("❌ Lỗi lấy chi tiết: " + e.getMessage()));
            }
        }).start();
    }

    // Hàm phụ trợ đẩy dữ liệu lên Giao diện
    private void updateUI(AuctionDTO dto) {
        nameLabel.setText(dto.getTitle());
        sellerLabel.setText("👤 Seller: " + dto.getSellerUsername());
        currentPriceLabel.setText(String.format("💰 Giá hiện tại: %,.0f VNĐ", dto.getCurrentHighestBid()));

        String currentStatus = dto.getStatus() != null ? dto.getStatus().name() : "UNKNOWN";
        statusLabel.setText("📊 Trạng thái: " + currentStatus);

        timeLeftLabel.setText("⏰ Kết thúc: " + dto.getEndTime());

        // Lấy User hiện tại để phân quyền nút bấm (ĐÃ FIX LỖI)
        UserDTO currentUser = ClientSession.getInstance().getCurrentUser();
        String currentUsername = (currentUser != null) ? currentUser.getUsername() : "";

        // Nếu tôi là chủ món hàng VÀ phiên đang ở trạng thái OPEN thì mới cho bấm Start
        if (currentUsername.equals(dto.getSellerUsername()) && "OPEN".equals(currentStatus)) {
            btnStartAuction.setVisible(true);
            btnStartAuction.setManaged(true);
        }
    }

    @FXML
    private void handleStartAuction() {
        String token = ClientSession.getInstance().getToken();

        btnStartAuction.setDisable(true);
        btnStartAuction.setText("⏳ Đang xử lý...");

        // GỌI MẠNG LỆNH BẮT ĐẦU PHIÊN (START_AUCTION)
        new Thread(() -> {
            try {
                AuctionClient client = new AuctionClient();
                client.startAuction(token, currentAuctionId);

                Platform.runLater(() -> {
                    statusLabel.setText("📊 Trạng thái: 🟢 RUNNING");
                    btnStartAuction.setText("Đã bắt đầu phiên");
                    // Tùy chọn: Ẩn nút đi sau khi bấm thành công
                    // btnStartAuction.setVisible(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("❌ Lỗi: " + e.getMessage());
                    btnStartAuction.setText("Bắt đầu phiên");
                    btnStartAuction.setDisable(false);
                });
            }
        }).start();
    }
}