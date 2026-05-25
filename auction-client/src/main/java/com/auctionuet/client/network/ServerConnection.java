package com.auctionuet.client.network;

import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.view.SceneManager;
import com.auctionuet.protocol.ActionType;
import com.auctionuet.protocol.PushMessage;
import com.auctionuet.protocol.Request;
import com.auctionuet.protocol.Response;
import com.auctionuet.protocol.util.NetworkGson;
import com.google.gson.Gson;

import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ServerConnection {
    private static final int RESPONSE_TIMEOUT_SECONDS = 10;
    private static final int HEARTBEAT_INTERVAL_SECONDS = 30;

    private static ServerConnection instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson = NetworkGson.create();

    private String host;
    private int port;
    private boolean manualDisconnect;
    private volatile boolean connectionAlive;
    private volatile boolean authExpiredHandled;
    private int connectionGeneration;

    private PushListener pushListener;
    private Thread listenerThread;
    private Thread heartbeatThread;
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

    public synchronized void connect(String host, int port) throws Exception {
        this.host = host;
        this.port = port;
        this.manualDisconnect = false;
        this.authExpiredHandled = false;

        if (!isConnected()) {
            openSocket();
        }
        startHeartbeat();
    }

    public synchronized void disconnect() {
        manualDisconnect = true;
        stopHeartbeat();
        closeSocket();
        System.out.println("Da ngat ket noi Server.");
    }

    public synchronized boolean isConnected() {
        return socket != null
                && socket.isConnected()
                && !socket.isClosed()
                && !socket.isInputShutdown()
                && !socket.isOutputShutdown()
                && connectionAlive;
    }

    public synchronized Response sendRequest(Request req) throws Exception {
        return sendRequest(req, true);
    }

    private synchronized Response sendRequest(Request req, boolean allowRetry) throws Exception {
        try {
            ensureConnected();
            Response response = sendOnce(req);
            handleAuthExpiredIfNeeded(response);
            return response;
        } catch (Exception e) {
            markDisconnected(connectionGeneration);
            if (!allowRetry || !canReconnect()) {
                throw e;
            }
            reconnect();
            Response response = sendOnce(req);
            handleAuthExpiredIfNeeded(response);
            return response;
        }
    }

    private void ensureConnected() throws Exception {
        if (!isConnected()) {
            if (!canReconnect()) {
                throw new Exception("Chua ket noi toi server!");
            }
            reconnect();
        }
    }

    private Response sendOnce(Request req) throws Exception {
        if (out == null) {
            throw new Exception("Chua ket noi toi server!");
        }

        responseQueue.clear();
        out.println(req.toJson());
        if (out.checkError()) {
            throw new Exception("Khong gui duoc request toi server");
        }

        String jsonResponse = responseQueue.poll(RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (jsonResponse == null) {
            throw new Exception("Server khong phan hoi (timeout)!");
        }

        return gson.fromJson(jsonResponse, Response.class);
    }

    private void openSocket() throws Exception {
        closeSocket();
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        connectionAlive = true;
        connectionGeneration++;
        responseQueue.clear();
        System.out.println("Da ket noi toi Server: " + host + ":" + port);
        startListenerThread(connectionGeneration);
    }

    private void startListenerThread(int generation) {
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
                if (!manualDisconnect) {
                    System.out.println("Ket noi server bi gian doan: " + e.getMessage());
                }
            } finally {
                markDisconnected(generation);
            }
        }, "server-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void startHeartbeat() {
        if (heartbeatThread != null && heartbeatThread.isAlive()) {
            return;
        }
        heartbeatThread = new Thread(() -> {
            while (!manualDisconnect) {
                try {
                    TimeUnit.SECONDS.sleep(HEARTBEAT_INTERVAL_SECONDS);
                    String token = ClientSession.getInstance().getToken();
                    if (token == null || token.isBlank()) {
                        continue;
                    }
                    Response response = sendRequest(new Request(ActionType.PING, null, token), true);
                    handleAuthExpiredIfNeeded(response);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception e) {
                    System.out.println("Heartbeat khong thanh cong: " + e.getMessage());
                }
            }
        }, "server-heartbeat");
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }

    private void stopHeartbeat() {
        if (heartbeatThread != null) {
            heartbeatThread.interrupt();
            heartbeatThread = null;
        }
    }

    private void reconnect() throws Exception {
        if (!canReconnect()) {
            throw new Exception("Chua co thong tin server de ket noi lai");
        }
        openSocket();
    }

    private boolean canReconnect() {
        return host != null && !host.isBlank() && port > 0;
    }

    private void closeSocket() {
        try {
            if (listenerThread != null) {
                listenerThread.interrupt();
            }
            if (socket != null) socket.close();
            if (out != null) out.close();
            if (in != null) in.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            markDisconnected(connectionGeneration);
            socket = null;
            out = null;
            in = null;
        }
    }

    private synchronized void markDisconnected(int generation) {
        if (generation == connectionGeneration) {
            connectionAlive = false;
        }
    }

    private void handleAuthExpiredIfNeeded(Response response) {
        if (response == null || !"ERROR".equals(response.getStatus()) || !isAuthExpiredMessage(response.getMessage())) {
            return;
        }
        if (authExpiredHandled) {
            return;
        }
        authExpiredHandled = true;
        ClientSession.getInstance().clearSession();
        Platform.runLater(() -> SceneManager.getInstance().switchScene("/fxml/MainView.fxml"));
    }

    private boolean isAuthExpiredMessage(String message) {
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("token không hợp lệ")
                || normalized.contains("token khong hop le")
                || normalized.contains("hết hạn")
                || normalized.contains("het han");
    }
}
