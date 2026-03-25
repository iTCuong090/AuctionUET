package com.auctionuet.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class ClientApp extends Application {
    
    public static final String APP_NAME = "AuctionUET Client";
    
    @Override
    public void start(Stage primaryStage) {
        Label label = new Label("Welcome to AuctionUET!");
        label.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        
        StackPane root = new StackPane(label);
        Scene scene = new Scene(root, 800, 600);
        
        primaryStage.setTitle(APP_NAME);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
