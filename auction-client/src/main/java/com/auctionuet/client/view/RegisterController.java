package com.auctionuet.client.view;

import com.auctionuet.client.network.AuthClient;
import com.auctionuet.client.network.ServerConnection;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private TextField serverHostField;
    @FXML private TextField serverPortField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label errorLabel;
    @FXML private javafx.scene.control.Button registerButton;

    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        roleComboBox.getItems().addAll("Bidder", "Seller");
    }

    @FXML
    public void handleRegister() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirm = confirmField.getText();
        String email = emailField.getText();
        String selectedRole = roleComboBox.getValue();

        if (selectedRole == null) {
            showError("Lỗi: Vui lòng chọn vai trò.");
            return;
        }

        if (username.isBlank() || password.isBlank() || email.isBlank()) {
            showError("Lỗi: Vui lòng điền đủ thông tin!");
            return;
        }

        if (!password.equals(confirm)) {
            showError("Lỗi: Mật khẩu nhập lại không khớp!");
            return;
        }

        String host = serverHostField.getText().isBlank() ? "localhost" : serverHostField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(serverPortField.getText().trim());
        } catch (NumberFormatException e) {
            showError("Lỗi: Port phải là số!");
            return;
        }

        String role = selectedRole.toUpperCase();
        errorLabel.setText("Đang kết nối Server...");
        errorLabel.setStyle("-fx-text-fill: blue;");
        errorLabel.setVisible(true);
        registerButton.setDisable(true);

        int finalPort = port;
        new Thread(() -> {
            try {
                ServerConnection.getInstance().connect(host, finalPort);
                AuthClient client = new AuthClient();
                client.register(username, password, email, role);

                javafx.application.Platform.runLater(() -> {
                    errorLabel.setText("Đăng ký thành công! Đang chuyển sang Đăng nhập...");
                    errorLabel.setStyle("-fx-text-fill: green;");

                    javafx.animation.PauseTransition pause =
                            new javafx.animation.PauseTransition(javafx.util.Duration.millis(1500));
                    pause.setOnFinished(e -> {
                        if (mainController != null) {
                            mainController.switchToLogin();
                        }
                    });
                    pause.play();
                    registerButton.setDisable(false);
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    errorLabel.setText("Lỗi đăng ký: " + e.getMessage());
                    errorLabel.setStyle("-fx-text-fill: red;");
                    registerButton.setDisable(false);
                    e.printStackTrace();
                });
            }
        }).start();
    }

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
