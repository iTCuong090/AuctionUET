package com.auctionuet.client.network;

import com.auctionuet.client.network.protocol.Request;
import com.auctionuet.client.network.protocol.Response;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerConnection {
    private static ServerConnection instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson = new Gson();

    private ServerConnection() {}

    public static ServerConnection getInstance() {
        if (instance == null) {
            instance = new ServerConnection();
        }
        return instance;
    }

    public void connect(String host, int port) throws Exception {
        if (!isConnected()) {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Đã kết nối tới Server: " + host + ":" + port);
        }
    }

    public void disconnect() {
        try {
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

    public Response sendRequest(Request req) throws Exception {
        if (!isConnected()) throw new Exception("Chưa kết nối tới server!");

        // 1. Chuyển Object thành chuỗi JSON
        String jsonRequest = gson.toJson(req);

        // 2. Bắn sang Server
        out.println(jsonRequest);

        // 3. Nín thở chờ Server trả lời
        String jsonResponse = in.readLine();
        if (jsonResponse == null) throw new Exception("Server đã ngắt kết nối đột ngột!");

        // 4. Dịch chuỗi JSON về lại Object Response
        return gson.fromJson(jsonResponse, Response.class);
    }
}