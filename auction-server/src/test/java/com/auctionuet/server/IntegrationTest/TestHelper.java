package com.auctionuet.server.IntegrationTest;

import com.auctionuet.protocol.Response;
import com.auctionuet.protocol.util.NetworkGson;
import com.auctionuet.server.network.server.AuctionServer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class TestHelper {
    private static final String[] DATA_FILES = {
            "data/users.json",
            "data/items.json",
            "data/auctions.json",
            "data/bids.json"
    };

    private static final Map<String, String> ORIGINAL_DATA = new LinkedHashMap<>();
    private static boolean originalDataCaptured;
    private static AuctionServer runningServer;

    public static void startTestServer(int port) {
        captureOriginalData();
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
        if (runningServer == null && originalDataCaptured) {
            restoreOriginalData();
            return;
        }

        for (String path : DATA_FILES) {
            writeDataFile(path, "[]");
        }
    }

    private static void captureOriginalData() {
        if (originalDataCaptured) {
            return;
        }

        for (String path : DATA_FILES) {
            try {
                Path file = Path.of(path);
                ORIGINAL_DATA.put(path, Files.exists(file) ? Files.readString(file) : null);
            } catch (Exception e) {
                ORIGINAL_DATA.put(path, null);
            }
        }
        originalDataCaptured = true;
    }

    private static void restoreOriginalData() {
        for (String path : DATA_FILES) {
            String content = ORIGINAL_DATA.get(path);
            try {
                Path file = Path.of(path);
                if (content == null) {
                    Files.deleteIfExists(file);
                } else {
                    writeDataFile(path, content);
                }
            } catch (Exception e) {
                System.err.println("Khong the khoi phuc test data: " + path);
            }
        }
    }

    private static void writeDataFile(String path, String content) {
        try {
            Path file = Path.of(path);
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(file, content);
        } catch (Exception e) {
            System.err.println("Khong the ghi test data: " + path);
        }
    }
}
