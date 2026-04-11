package com.auctionuet.server;

import com.auctionuet.server.network.server.AuctionServer;
public class ServerApp {
    
    public static final String APP_NAME = "AuctionUET Server";
    public static final String VERSION = "1.0-SNAPSHOT";
    
    public static String getWelcomeMessage() {
        return APP_NAME + " v" + VERSION + " started successfully!";
    }
    
    public static void main(String[] args) {
        System.out.println(getWelcomeMessage());
        
        int port = 8888;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number provided. Using default: 8888");
            }
        }
        
        AuctionServer server = new AuctionServer(port);
        server.start();
    }
}
