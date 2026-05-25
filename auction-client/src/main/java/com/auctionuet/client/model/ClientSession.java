package com.auctionuet.client.model;

import com.auctionuet.protocol.dto.response.user.UserDTO;
import com.auctionuet.protocol.enums.UserRole;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class ClientSession {
    private static ClientSession instance;

    private String token;
    private UserDTO currentUser;
    private boolean mustChangePassword;
    private final List<Consumer<UserDTO>> userChangeListeners = new CopyOnWriteArrayList<>();

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
        login(token, user, false);
    }

    public void login(String token, UserDTO user, boolean mustChangePassword) {
        this.token = token;
        this.currentUser = user;
        this.mustChangePassword = mustChangePassword;
        notifyUserChanged();
    }

    public void updateCurrentUser(UserDTO user) {
        this.currentUser = user;
        notifyUserChanged();
    }

    public void clearSession() {
        this.token = null;
        this.currentUser = null;
        this.mustChangePassword = false;
        notifyUserChanged();
    }

    public void addUserChangeListener(Consumer<UserDTO> listener) {
        if (listener != null) {
            userChangeListeners.add(listener);
        }
    }

    public boolean isSeller() {
        return currentUser != null && currentUser.getRole() == UserRole.SELLER;
    }

    public boolean isBidder() {
        return currentUser != null && currentUser.getRole() == UserRole.BIDDER;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == UserRole.ADMIN;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void completeRequiredPasswordChange() {
        mustChangePassword = false;
        notifyUserChanged();
    }

    private void notifyUserChanged() {
        for (Consumer<UserDTO> listener : userChangeListeners) {
            listener.accept(currentUser);
        }
    }
}
