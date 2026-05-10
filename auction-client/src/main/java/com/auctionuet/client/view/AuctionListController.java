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
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold;");
        title.getStyleClass().add("text-primary");
        title.setWrapText(true);

        Label price = new Label(String.format("💰 %,.0f VNĐ", item.getCurrentHighestBid()));
        price.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        price.getStyleClass().add("text-accent");

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
        String endTime = "Chưa rõ";
        if (item.getEndTime() != null) {
            try {
                java.time.LocalDateTime dt = java.time.LocalDateTime.parse(item.getEndTime());
                endTime = dt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            } catch (Exception e) {
                endTime = item.getEndTime();
            }
        }
        String seller = item.getSellerUsername() != null ? item.getSellerUsername() : "Chưa rõ";

        Label timeLabel = new Label("⏰ Kết thúc: " + endTime);
        Label sellerLabel = new Label("👤 Seller: " + seller);

        String subStyle = "-fx-font-size: 13px;";
        timeLabel.setStyle(subStyle);
        timeLabel.getStyleClass().add("text-secondary");
        sellerLabel.setStyle(subStyle);
        sellerLabel.getStyleClass().add("text-secondary");

        infoBox.getChildren().addAll(timeLabel, sellerLabel);

        Button btnDetail = new Button("Xem chi tiết");
        btnDetail.getStyleClass().add("button");
        btnDetail.setMaxWidth(Double.MAX_VALUE);

        btnDetail.setOnAction(e -> {
            try {
                // Nếu trạng thái là RUNNING, vào màn hình Đấu Giá Realtime (BiddingView)
                // Nếu là OPEN hoặc FINISHED, vào màn hình Chi Tiết tĩnh (AuctionDetailView)
                String fxmlPath = "RUNNING".equals(statusStr) ? "/fxml/BiddingView.fxml" : "/fxml/AuctionDetailView.fxml";
                
                // 1. Tải bản vẽ của màn hình
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(fxmlPath));
                javafx.scene.Parent root = loader.load();

                // 2. Tóm lấy thằng Quản lý và truyền ID
                if ("RUNNING".equals(statusStr)) {
                    BiddingController biddingController = loader.getController();
                    biddingController.setAuctionId(item.getId());
                } else {
                    AuctionDetailController detailController = loader.getController();
                    detailController.setAuctionData(item.getId());
                }

                // ==========================================
                // 3. ĐƯA VÀO GIỮA DASHBOARD (THAY VÌ POPUP)
                // ==========================================
                // Bóp lấy cái Khung to nhất của màn hình hiện tại (chính là cái Dashboard)
                javafx.scene.Parent currentRoot = btnDetail.getScene().getRoot();

                // TH TRƯỜNG HỢP 1: Nếu Dashboard của ông dùng HBox làm gốc (DashboardView.fxml)
                if (currentRoot instanceof javafx.scene.layout.HBox) {
                    javafx.scene.layout.VBox mainCard = (javafx.scene.layout.VBox) ((javafx.scene.layout.HBox) currentRoot).getChildren().get(1);
                    javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) mainCard.getChildren().get(1);
                    contentArea.getChildren().setAll(root);
                }
                // TH TRƯỜNG HỢP 2: Nếu Dashboard dùng BorderPane
                else if (currentRoot instanceof javafx.scene.layout.BorderPane) {
                    javafx.scene.layout.BorderPane dashboard = (javafx.scene.layout.BorderPane) currentRoot;
                    dashboard.setCenter(root);
                }
                else {
                    System.out.println("⚠️ Dashboard layout không khớp (không phải HBox hay BorderPane).");
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