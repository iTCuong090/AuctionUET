package com.auctionuet.server.network.server;

import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.network.protocol.ActionType;
import com.auctionuet.server.network.protocol.MessageSerializer;
import com.auctionuet.server.network.protocol.Request;
import com.auctionuet.server.network.protocol.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements  Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private User currentUser;
    private String token;
    private final RequestRouter router;
    public ClientHandler(Socket socket,RequestRouter router) throws IOException{
        this.socket=socket;
        this.out=new PrintWriter(socket.getOutputStream(),true);
        this.in=new BufferedReader(new InputStreamReader(socket.getInputStream()));
        currentUser=null;
        this.router=router;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    @Override
    public void run(){
        try {
            String line;
            while ((line = in.readLine()) != null) {
                // 1. Deserialize JSON → Request
                Request request = MessageSerializer.deserialize(line);
                // 2. Xử lý request
                Response response;
                if (request == null) {
                    response = Response.error("Invalid JSON format");
                } else {
                    response = router.route(request);
                    // Nếu login thành công → cập nhật currentUser
                    if (request.getAction() == ActionType.LOGIN
                            && "OK".equals(response.getStatus())) {
                        
                    }
                    }
                    // 3. Gửi response
                    sendMessage(response);
                }

        }catch (IOException e) {
            System.out.println("[SERVER] Client disconnected: " + socket.getInetAddress());
        } finally {
            cleanup();
        }
    }
    public synchronized void sendMessage(Response response) {
        String json = MessageSerializer.serialize(response);
        out.println(json);  // println tự thêm \n → BufferedReader.readLine() của Client đọc được
    }
    private void cleanup() {
        try {
            socket.close();
        } catch (IOException e) {
        }
        System.out.println("[SERVER] Client handler cleaned up");
    }
}

