package com.auctionuet.client.view;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class RegisterController {
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {
        roleComboBox.getItems().addAll("Bidder", "Seller");
    }

    @FXML
    public void handleRegister() {
        // Tuần 2 chỉ giả vờ kiểm tra đầu vào, chưa có Server
        if (usernameField.getText().length() < 3) {
            showError("Lỗi: Username phải có ít nhất 3 ký tự");
        } else {
            errorLabel.setVisible(false);
            System.out.println("Đăng ký thành công giả vờ!");
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