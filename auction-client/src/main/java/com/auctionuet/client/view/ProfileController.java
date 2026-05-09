package com.auctionuet.client.view;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.util.List;

import com.auctionuet.client.model.AuctionDTO;
import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.model.UserDTO;
import com.auctionuet.client.network.AuctionClient;
import com.auctionuet.client.network.WalletClient;
import com.auctionuet.client.network.protocol.AuctionStatus;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import java.util.Map;

public class ProfileController {

    // === THÔNG TIN CÁ NHÂN (Panel trái) ===
    @FXML private Label usernameLabel;
    @FXML private Label roleLabel;
    @FXML private Label balanceLabel;

    // === THỐNG KÊ HOẠT ĐỘNG (Grid bên phải) ===
    @FXML private Label statWonLabel;        // Đấu giá đã thắng
    @FXML private Label statSoldLabel;       // Sản phẩm đã bán
    @FXML private Label statReputationLabel; // Điểm uy tín (hiện sao)
    @FXML private Label statActiveLabel;     // Đang tham gia

    @FXML
    public void initialize() {
        System.out.println("Đã tải màn hình Profile!");

        // ==============================
        // PHẦN 1: HIỂN THỊ THÔNG TIN USER
        // ==============================
        UserDTO currentUser = ClientSession.getInstance().getCurrentUser();

        if (currentUser != null) {
            // Hiển thị tên
            usernameLabel.setText("@" + currentUser.getUsername());

            // Hiển thị vai trò (Role) và đổi màu cho ngầu
            String role = currentUser.getRole() != null ? currentUser.getRole().name() : "GUEST";
            roleLabel.setText(role);

            if ("SELLER".equals(role)) {
                // Seller -> nhãn đỏ cam
                roleLabel.setStyle("-fx-background-color: #e94560; -fx-background-radius: 10; -fx-padding: 6 18;");
            } else {
                // Bidder -> nhãn xanh dương
                roleLabel.setStyle("-fx-background-color: #3498db; -fx-background-radius: 10; -fx-padding: 6 18;");
            }

            // Số dư (Tạm để cứng vì DTO hiện tại chưa có trường Balance)
            balanceLabel.setText("Đang tải...");

        } else {
            // Chưa đăng nhập hoặc session bị lỗi
            usernameLabel.setText("Chưa đăng nhập");
            roleLabel.setText("N/A");
            balanceLabel.setText("0 VNĐ");
        }

        // ==============================
        // PHẦN 2: GỌI API LẤY THỐNG KÊ THẬT VÀ SỐ DƯ VÍ
        // ==============================
        loadProfileStats();
        loadWalletBalance();
    }

    private void loadWalletBalance() {
        String token = ClientSession.getInstance().getToken();
        if (token == null) return;

        new Thread(() -> {
            try {
                WalletClient walletClient = new WalletClient();
                Map<String, Double> walletInfo = walletClient.getWallet(token);
                double balance = walletInfo.getOrDefault("balance", 0.0);
                Platform.runLater(() -> balanceLabel.setText(String.format("%,.0f VNĐ", balance)));
            } catch (Exception e) {
                Platform.runLater(() -> balanceLabel.setText("Lỗi"));
            }
        }).start();
    }

    @FXML
    private void handleTopUp() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/WalletView.fxml"));
            Parent root = loader.load();
            
            // Tìm Dashboard HBox/StackPane
            Parent currentRoot = balanceLabel.getScene().getRoot();
            if (currentRoot instanceof HBox) {
                // Trong DashboardView, contentArea nằm trong main-card VBox
                VBox mainCard = (VBox) ((HBox) currentRoot).getChildren().get(1);
                StackPane contentArea = (StackPane) mainCard.getChildren().get(1);
                contentArea.getChildren().setAll(root);
            } else if (currentRoot instanceof javafx.scene.layout.BorderPane) {
                ((javafx.scene.layout.BorderPane) currentRoot).setCenter(root);
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi chuyển sang ví: " + e.getMessage());
        }
    }

    /**
     * Gọi API GET_AUCTIONS để tính thống kê cho User hiện tại:
     * - Đấu giá đã thắng: Số phiên FINISHED mà currentWinnerUsername == username hiện tại
     * - Sản phẩm đã bán: Số phiên FINISHED mà sellerUsername == username hiện tại
     * - Điểm uy tín: Tạm tính = ⭐ x (số thắng + số bán) (cần API riêng sau này)
     * - Đang tham gia: Số phiên RUNNING (tạm tính = tất cả phiên RUNNING vì chưa có API bid history)
     */
    private void loadProfileStats() {
        String token = ClientSession.getInstance().getToken();
        UserDTO currentUser = ClientSession.getInstance().getCurrentUser();

        if (token == null || currentUser == null) {
            setStatsDefault();
            return;
        }

        String myUsername = currentUser.getUsername();

        // Chạy trên luồng nền
        new Thread(() -> {
            try {
                AuctionClient client = new AuctionClient();
                List<AuctionDTO> allAuctions = client.getAuctions(token);

                // ── TÍNH THỐNG KÊ ──

                // 1. Đấu giá đã thắng: phiên FINISHED mà mình là người thắng
                long wonCount = allAuctions.stream()
                        .filter(a -> a.getStatus() == AuctionStatus.FINISHED)
                        .filter(a -> myUsername.equals(a.getCurrentWinnerUsername()))
                        .count();

                // 2. Sản phẩm đã bán: phiên FINISHED mà mình là seller
                long soldCount = allAuctions.stream()
                        .filter(a -> a.getStatus() == AuctionStatus.FINISHED)
                        .filter(a -> myUsername.equals(a.getSellerUsername()))
                        .count();

                // 3. Đang tham gia: phiên RUNNING (tạm tính tất cả vì chưa có API bid history)
                long activeCount = allAuctions.stream()
                        .filter(a -> a.getStatus() == AuctionStatus.RUNNING)
                        .count();

                // 4. Điểm uy tín: tạm tính sao dựa trên số giao dịch
                int totalTransactions = (int) (wonCount + soldCount);
                String reputation = calculateReputation(totalTransactions);

                // ── CẬP NHẬT GIAO DIỆN ──
                Platform.runLater(() -> {
                    statWonLabel.setText(String.valueOf(wonCount));
                    statSoldLabel.setText(String.valueOf(soldCount));
                    statActiveLabel.setText(String.valueOf(activeCount));
                    statReputationLabel.setText(reputation);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.out.println("❌ Lỗi tải thống kê Profile: " + e.getMessage());
                    setStatsDefault();
                });
            }
        }).start();
    }

    /**
     * Tính điểm uy tín dạng ⭐ dựa trên tổng giao dịch.
     * Mỗi 2 giao dịch = 1 sao, tối đa 5 sao.
     */
    private String calculateReputation(int totalTransactions) {
        int stars = Math.min(5, Math.max(1, (totalTransactions / 2) + 1));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stars; i++) {
            sb.append("⭐");
        }
        // Thêm sao trống cho đẹp
        for (int i = stars; i < 5; i++) {
            sb.append("☆");
        }
        return sb.toString();
    }

    /**
     * Đặt giá trị mặc định khi không thể tải dữ liệu
     */
    private void setStatsDefault() {
        statWonLabel.setText("0");
        statSoldLabel.setText("0");
        statActiveLabel.setText("0");
        statReputationLabel.setText("⭐☆☆☆☆");
    }
}
