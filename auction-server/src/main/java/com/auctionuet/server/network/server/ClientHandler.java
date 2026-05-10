package com.auctionuet.server.network.server;

import com.auctionuet.server.domain.model.AuctionObserver;
import com.auctionuet.server.domain.model.BidRecord;
import com.auctionuet.server.network.protocol.ActionType;
import com.auctionuet.server.network.protocol.MessageSerializer;
import com.auctionuet.server.network.protocol.Request;
import com.auctionuet.server.network.protocol.Response;
import com.auctionuet.server.util.AppLogger;
import com.google.gson.Gson;
import com.auctionuet.server.util.json.GsonFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientHandler implements Runnable, AuctionObserver {

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
        Response response = router.route(request,this);

        // 5. Gửi response về client
        sendMessage(response);

        // 6. Log response + timing
        long elapsed = System.currentTimeMillis() - startTime;
        String respMessage = response.getMessage();
        AppLogger.logResponse(response.getStatus(), elapsed, respMessage);
    }

    public synchronized void sendMessage(Response response) {
        // Thêm trường type = "RESPONSE" để client listener thread phân loại
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("type", "RESPONSE");
        wrapper.put("status", response.getStatus());
        wrapper.put("message", response.getMessage());
        wrapper.put("data", response.getData());
        String json = GsonFactory.createForNetwork().toJson(wrapper);
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
    // ========== AuctionObserver PUSH METHODS ==========

    @Override
    public void onBidPlaced(String auctionId, BidRecord record) {
        // Server chủ động push JSON về client khi có bid mới
        Map<String, Object> push = new HashMap<>();
        push.put("type", "PUSH");
        push.put("pushType", "BID_UPDATE");
        push.put("auctionId", auctionId);
        push.put("bidderUsername", record.getBidderUsername());
        push.put("amount", record.getAmount());
        push.put("timestamp", record.getTimestamp().toString());

        String json = GsonFactory.createForNetwork().toJson(push);
        sendPush(json);
    }

    @Override
    public void onAuctionEnded(String auctionId, String winnerId, double finalPrice) {
        Map<String, Object> push = new HashMap<>();
        push.put("type", "PUSH");
        push.put("pushType", "AUCTION_ENDED");
        push.put("auctionId", auctionId);
        push.put("winnerId", winnerId);
        push.put("finalPrice", finalPrice);

        String json = GsonFactory.createForNetwork().toJson(push);
        sendPush(json);
    }

    @Override
    public void onAuctionExtended(String auctionId, LocalDateTime newEndTime) {
        Map<String, Object> push = new HashMap<>();
        push.put("type", "PUSH");
        push.put("pushType", "AUCTION_EXTENDED");
        push.put("auctionId", auctionId);
        push.put("newEndTime", newEndTime.toString());

        String json = GsonFactory.createForNetwork().toJson(push);
        sendPush(json);
    }
    /**
     * Push JSON trực tiếp xuống Client qua PrintWriter.
     * Thread-safe nhờ synchronized.
     */
    public synchronized void sendPush(String json) {
        out.println(json);
    }

}
