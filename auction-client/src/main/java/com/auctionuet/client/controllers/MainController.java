package com.auctionuet.client.controllers;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.util.Duration;

// Lớp MainController đóng vai trò xử lý logic cho giao diện MainView.fxml
public class MainController {

    // 1. Khai báo các @FXML fields tương ứng với fx:id trong FXML
    // Annotation @FXML giúp JavaFX tự động "tiêm" (inject) các thành phần giao diện
    // từ file FXML vào các biến này. Bắt buộc tên biến phải giống 100% với fx:id.
    @FXML
    private Label titleLabel;

    @FXML
    private Label subtitleLabel;

    @FXML
    private Button startButton;

    @FXML
    private Label statusLabel;

    @FXML
    private Label versionLabel;

    // 2. Method initialize() — tự động gọi sau khi FXML load xong
    // Annotation @FXML ở đây báo cho FXMLLoader biết đây là hàm khởi tạo.
    // Tại sao cần initialize? Để thiết lập các giá trị mặc định ngay khi app vừa bật lên
    // mà không cần đợi người dùng thao tác. Ở đây ta cần xóa trắng dòng thông báo.
    @FXML
    public void initialize() {
        statusLabel.setText("");
    }

    // 3. Handler cho nút Start
    // Hàm này được liên kết với thuộc tính onAction="#onStartClicked" của nút Bắt đầu trong FXML.
    @FXML
    private void onStartClicked() {
        // - Đổi text statusLabel thành thông báo thành công và set màu chữ thành Xanh (Green)
        statusLabel.setText("✅ Chào mừng đến với AuctionUET!");
        statusLabel.setTextFill(Color.GREEN);

        // - Disable button (vì đã click rồi, ngăn người dùng bấm spam nhiều lần)
        startButton.setDisable(true);

        // - Dùng PauseTransition để tạo độ trễ 2 giây mà không làm đơ giao diện ứng dụng
        PauseTransition pause2s = new PauseTransition(Duration.seconds(2));
        pause2s.setOnFinished(event -> {
            // Sau khi hết 2 giây đầu tiên, cập nhật lại trạng thái thành đang kết nối (Màu Cam)
            statusLabel.setText("⏳ Đang kết nối server...");
            statusLabel.setTextFill(Color.ORANGE);

            // - Tiếp tục tạo thêm một PauseTransition 3 giây nữa (để tổng cộng là 5 giây từ lúc bấm)
            PauseTransition pause3s = new PauseTransition(Duration.seconds(3));
            pause3s.setOnFinished(e -> {
                // Sau khi hết 3 giây tiếp theo, báo lỗi không có server (Màu Đỏ)
                statusLabel.setText("❌ Chưa có server (sẽ implement ở tuần 4)");
                statusLabel.setTextFill(Color.RED);

                // Enable lại nút Bắt đầu để người dùng có thể click lại nếu muốn
                startButton.setDisable(false);
            });
            // Bắt đầu chạy bộ đếm 3 giây
            pause3s.play();
        });

        // Bắt đầu chạy bộ đếm 2 giây
        pause2s.play();
    }
}