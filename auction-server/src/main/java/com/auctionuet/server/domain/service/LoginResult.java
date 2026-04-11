package com.auctionuet.server.domain.service;

import com.auctionuet.server.domain.model.User;

public class LoginResult {
    private final String token;
    private final User user;

    public LoginResult(String token, User user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public User getUser() {
        return user;
    }
}
