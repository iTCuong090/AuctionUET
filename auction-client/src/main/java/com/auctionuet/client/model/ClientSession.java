package com.auctionuet.client.model;

import com.auctionuet.protocol.dto.response.user.UserDTO;
import com.auctionuet.protocol.enums.UserRole;
import com.google.gson.Gson;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class ClientSession {
    private static final String CONFIG_FILENAME = "gemini_config.json";
    private static ClientSession instance;

    private String token;
    private UserDTO currentUser;
    private boolean mustChangePassword;
    private String geminiApiKey = "";
    private String geminiModel = "gemini-2.5-flash-lite";
    private final List<Consumer<UserDTO>> userChangeListeners = new CopyOnWriteArrayList<>();

    private ClientSession() {
        loadConfigFromFile();
    }

    public static ClientSession getInstance() {
        if (instance == null) {
            instance = new ClientSession();
        }
        return instance;
    }

    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    public void setGeminiApiKey(String apiKey) {
        this.geminiApiKey = apiKey != null ? apiKey.trim() : "";
        saveConfigToFile();
    }

    public String getGeminiModel() {
        return geminiModel;
    }

    public void setGeminiModel(String geminiModel) {
        this.geminiModel = geminiModel != null && !geminiModel.isBlank() ? geminiModel.trim() : "gemini-2.5-flash-lite";
        saveConfigToFile();
    }

    private void loadConfigFromFile() {
        File file = new File(CONFIG_FILENAME);
        if (file.exists()) {
            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                Gson gson = new Gson();
                ConfigData data = gson.fromJson(reader, ConfigData.class);
                if (data != null) {
                    if (data.geminiApiKey != null) {
                        this.geminiApiKey = data.geminiApiKey.trim();
                    }
                    if (data.geminiModel != null && !data.geminiModel.isBlank()) {
                        this.geminiModel = data.geminiModel.trim();
                    }
                }
            } catch (Exception e) {
                System.err.println("Lỗi khi đọc file cấu hình Gemini: " + e.getMessage());
            }
        }
    }

    private void saveConfigToFile() {
        File file = new File(CONFIG_FILENAME);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            Gson gson = new Gson();
            ConfigData data = new ConfigData();
            data.geminiApiKey = this.geminiApiKey;
            data.geminiModel = this.geminiModel;
            gson.toJson(data, writer);
        } catch (Exception e) {
            System.err.println("Lỗi khi ghi file cấu hình Gemini: " + e.getMessage());
        }
    }

    private static class ConfigData {
        String geminiApiKey;
        String geminiModel;
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
