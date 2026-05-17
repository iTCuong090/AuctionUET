package com.auctionuet.client.network;

import com.auctionuet.protocol.PushMessage;
import com.auctionuet.protocol.Request;
import com.auctionuet.protocol.Response;
import com.auctionuet.protocol.util.NetworkGson;
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
    private final Gson gson = NetworkGson.create();

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
            System.out.println("Da ket noi toi Server: " + host + ":" + port);

            listenerThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        Map<?, ?> msg = gson.fromJson(line, Map.class);
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
                    System.out.println("Ket noi server bi gian doan: " + e.getMessage());
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
            System.out.println("Da ngat ket noi Server.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public synchronized Response sendRequest(Request req) throws Exception {
        if (!isConnected()) throw new Exception("Chua ket noi toi server!");

        out.println(req.toJson());

        String jsonResponse = responseQueue.poll(10, TimeUnit.SECONDS);
        if (jsonResponse == null) throw new Exception("Server khong phan hoi (timeout)!");

        return gson.fromJson(jsonResponse, Response.class);
    }
}
