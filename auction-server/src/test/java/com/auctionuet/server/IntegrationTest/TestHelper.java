package com.auctionuet.server.IntegrationTest;

import java.io.*;
import java.net.Socket;
import com.auctionuet.protocol.Response;
import com.auctionuet.server.network.server.AuctionServer;
import com.google.gson.Gson;


public class TestHelper {
    private static AuctionServer runningServer;

    public static void startTestServer(int port) {
        new Thread(() -> {  // Thread(runable)
            try {
                runningServer = new AuctionServer(port);
                runningServer.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // Cho sever có thời gian khởi động, nếu không sẽ gây lỗi mất kết nối
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
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            out.println(json);          // gửi request
            String response = in.readLine();
            return new Gson().fromJson(response, Response.class);
        }
    }

    public static void cleanTestData() {
        // 1. Chỉ định đường dẫn đến file JSON test
        java.io.File file = new java.io.File("data/users.json");

        // 2. Kiểm tra xem file có tồn tại không trước khi xóa
        if (file.exists()) {
            boolean deleted = file.delete(); // Lệnh xóa file

            if (deleted) {
                System.out.println("Đã xóa file dữ liệu test thành công.");
            } else {
                System.err.println("Không thể xóa file test. Có thể file đang được mở bởi một chương trình khác.");
            }
        } else {
            System.out.println("Không tìm thấy file test để xóa.");
        }
    }
}
