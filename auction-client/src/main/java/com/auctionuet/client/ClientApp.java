package com.auctionuet.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import com.auctionuet.client.view.SceneManager;

import java.net.URL;

public class ClientApp extends Application {


    public static final String APP_NAME = "AuctionUET Client";

    @Override
    public void start(Stage primaryStage) throws Exception {
        SceneManager.getInstance().init(primaryStage);
        // Đập bỏ UI code chay, thay bằng FXMLLoader
        URL fxmlLocation = getClass().getResource("/fxml/MainView.fxml");
        if (fxmlLocation == null) {
            System.err.println("Lỗi: Không tìm thấy file MainView.fxml");
            System.exit(1);
        }

        FXMLLoader loader = new FXMLLoader(fxmlLocation);
        Parent root = loader.load();

        Scene scene = new Scene(root, 900, 600);
        com.auctionuet.client.view.ThemeManager.getInstance().applyTheme(scene);

        primaryStage.setTitle(APP_NAME);
        applyAppIcon(primaryStage);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void applyAppIcon(Stage stage) {
        URL iconUrl = getClass().getResource("/images/UET-circle.png");
        if (iconUrl != null) {
            stage.getIcons().setAll(new Image(iconUrl.toExternalForm()));
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
