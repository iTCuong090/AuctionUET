package com.auctionuet.client.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class SceneManager {
    private static SceneManager instance;
    private Stage primaryStage;

    private SceneManager() {}

    public static SceneManager getInstance() {
        if (instance == null) {
            instance = new SceneManager();
        }
        return instance;
    }

    public void init(Stage stage) {
        this.primaryStage = stage;
    }

    public void switchScene(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            
            if (primaryStage.getScene() == null) {
                // Nếu chưa có Scene nào (lần đầu), tạo mới
                Scene scene = new Scene(root);
                ThemeManager.getInstance().applyTheme(scene);
                primaryStage.setScene(scene);
            } else {
                // Nếu đã có Scene, chỉ cần thay đổi "ruột" (Root)
                // Việc này giúp giữ nguyên trạng thái cửa sổ (Maximized, Width, Height) mà không bị giật
                Scene scene = primaryStage.getScene();
                scene.setRoot(root);
                ThemeManager.getInstance().applyTheme(scene);
            }

            // --- Tự động Resize cửa sổ theo màn hình ---
            if (fxmlPath.contains("DashboardView")) {
                // Nếu vào Dashboard -> Phóng to toàn màn hình cho "đã"
                primaryStage.setMaximized(true);
            } else if (fxmlPath.contains("MainView")) {
                // Nếu quay về Đăng nhập -> Thu nhỏ lại cho đẹp
                primaryStage.setMaximized(false);
                primaryStage.setWidth(1000);
                primaryStage.setHeight(650);
                // Căn giữa lại màn hình sau khi thu nhỏ
                primaryStage.centerOnScreen();
            }

            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Lỗi đéo tìm thấy file FXML: " + fxmlPath);
        }
    }
}