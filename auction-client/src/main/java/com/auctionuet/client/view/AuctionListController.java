package com.auctionuet.client.view;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import java.util.List;

import com.auctionuet.client.model.AuctionDTO;
import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.network.AuctionClient;

public class AuctionListController {

    @FXML private FlowPane auctionGrid;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private TextField searchField;

    @FXML
    public void initialize() {
        filterComboBox.getItems().addAll("Tất cả", "OPEN", "RUNNING", "FINISHED");
        filterComboBox.setValue("Tất cả");

        // Gọi hàm tải dữ liệu thật từ Server
        loadAuctionsFromServer();
    }

    private void loadAuctionsFromServer() {
        String currentToken = ClientSession.getInstance().getToken();

        if (currentToken == null) return;

        // CHẠY LUỒNG MẠNG THẬT
        new Thread(() -> {
            try {
                AuctionClient client = new AuctionClient();
                // Bắn API lấy danh sách phiên đấu giá
                List<AuctionDTO> list = client.getAuctions(currentToken);

                // Đẩy lên giao diện
                Platform.runLater(() -> renderGrid(list));

            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.out.println("❌ Lỗi tải danh sách đấu giá: " + e.getMessage());
                    // Nếu Server tắt hoặc lỗi, ông có thể hiện Label thông báo ở đây
                });
            }
        }).start();
    }

    private void renderGrid(List<AuctionDTO> auctions) {
        auctionGrid.getChildren().clear();
        for (AuctionDTO item : auctions) {
            auctionGrid.getChildren().add(createAuctionCard(item));
        }
    }

    private VBox createAuctionCard(AuctionDTO item) {
        VBox card = new VBox(12);
        card.getStyleClass().add("auction-card");
        card.setPrefWidth(250);

        Label title = new Label(item.getTitle());
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: white;");
        title.setWrapText(true);

        Label price = new Label(String.format("💰 %,.0f VNĐ", item.getCurrentHighestBid()));
        price.setStyle("-fx-font-size: 15px; -fx-text-fill: #4ecdc4; -fx-font-weight: bold;");

        String statusStr = item.getStatus() != null ? item.getStatus().name() : "UNKNOWN";
        Label statusBadge = new Label(statusStr);

        if ("RUNNING".equals(statusStr)) {
            statusBadge.setText("🟢 " + statusStr);
            statusBadge.getStyleClass().add("status-badge-running");
        } else if ("FINISHED".equals(statusStr)) {
            statusBadge.setText("🔴 " + statusStr);
            statusBadge.getStyleClass().add("status-badge-finished");
        } else {
            statusBadge.setText("🟡 " + statusStr);
            statusBadge.getStyleClass().add("status-badge-open");
        }

        VBox infoBox = new VBox(5);
        // Kiểm tra null cho thời gian và người bán
        String endTime = item.getEndTime() != null ? item.getEndTime() : "Chưa rõ";
        String seller = item.getSellerUsername() != null ? item.getSellerUsername() : "Chưa rõ";

        Label timeLabel = new Label("⏰ Kết thúc: " + endTime);
        Label sellerLabel = new Label("👤 Seller: " + seller);

        String subStyle = "-fx-text-fill: #a0a0b0; -fx-font-size: 13px;";
        timeLabel.setStyle(subStyle);
        sellerLabel.setStyle(subStyle);

        infoBox.getChildren().addAll(timeLabel, sellerLabel);

        Button btnDetail = new Button("Xem chi tiết");
        btnDetail.getStyleClass().add("button");
        btnDetail.setMaxWidth(Double.MAX_VALUE);

        btnDetail.setOnAction(e -> {
            try {
                // 1. Tải bản vẽ của màn hình Chi tiết
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/AuctionDetailView.fxml"));
                javafx.scene.Parent detailRoot = loader.load();

                // 2. Tóm lấy thằng Quản lý và truyền ID
                AuctionDetailController detailController = loader.getController();
                detailController.setAuctionData(item.getId());

                // ==========================================
                // 3. ĐƯA VÀO GIỮA DASHBOARD (THAY VÌ POPUP)
                // ==========================================
                // Bóp lấy cái Khung to nhất của màn hình hiện tại (chính là cái Dashboard)
                javafx.scene.Parent currentRoot = btnDetail.getScene().getRoot();

                // TH TRƯỜNG HỢP 1: Nếu Dashboard của ông dùng BorderPane làm gốc
                if (currentRoot instanceof javafx.scene.layout.BorderPane) {
                    javafx.scene.layout.BorderPane dashboard = (javafx.scene.layout.BorderPane) currentRoot;
                    // Ném cái màn hình chi tiết vào phân vùng Center
                    dashboard.setCenter(detailRoot);
                }
                // TH TRƯỜNG HỢP 2: Nếu Dashboard dùng StackPane hoặc layout khác
                else {
                    System.out.println("⚠️ Dashboard không phải BorderPane, đang dùng cách ghi đè cục bộ...");
                    // Chỗ này tùy thuộc vào việc ông thiết kế Dashboard FXML như thế nào.
                    // Nếu code rơi vào dòng này, ông cứ chụp cái file DashboardView.fxml lên đây t chỉ cách nhét chính xác.
                }

            } catch (Exception ex) {
                System.out.println("❌ Lỗi chuyển màn hình: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        card.getChildren().addAll(title, price, statusBadge, infoBox, btnDetail);
        return card;
    }
}