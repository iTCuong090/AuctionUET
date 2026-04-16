package com.auctionuet.client.view;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import java.util.ArrayList;
import java.util.List;

public class AuctionListController {

    @FXML private FlowPane auctionGrid;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private TextField searchField;

    @FXML
    public void initialize() {
        // 1. Setup thanh lọc
        filterComboBox.getItems().addAll("Tất cả", "OPEN", "RUNNING", "FINISHED");
        filterComboBox.setValue("Tất cả");

        // 2. Load data (Giả lập luồng mạng Tuần 4)
        loadAuctionsFromServer();
    }

    private void loadAuctionsFromServer() {
        new Thread(() -> {
            try {
                // Giả lập delay mạng
                Thread.sleep(600);

                // Mock Data: Nửa cuối tuần ông sẽ thay bằng AuctionClient.getAuctions()
                List<MockAuctionDTO> list = new ArrayList<>();
                list.add(new MockAuctionDTO("1", "📱 iPhone 15 Pro Max", 25000000, "RUNNING", "02:30:15", "seller_cuong", 12));
                list.add(new MockAuctionDTO("2", "🎨 Bức tranh Mona Lisa", 500000000, "OPEN", "Chưa bắt đầu", "anh_art", 0));
                list.add(new MockAuctionDTO("3", "🚗 Toyota Camry 2024", 900000000, "RUNNING", "10:05:00", "admin_auto", 5));
                list.add(new MockAuctionDTO("4", "💻 Macbook Pro M3", 40000000, "FINISHED", "Đã bán", "seller_cuong", 25));
                list.add(new MockAuctionDTO("5", "🎧 Tai nghe Sony XM5", 5000000, "RUNNING", "00:15:30", "hoan_music", 8));
                list.add(new MockAuctionDTO("6", "⌚ Apple Watch Ultra", 15000000, "CANCELED", "Đã hủy", "seller_k", 2));

                Platform.runLater(() -> renderGrid(list));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void renderGrid(List<MockAuctionDTO> auctions) {
        auctionGrid.getChildren().clear();
        for (MockAuctionDTO item : auctions) {
            auctionGrid.getChildren().add(createAuctionCard(item));
        }
    }

    // --- HÀM ĐÚC THẺ (CARD FACTORY) ---
    private VBox createAuctionCard(MockAuctionDTO item) {
        VBox card = new VBox(12);
        card.getStyleClass().add("auction-card"); // Ăn CSS: nhô lên khi hover
        card.setPrefWidth(250);

        // 1. Tiêu đề sản phẩm
        Label title = new Label(item.title);
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: white;");
        title.setWrapText(true);

        // 2. Giá hiện tại
        Label price = new Label(String.format("💰 %,d VNĐ", item.price));
        price.setStyle("-fx-font-size: 15px; -fx-text-fill: #4ecdc4; -fx-font-weight: bold;");

        // 3. Huy hiệu trạng thái (Sử dụng các class CSS vừa thêm)
        Label statusBadge = new Label(item.status);
        switch (item.status) {
            case "RUNNING" -> {
                statusBadge.setText("🟢 " + item.status);
                statusBadge.getStyleClass().add("status-badge-running");
            }
            case "OPEN" -> {
                statusBadge.setText("🟡 " + item.status);
                statusBadge.getStyleClass().add("status-badge-open");
            }
            case "FINISHED" -> {
                statusBadge.setText("🔴 " + item.status);
                statusBadge.getStyleClass().add("status-badge-finished");
            }
            default -> {
                statusBadge.setText("⚫ " + item.status);
                statusBadge.getStyleClass().add("status-badge-canceled");
            }
        }

        // 4. Thông tin phụ (Time, Seller, Bids)
        VBox infoBox = new VBox(5);
        Label timeLabel = new Label("⏰ " + item.timeLeft);
        Label sellerLabel = new Label("👤 Seller: " + item.seller);
        Label bidsLabel = new Label("🔥 " + item.bids + " lượt đấu giá");

        String subStyle = "-fx-text-fill: #a0a0b0; -fx-font-size: 13px;";
        timeLabel.setStyle(subStyle);
        sellerLabel.setStyle(subStyle);
        bidsLabel.setStyle("-fx-text-fill: #e94560; -fx-font-weight: bold; -fx-font-size: 13px;");

        infoBox.getChildren().addAll(timeLabel, sellerLabel, bidsLabel);

        // 5. Nút bấm
        Button btnDetail = new Button("Xem chi tiết");
        btnDetail.getStyleClass().add("button"); // Ăn CSS Gradient
        btnDetail.setMaxWidth(Double.MAX_VALUE);
        btnDetail.setOnAction(e -> {
            System.out.println("Mở Auction ID: " + item.id);
            // Tuần sau gọi SceneManager.switchScene("/fxml/AuctionDetailView.fxml")
        });

        card.getChildren().addAll(title, price, statusBadge, infoBox, btnDetail);
        return card;
    }

    // Class dữ liệu tạm thời (Nửa sau tuần 4 thay bằng AuctionDTO của thằng Anh)
    private static class MockAuctionDTO {
        String id, title, status, timeLeft, seller;
        long price;
        int bids;

        public MockAuctionDTO(String id, String title, long price, String status, String timeLeft, String seller, int bids) {
            this.id = id; this.title = title; this.price = price;
            this.status = status; this.timeLeft = timeLeft;
            this.seller = seller; this.bids = bids;
        }
    }
}