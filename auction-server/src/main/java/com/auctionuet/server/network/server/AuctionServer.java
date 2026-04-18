package com.auctionuet.server.network.server;

import com.auctionuet.server.domain.manager.DataManager;
import com.auctionuet.server.domain.service.AuctionService;
import com.auctionuet.server.domain.service.AuthService;
import com.auctionuet.server.network.controller.AuctionController;
import com.auctionuet.server.network.controller.AuthController;
import com.auctionuet.server.persistence.dao.AuctionDAO;
import com.auctionuet.server.persistence.dao.ItemDAO;
import com.auctionuet.server.persistence.dao.UserDAO;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class AuctionServer {
    int port;
    private ServerSocket serverSocket ;
    private boolean isRunning=false;
    private RequestRouter router;
    public AuctionServer(int port){
        this.port=port;
    }
    public void start(){
        try {
            // Tạo DAO
            UserDAO userDAO = DataManager.getInstance().getUserDAO();
            ItemDAO itemDAO = DataManager.getInstance().getItemDAO();
            AuctionDAO auctionDAO = DataManager.getInstance().getAuctionDAO();

            // Tạo Service
            AuthService authService = new AuthService(userDAO);
            AuctionService auctionService = new AuctionService(itemDAO, auctionDAO);

            // Tạo Controller
            AuthController authController = new AuthController(authService);
            AuctionController auctionController = new AuctionController(auctionService, itemDAO);

            // Tạo Router
            this.router = new RequestRouter(authController, auctionController);

            isRunning=true;
            serverSocket=new ServerSocket(port);
            System.out.println("[SERVER] Listening on port " + port);
            while (isRunning) {
                Socket clientSocket = serverSocket.accept();  // blocking
                System.out.println("[SERVER] New client connected: " + clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket,this.router);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            if (isRunning) {
                System.out.println("[SERVER] Lỗi khi mở Sever.");
                e.printStackTrace();
            }
        }
    }
    public void stop() {
        this.isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("[SERVER] Đã đóng Server.");
            }
        } catch (IOException e) {
            System.err.println("[SERVER] Lỗi khi đóng Server: " + e.getMessage());
        }
    }
}

