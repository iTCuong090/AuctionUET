package com.auctionuet.client.network;

import com.auctionuet.protocol.PushMessage;
import com.auctionuet.protocol.Request;
import com.auctionuet.protocol.Response;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ServerConnection {
    private static ServerConnection instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson = new Gson();
    
    private PushListener pushListener;
    private Thread listenerThread;
    private final BlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();

    public interface PushListener {
        void onPushMessage(PushMessage message);
    }

    private ServerConnection() {}

    public static ServerConnection getInstance() {
        if (instance == null) {
            instance = new ServerConnection();
        }
        return instance;
    }
    
    public void setPushListener(PushListener listener) {
        this.pushListener = listener;
    }

    public void connect(String host, int port) throws Exception {
        if (!isConnected()) {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Đã kết nối tới Server: " + host + ":" + port);
            
            listenerThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        Map<String, Object> msg = gson.fromJson(line, Map.class);
                        String type = (String) msg.get("type");

                        if ("PUSH".equals(type)) {
                            PushMessage push = gson.fromJson(line, PushMessage.class);
                            if (pushListener != null) {
                                pushListener.onPushMessage(push);
                            }
                        } else {
                            responseQueue.put(line);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Kết nối server bị gián đoạn: " + e.getMessage());
                }
            }, "server-listener");
            listenerThread.setDaemon(true);
            listenerThread.start();
        }
    }

    public void disconnect() {
        try {
            if (listenerThread != null) {
                listenerThread.interrupt();
            }
            if (socket != null) socket.close();
            if (out != null) out.close();
            if (in != null) in.close();
            System.out.println("Đã ngắt kết nối Server.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public synchronized Response sendRequest(Request req) throws Exception {
        if (!isConnected()) throw new Exception("Chưa kết nối tới server!");

        // --- CHIÊU MỚI: Tự động Validate bằng Hợp đồng ngay tại Client ---
        if (req.getAction() != null && req.getData() != null) {
            try {
                // Thử validate dữ liệu theo định nghĩa trong ActionType
                req.getAction().validateRequest(req.getDataObject());
            } catch (IllegalArgumentException e) {
                // Nếu sai cấu trúc, chặn lại luôn và báo lỗi cho UI
                return Response.error("Lỗi cấu trúc Request (Client-side): " + e.getMessage());
            }
        }

        String jsonRequest = com.auctionuet.protocol.util.NetworkGson.create().toJson(req);
        out.println(jsonRequest);

        String jsonResponse = responseQueue.poll(10, TimeUnit.SECONDS);
        if (jsonResponse == null) throw new Exception("Server không phản hồi (timeout)!");

        return com.auctionuet.protocol.util.NetworkGson.create().fromJson(jsonResponse, Response.class);
    }
}
