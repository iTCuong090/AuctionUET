package com.auctionuet.client.view;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import com.auctionuet.client.network.ServerConnection;
import com.auctionuet.client.network.AuthClient;
import com.auctionuet.client.network.protocol.Response;

public class RegisterController {
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label errorLabel;
    @FXML private javafx.scene.control.Button registerButton;

    // Reference tới MainController cha để chuyển panel
    private MainController mainController;

    /**
     * Được gọi bởi MainController sau khi load FXML,
     * để RegisterController có thể yêu cầu chuyển panel.
     */
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        roleComboBox.getItems().addAll("Bidder", "Seller");
    }

    @FXML
    public void handleRegister() {
        // Lấy dữ liệu từ màn hình
        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirm = confirmField.getText();
        String email = emailField.getText();
        // Lấy giá trị mà người dùng đã chọn trong menu thả xuống (ComboBox)
        String selectedRole = roleComboBox.getValue();
        String role;

        // Kiểm tra xem người dùng đã chọn chức vụ chưa
        if (selectedRole == null) {
            errorLabel.setText("Lỗi: Vui lòng chọn vai trò (Bidder hoặc Seller)!");
            errorLabel.setVisible(true);
            return; // Chặn lại, không cho chạy tiếp xuống tầng hầm mạng
        } else {
            // Nếu đã chọn, chuyển chữ đó thành in hoa (BIDDER hoặc SELLER) để Server chuẩn hóa dữ liệu
            role = selectedRole.toUpperCase();
        }

        // 1. Kiểm tra lởm trên máy khách
        if (username.isBlank() || password.isBlank() || email.isBlank()) {
            errorLabel.setText("Lỗi: Vui lòng điền đủ thông tin!");
            errorLabel.setVisible(true);
            return;
        }

        if (!password.equals(confirm)) {
            errorLabel.setText("Lỗi: Mật khẩu nhập lại không khớp!");
            errorLabel.setVisible(true);
            return;
        }

        // 2. Bật chế độ chờ
        errorLabel.setText("Đang gửi thông tin đăng ký...");
        errorLabel.setStyle("-fx-text-fill: blue;");
        errorLabel.setVisible(true);
        registerButton.setDisable(true); // Khóa nút Đăng ký

        // 3. Ném xuống tầng hầm cho chạy ngầm
        new Thread(() -> {
            try {
                // Mở ống nước
                ServerConnection.getInstance().connect("localhost", 8888);

                // Gọi thằng phiên dịch mang gói hàng REGISTER đi
                AuthClient client = new AuthClient();
                Response res = client.register(username, password, email, role);

                // 4. Báo cáo kết quả lên giao diện
                javafx.application.Platform.runLater(() -> {
                    if ("OK".equals(res.getStatus())) {
                        errorLabel.setText("✅ Đăng ký thành công! Đang chuyển sang Đăng nhập...");
                        errorLabel.setStyle("-fx-text-fill: green;");

                        // Tự động chuyển về Login sau 1.5 giây
                        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(1500));
                        pause.setOnFinished(e -> {
                            if (mainController != null) {
                                mainController.switchToLogin();
                            }
                        });
                        pause.play();
                    } else {
                        errorLabel.setText("❌ " + res.getMessage());
                        errorLabel.setStyle("-fx-text-fill: red;");
                    }
                    registerButton.setDisable(false);
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    errorLabel.setText("❌ Không thể kết nối Server!");
                    errorLabel.setStyle("-fx-text-fill: red;");
                    registerButton.setDisable(false);
                    e.printStackTrace();
                });
            }
        }).start();
    }

    /**
     * Chuyển panel phải sang LoginView (không đổi Scene).
     */
    @FXML
    public void handleSwitchToLogin() {
        if (mainController != null) {
            mainController.switchToLogin();
        }
    }

    @FXML
    public void handleBack() {
        SceneManager.getInstance().switchScene("/fxml/MainView.fxml");
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
}