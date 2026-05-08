package com.auctionuet.client.view;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import com.auctionuet.client.network.ServerConnection;
import com.auctionuet.client.network.AuthClient;
import com.auctionuet.client.network.protocol.Response;
import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.model.UserDTO;

// Import thư viện bóc tách JSON
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private javafx.scene.control.Button loginButton;

    // Reference tới MainController cha để chuyển panel
    private MainController mainController;

    /**
     * Được gọi bởi MainController sau khi load FXML,
     * để LoginController có thể yêu cầu chuyển panel.
     */
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isBlank() || password.isBlank()) {
            errorLabel.setText("Lỗi: Không được để trống!");
            errorLabel.setVisible(true);
            return;
        }

        errorLabel.setText("Đang kết nối Server...");
        errorLabel.setStyle("-fx-text-fill: blue;");
        errorLabel.setVisible(true);
        loginButton.setDisable(true);

        new Thread(() -> {
            try {
                ServerConnection.getInstance().connect("localhost", 8888);
                AuthClient client = new AuthClient();
                Response res = client.login(username, password);

                javafx.application.Platform.runLater(() -> {
                    if ("OK".equals(res.getStatus())) {
                        try {
                            Gson gson = new Gson();
                            String dataStr = gson.toJson(res.getData());

                            // NẾU DÙNG MOCK DATA (Server chưa code xong)
                            if (!dataStr.trim().startsWith("{")) {
                                // Tự nặn ra một cái JSON ảo có role SELLER để qua cửa
                                String mockJson = "{\"username\":\"" + username + "\", \"role\":\"SELLER\"}";
                                UserDTO mockUser = gson.fromJson(mockJson, UserDTO.class);

                                ClientSession.getInstance().login(dataStr, mockUser);
                                System.out.println("⚠️ CẢNH BÁO: Đang dùng FakeServer. Tự động mock data!");
                            }
                            // NẾU LÀ SERVER THẬT
                            else {
                                JsonObject dataObj = JsonParser.parseString(dataStr).getAsJsonObject();
                                String exactToken = dataObj.get("token").getAsString();

                                // Lấy object "user" từ dataObj rồi map qua UserDTO
                                UserDTO loggedInUser = gson.fromJson(dataObj.get("user"), UserDTO.class);

                                ClientSession.getInstance().login(exactToken, loggedInUser);
                                System.out.println("✅ Server Thật - Đăng nhập thành công! Role: " + loggedInUser.getRole());
                            }

                            // CHUYỂN TOÀN BỘ SCENE SANG DASHBOARD
                            SceneManager.getInstance().switchScene("/fxml/DashboardView.fxml");

                        } catch (Exception ex) {
                            System.out.println("❌ Lỗi trong quá trình bóc tách JSON hoặc chuyển trang!");
                            ex.printStackTrace();
                            errorLabel.setText("❌ Lỗi xử lý dữ liệu từ Server!");
                            errorLabel.setStyle("-fx-text-fill: red;");
                        }

                    } else {
                        errorLabel.setText("❌ " + res.getMessage());
                        errorLabel.setStyle("-fx-text-fill: red;");
                    }
                    loginButton.setDisable(false);
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    errorLabel.setText("❌ Không tìm thấy Server. Hãy kiểm tra lại kết nối!");
                    errorLabel.setStyle("-fx-text-fill: red;");
                    loginButton.setDisable(false);
                    e.printStackTrace();
                });
            }
        }).start();
    }

    /**
     * Chuyển panel phải sang RegisterView (không đổi Scene).
     */
    @FXML
    public void handleSwitchToRegister() {
        if (mainController != null) {
            mainController.switchToRegister();
        }
    }

    @FXML
    public void handleBack() {
        SceneManager.getInstance().switchScene("/fxml/MainView.fxml");
    }
}