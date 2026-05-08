package com.auctionuet.server;

import com.auctionuet.server.network.server.AuctionServer;
import com.auctionuet.server.util.AppLogger;

public class ServerApp {
    
    public static final String APP_NAME = "AuctionUET Server";
    public static final String VERSION = "1.0-SNAPSHOT";

    /** Trả về welcome message — dùng bởi unit test. */
    public static String getWelcomeMessage() {
        return APP_NAME + " v" + VERSION + " started successfully!";
    }
    
    public static void main(String[] args) {
        int port = 8888;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number provided. Using default: 8888");
            }
        }

        // In banner khởi động với đầy đủ thông tin mode & port
        AppLogger.logBanner(APP_NAME, VERSION, port, AppLogger.LogMode.current());

        AuctionServer server = new AuctionServer(port);
        server.start();
    }
}
