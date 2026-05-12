package com.auctionuet.client.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.Parent;

/**
 * MainController - Điều khiển màn hình chính (MainView).
 * Layout chia đôi: Trái là banner branding, Phải là panel Login/Register.
 * Login/Register chuyển đổi trong cùng panel phải mà không đổi Scene.
 */
public class MainController {

    @FXML private StackPane rightPanel;
    @FXML private Button btnToggleTheme;

    @FXML
    public void initialize() {
        // Mặc định load LoginView vào panel bên phải
        loadViewIntoPanel("/fxml/LoginView.fxml");

        // Cập nhật icon toggle theme
        if (btnToggleTheme != null) {
            btnToggleTheme.setText(ThemeManager.getInstance().isDarkMode() ? "T" : "S");
        }
    }

    /**
     * Load một FXML view vào ô bên phải (rightPanel).
     * Dùng để chuyển đổi Login <-> Register mà không đổi Scene.
     */
    public void loadViewIntoPanel(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            // Truyền reference của MainController cho controller con
            Object controller = loader.getController();
            if (controller instanceof LoginController) {
                ((LoginController) controller).setMainController(this);
            } else if (controller instanceof RegisterController) {
                ((RegisterController) controller).setMainController(this);
            }

            rightPanel.getChildren().clear();
            rightPanel.getChildren().add(view);
        } catch (Exception e) {
            System.out.println("❌ Lỗi load view vào panel: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Chuyển panel phải sang RegisterView.
     */
    public void switchToRegister() {
        loadViewIntoPanel("/fxml/RegisterView.fxml");
    }

    /**
     * Chuyển panel phải sang LoginView.
     */
    public void switchToLogin() {
        loadViewIntoPanel("/fxml/LoginView.fxml");
    }

    @FXML
    private void handleToggleTheme() {
        ThemeManager.getInstance().toggleTheme();
        ThemeManager.getInstance().applyTheme(btnToggleTheme.getScene());
        btnToggleTheme.setText(ThemeManager.getInstance().isDarkMode() ? "T" : "S");
    }
}
