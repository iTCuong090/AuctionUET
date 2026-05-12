package com.auctionuet.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class FakeServer {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(8888)) {
            System.out.println("🤖 Server Pha-ke v2.0 đang chạy ở cổng 8888...");

            while (true) {
                // Đứng chờ Client cắm ống nước vào
                Socket clientSocket = serverSocket.accept();
                System.out.println("🔌 Có Client vừa cắm ống nước!");

                // Tạo một "nhân viên" riêng biệt để ngồi phục vụ cái ống nước này liên tục
                new Thread(() -> {
                    try {
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                        String requestLine;
                        // VÒNG LẶP NÀY CHÍNH LÀ CHÌA KHÓA: Liên tục nghe ngóng xem Client có gửi gì không
                        while ((requestLine = in.readLine()) != null) {
                            System.out.println("📦 Gói hàng Client gửi: " + requestLine);

                            // Bất kể gửi Đăng nhập hay Đăng xuất, cứ nhổ ra chữ OK
                            String fakeResponse = "{\"type\":\"RESPONSE\",\"status\":\"OK\",\"message\":\"\",\"data\":\"token-gia-vo-day\"}";
                            out.println(fakeResponse);
                            System.out.println("📤 Đã nhổ kết quả OK về cho Client!\n");
                        }
                    } catch (Exception e) {
                        System.out.println("❌ Ngắt kết nối với 1 Client.");
                    }
                }).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
