package com.auctionuet.client.model;

import com.auctionuet.protocol.dto.response.user.UserDTO;
import com.auctionuet.protocol.enums.UserRole;

public class ClientSession {
    private static ClientSession instance;

    private String token;
    private UserDTO currentUser;

    private ClientSession() {}

    public static ClientSession getInstance() {
        if (instance == null) {
            instance = new ClientSession();
        }
        return instance;
    }

    public String getToken() {
        return token;
    }

    public UserDTO getCurrentUser() {
        return currentUser;
    }

    public void login(String token, UserDTO user) {
        this.token = token;
        this.currentUser = user;
    }

    public void updateCurrentUser(UserDTO user) {
        this.currentUser = user;
    }

    public void clearSession() {
        this.token = null;
        this.currentUser = null;
    }

    public boolean isSeller() {
        return currentUser != null && currentUser.getRole() == UserRole.SELLER;
    }

    public boolean isBidder() {
        return currentUser != null && currentUser.getRole() == UserRole.BIDDER;
    }
}
