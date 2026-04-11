package com.auctionuet.client.view;

import com.auctionuet.client.network.AuthClient;
import com.auctionuet.client.network.ServerConnection;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class DashboardController {

    @FXML
    private Label welcomeLabel;

    // Hàm này để nhận tên đăng nhập từ trang Login truyền sang
    public void setupDashboard(String username) {
        welcomeLabel.setText("Xin chào, " + username + " (BIDDER)");
    }

    @FXML
    public void handleLogout() {
        // Ném việc đăng xuất xuống tầng hầm chạy ngầm
        new Thread(() -> {
            try {
                AuthClient client = new AuthClient();
                client.logout("dummy-token"); // Gửi lệnh báo Server là t out đây
                ServerConnection.getInstance().disconnect(); // Chủ động rút ống nước

                // Quay về bờ (chuyển scene lại trang Đăng nhập trên UI Thread)
                javafx.application.Platform.runLater(() -> {
                    SceneManager.getInstance().switchScene("/fxml/LoginView.fxml");
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}