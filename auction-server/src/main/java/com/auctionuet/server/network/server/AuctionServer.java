package com.auctionuet.server.network.server;

import com.auctionuet.server.domain.manager.DataManager;
import com.auctionuet.server.domain.service.*;
import com.auctionuet.server.network.controller.*;
import com.auctionuet.server.persistence.dao.AuctionDAO;
import com.auctionuet.server.persistence.dao.BidDAO;
import com.auctionuet.server.persistence.dao.ItemDAO;
import com.auctionuet.server.persistence.dao.UserDAO;
import com.auctionuet.server.util.AppLogger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

public class AuctionServer {
    int port;
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private RequestRouter router;

    // Đếm số connection đang active để log
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    public AuctionServer(int port) {
        this.port = port;
    }

    public void start() {
        try {
            // ── Khởi tạo DAO ──
            UserDAO userDAO = DataManager.getInstance().getUserDAO();
            AppLogger.logInit("UserDAO", null);

            ItemDAO itemDAO = DataManager.getInstance().getItemDAO();
            AppLogger.logInit("ItemDAO", null);

            AuctionDAO auctionDAO = DataManager.getInstance().getAuctionDAO();
            AppLogger.logInit("AuctionDAO", null);

            BidDAO bidDAO = DataManager.getInstance().getBidDAO();
            AppLogger.logInit("BidDAO", null);
            // ── Khởi tạo Service ──
            AuthService authService = new AuthService(userDAO);
            AppLogger.logInit("AuthService", null);

            ItemService itemService = new ItemService(itemDAO);
            AppLogger.logInit("ItemService", null);

            AuctionService auctionService = new AuctionService(itemService, auctionDAO);
            AppLogger.logInit("AuctionService", null);

            WalletService walletService = new WalletService(userDAO);
            AppLogger.logInit("WalletService", null);

            BidService bidService = new BidService(walletService, bidDAO, itemService, auctionDAO);
            // ── Khởi tạo Controller ──
            AuthController authController = new AuthController(authService);
            AppLogger.logInit("AuthController", null);

            ItemController itemController = new ItemController(itemService);
            AppLogger.logInit("ItemController", null);

            AuctionController auctionController = new AuctionController(auctionService, itemService);
            AppLogger.logInit("AuctionController", null);

            WalletController walletController = new WalletController(walletService);
            AppLogger.logInit("WalletController", null);

            BidController bidController = new BidController(bidService);
            AppLogger.logInit("WalletController", null);

            // ── Khởi tạo Router ──
            this.router = new RequestRouter(bidController,walletController,authController, itemController, auctionController);
            AppLogger.logInit("RequestRouter", "Ready");

            isRunning = true;
            serverSocket = new ServerSocket(port);
            AppLogger.logServerListening(port);

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();  // blocking
                int count = activeConnections.incrementAndGet();
                String ip = clientSocket.getInetAddress().getHostAddress();
                AppLogger.logClientConnected(ip, clientSocket.getPort(), count);

                ClientHandler handler = new ClientHandler(clientSocket, this.router, activeConnections);
                new Thread(handler, "client-" + ip).start();
            }

        } catch (IOException e) {
            if (isRunning) {
                AppLogger.logServerError("Lỗi khi mở Server", e);
            }
        }
    }

    public void stop() {
        this.isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                AppLogger.logServerStopped();
            }
        } catch (IOException e) {
            AppLogger.logServerError("Lỗi khi đóng Server: " + e.getMessage(), e);
        }
    }
}
