package com.auctionuet.client.view;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.util.Duration;

// Lớp MainController đóng vai trò xử lý logic cho giao diện MainView.fxml
public class MainController {


    // 1. CÁC BIẾN

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Button startButton;
    @FXML private Label statusLabel;
    @FXML private Label versionLabel;


    // 2. KHỞI TẠO

    public void initialize() {
        statusLabel.setText("");
    }


    // 3. CODE

    public void handleLogin() {
        System.out.println("Đang nhảy sang trang Đăng nhập...");
        SceneManager.getInstance().switchScene("/fxml/LoginView.fxml");
    }

    @FXML
    public void handleRegister() {
        System.out.println("Đang nhảy sang trang Đăng ký...");
        SceneManager.getInstance().switchScene("/fxml/RegisterView.fxml");
    }


    // 4. HIỆU ỨNG NÚT BẮT ĐẦU

    @FXML
    private void onStartClicked() {
        // Đổi text statusLabel thành thông báo thành công và set màu chữ thành Xanh (Green)
        statusLabel.setText("✅ Chào mừng đến với AuctionUET!");
        statusLabel.setTextFill(Color.GREEN);

        // Disable button (ngăn spam click)
        startButton.setDisable(true);

        // Dùng PauseTransition tạo độ trễ 2 giây
        PauseTransition pause2s = new PauseTransition(Duration.seconds(2));
        pause2s.setOnFinished(event -> {
            // Sau 2s -> Màu Cam
            statusLabel.setText("⏳ Đang kết nối server...");
            statusLabel.setTextFill(Color.ORANGE);

            // Tạo thêm PauseTransition 3 giây nữa
            PauseTransition pause3s = new PauseTransition(Duration.seconds(3));
            pause3s.setOnFinished(e -> {
                // Sau 3s tiếp -> Báo lỗi Màu Đỏ
                statusLabel.setText("❌ Chưa có server (sẽ implement ở tuần 4)");
                statusLabel.setTextFill(Color.RED);

                // Enable lại nút
                startButton.setDisable(false);
            });
            pause3s.play();
        });

        pause2s.play();
    }
}