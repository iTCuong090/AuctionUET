package com.auctionuet.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class ClientApp extends Application {


    public static final String APP_NAME = "AuctionUET Client";

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Đập bỏ UI code chay, thay bằng FXMLLoader của mày
        URL fxmlLocation = getClass().getResource("/com/auctionuet/client/views/MainView.fxml");
        if (fxmlLocation == null) {
            System.err.println("Lỗi: Không tìm thấy file MainView.fxml");
            System.exit(1);
        }

        FXMLLoader loader = new FXMLLoader(fxmlLocation);
        Parent root = loader.load();

        Scene scene = new Scene(root, 900, 600);

        primaryStage.setTitle(APP_NAME);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}