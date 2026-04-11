package com.auctionuet.client.view;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    public void handleLogin() {
        if (usernameField.getText().isBlank() || passwordField.getText().isBlank()) {
            errorLabel.setText("Lỗi: Không được để trống!");
            errorLabel.setVisible(true);
            return;
        }
        errorLabel.setVisible(false);
        System.out.println("Gửi yêu cầu đăng nhập: " + usernameField.getText());
    }

    @FXML
    public void handleBack() {
        SceneManager.getInstance().switchScene("/fxml/MainView.fxml");
    }
}