package com.auctionuet.client.view;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import com.auctionuet.client.network.ServerConnection;
import com.auctionuet.client.network.AuthClient;
import com.auctionuet.client.network.protocol.Response;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private javafx.scene.control.Button loginButton;

    @FXML
    public void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        // 1. Kiểm tra lởm trên máy khách
        if (username.isBlank() || password.isBlank()) {
            errorLabel.setText("Lỗi: Không được để trống!");
            errorLabel.setVisible(true);
            return;
        }

        // 2. Bật chế độ chờ (Tránh việc khách bấm nút 2 lần liên tiếp)
        errorLabel.setText("Đang kết nối Server...");
        errorLabel.setStyle("-fx-text-fill: blue;"); // Đổi màu xanh cho có hi vọng
        errorLabel.setVisible(true);
        loginButton.setDisable(true); // Khóa mẹ nút Đăng nhập lại

        // 3. Vứt việc nặng nhọc cho thằng lính đánh thuê (Background Thread)
        new Thread(() -> {
            try {
                // Mở ống nước
                ServerConnection.getInstance().connect("localhost", 8888);

                // Gọi thằng phiên dịch mang gói hàng đi
                AuthClient client = new AuthClient();
                Response res = client.login(username, password);

                // 4. Nhận được hàng về, nhờ Cô Lễ Tân (Platform.runLater) báo cáo lên màn hình
                javafx.application.Platform.runLater(() -> {
                    if ("OK".equals(res.getStatus())) {
                        System.out.println("Đăng nhập thành công!");

                        // Chuyển sang màn hình Dashboard
                        SceneManager.getInstance().switchScene("/fxml/DashboardView.fxml");

                    } else {
                        // Trả về ERROR thì hiện lời chửi của Server
                        errorLabel.setText("❌ " + res.getMessage());
                        errorLabel.setStyle("-fx-text-fill: red;");
                    }
                    loginButton.setDisable(false);
                });

            } catch (Exception e) {
                // Nếu Server chưa bật, nó văng Exception vào đây
                javafx.application.Platform.runLater(() -> {
                    errorLabel.setText("❌ Không tìm thấy Server. Thằng Khánh chưa bật!");
                    errorLabel.setStyle("-fx-text-fill: red;");
                    loginButton.setDisable(false);
                    e.printStackTrace();
                });
            }
        }).start();
    }

    @FXML
    public void handleBack() {
        SceneManager.getInstance().switchScene("/fxml/MainView.fxml");
    }
}