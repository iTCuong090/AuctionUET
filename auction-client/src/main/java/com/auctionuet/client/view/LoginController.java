package com.auctionuet.client.view;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import com.auctionuet.client.network.ServerConnection;
import com.auctionuet.client.network.AuthClient;
import com.auctionuet.client.network.protocol.Response;
import com.auctionuet.client.model.ClientSession;

// Import thư viện bóc tách JSON
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

        // 2. Bật chế độ chờ
        errorLabel.setText("Đang kết nối Server...");
        errorLabel.setStyle("-fx-text-fill: blue;");
        errorLabel.setVisible(true);
        loginButton.setDisable(true);

        // 3. Ném việc cho Background Thread
        new Thread(() -> {
            try {
                // Mở ống nước
                ServerConnection.getInstance().connect("localhost", 8888);

                // Gọi AuthClient mang hàng đi
                AuthClient client = new AuthClient();
                Response res = client.login(username, password);

                // 4. Nhận kết quả và đẩy lên giao diện
                javafx.application.Platform.runLater(() -> {
                    if ("OK".equals(res.getStatus())) {
                        try {
                            // Lấy cục data Server trả về
                            String dataStr = res.getData().toString();

                            // ==========================================
                            // 5. CỖ MÁY BÓC TÁCH DỮ LIỆU THÔNG MINH
                            // ==========================================

                            // Nếu là FakeServer (Nó chỉ nhả ra chuỗi "token-gia-vo-day" chứ không phải ngoặc nhọn JSON)
                            if (!dataStr.trim().startsWith("{")) {
                                ClientSession.getInstance().setToken(dataStr);
                                ClientSession.getInstance().setUsername(username); // Lấy tên ông vừa nhập vào ô text
                                ClientSession.getInstance().setRole("SELLER");     // Ép cứng role để test UI
                                System.out.println("⚠️ CẢNH BÁO: Đang dùng FakeServer. Tự động mock data cho UI!");
                            }
                            // Nếu là Server Thật (Trả về JSON chuẩn có ngoặc nhọn { })
                            else {
                                JsonObject dataObj = JsonParser.parseString(dataStr).getAsJsonObject();

                                ClientSession.getInstance().setToken(dataObj.get("token").getAsString());
                                ClientSession.getInstance().setUsername(dataObj.get("username").getAsString());
                                ClientSession.getInstance().setRole(dataObj.get("role").getAsString());

                                System.out.println("✅ Server Thật - Đăng nhập thành công! User: " + ClientSession.getInstance().getUsername());
                            }

                            // Chuyển sang màn hình Dashboard
                            SceneManager.getInstance().switchScene("/fxml/DashboardView.fxml");

                        } catch (Exception ex) {
                            System.out.println("❌ Lỗi trong quá trình bóc tách JSON hoặc chuyển trang!");
                            ex.printStackTrace();
                            errorLabel.setText("❌ Lỗi xử lý dữ liệu từ Server!");
                            errorLabel.setStyle("-fx-text-fill: red;");
                        }

                    } else {
                        // Trả về ERROR thì hiện lời chửi của Server
                        errorLabel.setText("❌ " + res.getMessage());
                        errorLabel.setStyle("-fx-text-fill: red;");
                    }
                    loginButton.setDisable(false);
                });

            } catch (Exception e) {
                // Nếu Server chưa bật
                javafx.application.Platform.runLater(() -> {
                    errorLabel.setText("❌ Không tìm thấy Server. Hãy kiểm tra lại kết nối!");
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