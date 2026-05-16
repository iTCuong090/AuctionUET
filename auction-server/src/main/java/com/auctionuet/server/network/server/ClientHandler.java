package com.auctionuet.server.network.server;

import com.auctionuet.protocol.MessageSerializer;
import com.auctionuet.protocol.PushActionType;
import com.auctionuet.protocol.PushMessage;
import com.auctionuet.protocol.Request;
import com.auctionuet.protocol.Response;
import com.auctionuet.protocol.dto.push.PushEvents;
import com.auctionuet.protocol.dto.response.bid.BidDTO;
import com.auctionuet.protocol.dto.response.user.UserDTO;
import com.auctionuet.server.domain.model.AuctionObserver;
import com.auctionuet.server.domain.model.BidRecord;
import com.auctionuet.server.domain.service.UserService;
import com.auctionuet.server.util.AppLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientHandler implements Runnable, AuctionObserver {

    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;
    private final RequestRouter router;
    private final AtomicInteger activeConnections;
    private final UserService userService;

    private final String clientIp;
    private final long connectedAt;

    public ClientHandler(Socket socket, RequestRouter router, AtomicInteger activeConnections,
            UserService userService) throws IOException {
        this.socket = socket;
        this.router = router;
        this.activeConnections = activeConnections;
        this.userService = userService;
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
            // Client closed the connection.
        } finally {
            cleanup();
        }
    }

    private void processLine(String line) {
        long startTime = System.currentTimeMillis();

        AppLogger.logRawReceived(line);

        Request request = MessageSerializer.deserialize(line);

        if (request == null || request.getAction() == null) {
            AppLogger.logInvalidRequest("JSON khong hop le hoac thieu truong 'action'");
            Response errResponse = Response.error("Invalid JSON format or missing action");
            sendResponse(errResponse);
            long elapsed = System.currentTimeMillis() - startTime;
            AppLogger.logResponse("ERROR", elapsed, "Invalid JSON format");
            return;
        }

        AppLogger.logParsedRequest(
                request.getAction().name(),
                request.getToken(),
                request.getData() != null ? request.getData().keySet() : java.util.Collections.emptySet());

        Response response = router.route(request, this);
        sendResponse(response);

        long elapsed = System.currentTimeMillis() - startTime;
        String respMessage = response.getMessage();
        AppLogger.logResponse(response.getStatus(), elapsed, respMessage);
    }

    public synchronized void sendResponse(Response response) {
        if (response != null) {
            out.println(response.toJson());
        }
    }

    private void cleanup() {
        int remaining = activeConnections.decrementAndGet();
        long durationMs = System.currentTimeMillis() - connectedAt;
        AppLogger.logClientDisconnected(clientIp, durationMs, remaining);

        try {
            socket.close();
        } catch (IOException e) {
            // Ignore close errors.
        }
    }

    @Override
    public void onBidPlaced(String auctionId, BidRecord record) {
        BidDTO bid = new BidDTO(
                auctionId,
                userService.getUserDTOById(record.getBidderId()),
                record.getAmount(),
                record.getTimestamp(),
                record.getBidType());
        PushEvents.BidUpdatePush dto = new PushEvents.BidUpdatePush(auctionId, bid);
        sendPush(new PushMessage(PushActionType.BID_UPDATE, dto));
    }

    @Override
    public void onAuctionEnded(String auctionId, String winnerId, double finalPrice) {
        UserDTO winner = winnerId != null ? userService.getUserDTOById(winnerId) : null;
        PushEvents.AuctionEndedPush dto = new PushEvents.AuctionEndedPush(
                auctionId,
                winner,
                finalPrice,
                "Phien dau gia da ket thuc");
        sendPush(new PushMessage(PushActionType.AUCTION_ENDED, dto));
    }

    @Override
    public void onAuctionExtended(String auctionId, LocalDateTime newEndTime) {
        PushEvents.AuctionExtendedPush dto = new PushEvents.AuctionExtendedPush(auctionId, newEndTime);
        sendPush(new PushMessage(PushActionType.AUCTION_EXTENDED, dto));
    }

    public synchronized void sendPush(PushMessage push) {
        if (push != null) {
            out.println(push.toJson());
        }
    }
}
