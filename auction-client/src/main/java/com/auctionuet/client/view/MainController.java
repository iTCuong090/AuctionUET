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

    // 4. CHUYỂN TRANG ĐĂNG NHẬP

    @FXML
    private void onStartClicked() {
        System.out.println("Đang nhảy sang trang Đăng nhập...");

        // Mượn luôn con đường cao tốc SceneManager để chuyển trang
        SceneManager.getInstance().switchScene("/fxml/LoginView.fxml");
    }
}