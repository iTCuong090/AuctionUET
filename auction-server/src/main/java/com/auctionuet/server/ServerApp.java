package com.auctionuet.server;

public class ServerApp {
    
    public static final String APP_NAME = "AuctionUET Server";
    public static final String VERSION = "1.0-SNAPSHOT";
    
    public static String getWelcomeMessage() {
        return APP_NAME + " v" + VERSION + " started successfully!";
    }
    
    public static void main(String[] args) {
        System.out.println(getWelcomeMessage());
        System.out.println("Listening on port 8888...");
        // Server sẽ được implement ở tuần 4
    }
}
