package com.auctionuet.server.IntegrationTest;

import com.auctionuet.protocol.Response;
import com.auctionuet.protocol.util.NetworkGson;
import com.auctionuet.server.network.server.AuctionServer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TestHelper {
    private static AuctionServer runningServer;

    public static void startTestServer(int port) {
        new Thread(() -> {
            try {
                runningServer = new AuctionServer(port);
                runningServer.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void stopTestServer() {
        if (runningServer != null) {
            runningServer.stop();
            runningServer = null;
        }
    }

    public static Response sendRawRequest(int port, String json) throws Exception {
        try (Socket socket = new Socket("localhost", port)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(json);
            String response = in.readLine();
            return NetworkGson.create().fromJson(response, Response.class);
        }
    }

    public static void cleanTestData() {
        java.io.File file = new java.io.File("data/users.json");
        if (file.exists() && !file.delete()) {
            System.err.println("Khong the xoa file test users.json.");
        }
    }
}
