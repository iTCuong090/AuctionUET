package com.auctionuet.client.model;

public class ClientSession {
    // Biến lưu trữ duy nhất (Singleton)
    private static ClientSession instance;

    // Các "hộc tủ" chứa dữ liệu
    private String token;
    private String username;
    private String role; // "BIDDER", "SELLER", "ADMIN"

    // Chặn không cho tạo instance mới từ bên ngoài
    private ClientSession() {}

    // Lấy cái túi ra dùng
    public static ClientSession getInstance() {
        if (instance == null) {
            instance = new ClientSession();
        }
        return instance;
    }

    // ==========================================
    // GETTER VÀ SETTER ĐẦY ĐỦ CHO TỪNG BIẾN
    // ==========================================

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    // Hàm gộp của ông (tôi vẫn giữ lại phòng khi ông lười, muốn set 3 cái cùng lúc)
    public void setSession(String token, String username, String role) {
        this.token = token;
        this.username = username;
        this.role = role;
    }

    // ==========================================
    // HÀM DỌN DẸP (Dùng khi bấm nút Logout)
    // ==========================================
    public void clearSession() {
        this.token = null;
        this.username = null;
        this.role = null;
    }
}