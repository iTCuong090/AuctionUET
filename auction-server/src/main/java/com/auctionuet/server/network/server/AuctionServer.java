package com.auctionuet.server.network.server;

import com.auctionuet.server.domain.manager.AuctionManager;
import com.auctionuet.server.domain.manager.DataManager;
import com.auctionuet.server.domain.service.AuctionService;
import com.auctionuet.server.domain.service.AdminService;
import com.auctionuet.server.domain.service.AuthService;
import com.auctionuet.server.domain.service.BidService;
import com.auctionuet.server.domain.service.FinancialAuditService;
import com.auctionuet.server.domain.service.ItemService;
import com.auctionuet.server.domain.service.TransactionService;
import com.auctionuet.server.domain.service.UserService;
import com.auctionuet.server.domain.service.WalletService;
import com.auctionuet.server.domain.service.SystemMonitorService;
import com.auctionuet.server.network.controller.AuctionController;
import com.auctionuet.server.network.controller.AdminController;
import com.auctionuet.server.network.controller.AuthController;
import com.auctionuet.server.network.controller.BidController;
import com.auctionuet.server.network.controller.ItemController;
import com.auctionuet.server.network.controller.UserController;
import com.auctionuet.server.network.controller.WalletController;
import com.auctionuet.server.persistence.dao.AuctionDAO;
import com.auctionuet.server.persistence.dao.BidDAO;
import com.auctionuet.server.persistence.dao.ItemDAO;
import com.auctionuet.server.persistence.dao.TransactionDAO;
import com.auctionuet.server.persistence.dao.UserDAO;
import com.auctionuet.server.persistence.dao.SystemSettingDAO;
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

    private final AtomicInteger activeConnections = new AtomicInteger(0);

    public AuctionServer(int port) {
        this.port = port;
    }

    public void start() {
        try {
            UserDAO userDAO = DataManager.getInstance().getUserDAO();
            AppLogger.logInit("UserDAO", null);

            ItemDAO itemDAO = DataManager.getInstance().getItemDAO();
            AppLogger.logInit("ItemDAO", null);

            AuctionDAO auctionDAO = DataManager.getInstance().getAuctionDAO();
            AppLogger.logInit("AuctionDAO", null);

            BidDAO bidDAO = DataManager.getInstance().getBidDAO();
            AppLogger.logInit("BidDAO", null);

            TransactionDAO transactionDAO = DataManager.getInstance().getTransactionDAO();
            AppLogger.logInit("TransactionDAO", null);

            SystemSettingDAO systemSettingDAO = DataManager.getInstance().getSystemSettingDAO();
            AppLogger.logInit("SystemSettingDAO", null);

            UserService userService = new UserService(userDAO);
            AppLogger.logInit("UserService", null);

            AdminService adminService = new AdminService(userDAO);
            adminService.ensureDefaultAdminExists();
            AppLogger.logInit("AdminSeed", "Checked");

            AuthService authService = new AuthService(userDAO, userService);
            AppLogger.logInit("AuthService", null);

            ItemService itemService = new ItemService(itemDAO, userService, auctionDAO, systemSettingDAO);
            AppLogger.logInit("ItemService", null);

            TransactionService transactionService = new TransactionService(transactionDAO);
            AppLogger.logInit("TransactionService", null);

            FinancialAuditService financialAuditService = new FinancialAuditService(
                    userDAO,
                    transactionDAO,
                    auctionDAO,
                    transactionService);
            int migratedTransactions = financialAuditService.migratePaidAuctionDepositTransactions();
            AppLogger.logInit("FinancialAuditService", "Migrated transactions: " + migratedTransactions);

            SystemMonitorService systemMonitorService = new SystemMonitorService(
                    activeConnections::get,
                    AuctionManager.getInstance()::getLiveAuctionCount);
            AuctionManager.getInstance().setLiveAuctionCountChangeCallback(systemMonitorService::notifyMetricsChanged);
            AppLogger.logInit("SystemMonitorService", null);

            WalletService walletService = new WalletService(userDAO, userService, transactionService);
            AppLogger.logInit("WalletService", null);

            BidService bidService = new BidService(
                    AuctionManager.getInstance(),
                    walletService,
                    bidDAO,
                    itemService,
                    auctionDAO,
                    userService);
            AppLogger.logInit("BidService", null);

            AuctionService auctionService = new AuctionService(
                    itemService,
                    bidService,
                    userService,
                    auctionDAO,
                    walletService);
            AppLogger.logInit("AuctionService", null);
            auctionService.reconcileAuctionsFromDatabase();
            AuctionManager.getInstance().startPeriodicReconcile();
            AppLogger.logInit("AuctionReconcile", "Started");

            AuthController authController = new AuthController(authService);
            AppLogger.logInit("AuthController", null);

            ItemController itemController = new ItemController(itemService);
            AppLogger.logInit("ItemController", null);

            AuctionController auctionController = new AuctionController(auctionService, itemService);
            AppLogger.logInit("AuctionController", null);

            WalletController walletController = new WalletController(walletService);
            AppLogger.logInit("WalletController", null);

            BidController bidController = new BidController(bidService);
            AppLogger.logInit("BidController", null);

            UserController userController = new UserController(userService);
            AppLogger.logInit("UserController", null);

            AdminController adminController =
                    new AdminController(
                            adminService,
                            auctionService,
                            itemService,
                            financialAuditService,
                            systemMonitorService);
            AppLogger.logInit("AdminController", null);

            this.router = new RequestRouter(
                    bidController,
                    walletController,
                    authController,
                    itemController,
                    auctionController,
                    userController,
                    adminController,
                    userService);
            AppLogger.logInit("RequestRouter", "Ready");

            isRunning = true;
            serverSocket = new ServerSocket(port);
            AppLogger.logServerListening(port);

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                int count = activeConnections.incrementAndGet();
                systemMonitorService.notifyMetricsChanged();
                String ip = clientSocket.getInetAddress().getHostAddress();
                AppLogger.logClientConnected(ip, clientSocket.getPort(), count);

                ClientHandler handler = new ClientHandler(
                        clientSocket,
                        this.router,
                        activeConnections,
                        userService,
                        systemMonitorService);
                new Thread(handler, "client-" + ip).start();
            }

        } catch (IOException e) {
            if (isRunning) {
                AppLogger.logServerError("Loi khi mo Server", e);
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
            AppLogger.logServerError("Loi khi dong Server: " + e.getMessage(), e);
        }
    }
}
