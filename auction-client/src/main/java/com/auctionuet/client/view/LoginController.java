package com.auctionuet.client.view;

import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.network.AuthClient;
import com.auctionuet.client.network.ServerConnection;
import com.auctionuet.protocol.dto.response.auth.LoginResponseDTO;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField serverHostField;
    @FXML private TextField serverPortField;
    @FXML private Label errorLabel;
    @FXML private javafx.scene.control.Button loginButton;

    private MainController mainController;

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

        String host = serverHostField.getText().isBlank() ? "localhost" : serverHostField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(serverPortField.getText().trim());
        } catch (NumberFormatException e) {
            errorLabel.setText("Lỗi: Port phải là số!");
            errorLabel.setVisible(true);
            return;
        }

        errorLabel.setText("Đang kết nối Server...");
        errorLabel.setStyle("-fx-text-fill: blue;");
        errorLabel.setVisible(true);
        loginButton.setDisable(true);

        int finalPort = port;
        new Thread(() -> {
            try {
                ServerConnection.getInstance().connect(host, finalPort);
                AuthClient client = new AuthClient();
                LoginResponseDTO loginResponse = client.login(username, password);

                javafx.application.Platform.runLater(() -> {
                    ClientSession.getInstance().login(loginResponse.getToken(), loginResponse.getUser());
                    SceneManager.getInstance().switchScene("/fxml/DashboardView.fxml");
                    loginButton.setDisable(false);
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    errorLabel.setText("Lỗi đăng nhập: " + e.getMessage());
                    errorLabel.setStyle("-fx-text-fill: red;");
                    loginButton.setDisable(false);
                    e.printStackTrace();
                });
            }
        }).start();
    }

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
