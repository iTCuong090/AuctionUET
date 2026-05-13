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
            showError("Loi: Vui long chon vai tro.");
            return;
        }

        if (username.isBlank() || password.isBlank() || email.isBlank()) {
            showError("Loi: Vui long dien du thong tin!");
            return;
        }

        if (!password.equals(confirm)) {
            showError("Loi: Mat khau nhap lai khong khop!");
            return;
        }

        String role = selectedRole.toUpperCase();
        errorLabel.setText("Dang gui thong tin dang ky...");
        errorLabel.setStyle("-fx-text-fill: blue;");
        errorLabel.setVisible(true);
        registerButton.setDisable(true);

        new Thread(() -> {
            try {
                ServerConnection.getInstance().connect("localhost", 8888);
                AuthClient client = new AuthClient();
                client.register(username, password, email, role);

                javafx.application.Platform.runLater(() -> {
                    errorLabel.setText("Dang ky thanh cong! Dang chuyen sang Dang nhap...");
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
                    errorLabel.setText("Loi dang ky: " + e.getMessage());
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
