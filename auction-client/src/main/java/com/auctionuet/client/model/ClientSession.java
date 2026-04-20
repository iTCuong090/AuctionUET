package com.auctionuet.client.model;
import com.auctionuet.client.network.protocol.UserRole;
public class ClientSession {
    // Biến lưu trữ duy nhất (Singleton)
    private static ClientSession instance;

    // Các "hộc tủ" chứa dữ liệu
    private String token;
    private UserDTO currentUser; // ĐÃ ĐỔI: Dùng UserDTO thay vì các chuỗi rời rạc

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
    // GETTER VÀ SETTER
    // ==========================================

    public String getToken() {
        return token;
    }

    public UserDTO getCurrentUser() {
        return currentUser;
    }

    // Hàm gộp lúc đăng nhập thành công
    public void login(String token, UserDTO user) {
        this.token = token;
        this.currentUser = user;
    }

    // ==========================================
    // HÀM DỌN DẸP (Dùng khi bấm nút Logout)
    // ==========================================
    public void clearSession() {
        this.token = null;
        this.currentUser = null;
    }

    // Kiểm tra Role bằng Enum cực kỳ an toàn
    public boolean isSeller() {
        if (currentUser == null || currentUser.getRole() == null) return false;
        return currentUser.getRole() == UserRole.SELLER;
    }
}