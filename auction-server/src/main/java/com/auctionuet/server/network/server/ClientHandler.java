package com.auctionuet.server.network.server;

import com.auctionuet.server.network.protocol.ActionType;
import com.auctionuet.server.network.protocol.MessageSerializer;
import com.auctionuet.server.network.protocol.Request;
import com.auctionuet.server.network.protocol.Response;
import com.auctionuet.server.util.AppLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;
    private final RequestRouter router;
    private final AtomicInteger activeConnections;

    // IP của client để log disconnect
    private final String clientIp;
    // Thời điểm kết nối để tính session duration
    private final long connectedAt;

    public ClientHandler(Socket socket, RequestRouter router, AtomicInteger activeConnections) throws IOException {
        this.socket = socket;
        this.router = router;
        this.activeConnections = activeConnections;
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.clientIp = socket.getInetAddress().getHostAddress();
        this.connectedAt = System.currentTimeMillis();
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                processLine(line);
            }
        } catch (IOException e) {
            // Client đột ngột đóng kết nối — không cần log stack trace
        } finally {
            cleanup();
        }
    }

    /**
     * Xử lý một dòng text nhận từ client: deserialize → route → send response.
     * Đo timing cho toàn bộ pipeline.
     */
    private void processLine(String line) {
        long startTime = System.currentTimeMillis();

        // 1. Log raw JSON (DEV only)
        AppLogger.logRawReceived(line);

        // 2. Deserialize JSON → Request
        Request request = MessageSerializer.deserialize(line);

        if (request == null || request.getAction() == null) {
            AppLogger.logInvalidRequest("JSON không hợp lệ hoặc thiếu trường 'action'");
            Response errResponse = Response.error("Invalid JSON format or missing action");
            sendMessage(errResponse);
            long elapsed = System.currentTimeMillis() - startTime;
            AppLogger.logResponse("ERROR", elapsed, "Invalid JSON format");
            return;
        }

        // 3. Log parsed request info
        AppLogger.logParsedRequest(
            request.getAction().name(),
            request.getToken(),
            request.getData() != null ? request.getData().keySet() : java.util.Collections.emptySet()
        );

        // 4. Route request → Response
        Response response = router.route(request);

        // 5. Gửi response về client
        sendMessage(response);

        // 6. Log response + timing
        long elapsed = System.currentTimeMillis() - startTime;
        String respMessage = response.getMessage();
        AppLogger.logResponse(response.getStatus(), elapsed, respMessage);
    }

    public synchronized void sendMessage(Response response) {
        String json = MessageSerializer.serialize(response);
        out.println(json);
    }

    private void cleanup() {
        int remaining = activeConnections.decrementAndGet();
        long durationMs = System.currentTimeMillis() - connectedAt;
        AppLogger.logClientDisconnected(clientIp, durationMs);

        try {
            socket.close();
        } catch (IOException e) {
            // Ignore
        }
    }
}
