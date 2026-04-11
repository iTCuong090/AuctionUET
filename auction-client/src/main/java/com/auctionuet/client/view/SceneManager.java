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
            Scene scene = new Scene(root);

            // Nhúng CSS (Y hệt code cũ của ông, đảm bảo đéo bao giờ mất màu)
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            // --- BẮT ĐẦU VÁ LỖI NHẢY MÀN HÌNH ---
            // 1. Ghi nhớ lại trạng thái hiện tại của cửa sổ (đang full màn hay kích thước bao nhiêu)
            boolean isMaximized = primaryStage.isMaximized();
            double width = primaryStage.getWidth();
            double height = primaryStage.getHeight();

            // 2. Lắp Scene mới vào (Lúc này nó định thu nhỏ lại...)
            primaryStage.setScene(scene);

            // 3. ...Nhưng mình ép nó giữ nguyên form cũ ngay lập tức!
            if (isMaximized) {
                primaryStage.setMaximized(true); // Nếu trước đó full màn, ép full màn tiếp
            } else if (!Double.isNaN(width) && !Double.isNaN(height)) {
                primaryStage.setWidth(width);    // Nếu không thì giữ nguyên chiều rộng
                primaryStage.setHeight(height);  // Giữ nguyên chiều cao
            }
            // --- KẾT THÚC VÁ LỖI ---

            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Lỗi đéo tìm thấy file FXML: " + fxmlPath);
        }
    }
}