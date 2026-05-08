package com.auctionuet.client.view;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.auctionuet.client.model.AuctionDTO;
import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.network.AuctionClient;
import com.auctionuet.client.network.protocol.AuctionStatus;

public class HomeController {

    // === CÁC Ô THỐNG KÊ TRÊN BANNER ===
    @FXML private Label statAuctionCount;   // Tổng số phiên đấu giá
    @FXML private Label statRunningCount;   // Số phiên đang diễn ra (RUNNING)
    @FXML private Label statFinishedCount;  // Số phiên đã kết thúc (FINISHED)

    // === KHU VỰC "SẮP KẾT THÚC" ===
    @FXML private HBox endingSoonBox;              // Cái hộp chứa các Card sản phẩm sắp hết giờ
    @FXML private Label endingSoonPlaceholder;      // Dòng chữ placeholder "Đang tải..."

    @FXML
    public void initialize() {
        System.out.println("Đã tải màn hình Trang chủ (Home)!");

        // Gọi API lấy dữ liệu thật từ Server
        loadHomeData();
    }

    /**
     * Gọi API GET_AUCTIONS để lấy toàn bộ danh sách phiên đấu giá,
     * sau đó tính thống kê và chọn 3 phiên sắp kết thúc nhất.
     */
    private void loadHomeData() {
        String token = ClientSession.getInstance().getToken();
        if (token == null) {
            statAuctionCount.setText("0");
            statRunningCount.setText("0");
            statFinishedCount.setText("0");
            endingSoonPlaceholder.setText("Vui lòng đăng nhập để xem dữ liệu.");
            return;
        }

        // Chạy trên luồng nền để không đơ giao diện
        new Thread(() -> {
            try {
                AuctionClient client = new AuctionClient();
                List<AuctionDTO> allAuctions = client.getAuctions(token);

                // ── TÍNH THỐNG KÊ ──
                int total = allAuctions.size();
                long running = allAuctions.stream()
                        .filter(a -> a.getStatus() == AuctionStatus.RUNNING)
                        .count();
                long finished = allAuctions.stream()
                        .filter(a -> a.getStatus() == AuctionStatus.FINISHED)
                        .count();

                // ── LỌC 3 PHIÊN SẮP KẾT THÚC (đang RUNNING, sắp xếp theo endTime tăng dần) ──
                List<AuctionDTO> endingSoon = allAuctions.stream()
                        .filter(a -> a.getStatus() == AuctionStatus.RUNNING)
                        .filter(a -> a.getEndTime() != null)
                        .sorted(Comparator.comparing(AuctionDTO::getEndTime))
                        .limit(3)
                        .collect(Collectors.toList());

                // ── CẬP NHẬT GIAO DIỆN (phải dùng Platform.runLater) ──
                Platform.runLater(() -> {
                    // 1. Đắp số thống kê vào 3 ô vuông
                    statAuctionCount.setText(String.valueOf(total));
                    statRunningCount.setText(String.valueOf(running));
                    statFinishedCount.setText(String.valueOf(finished));

                    // 2. Render các card "Sắp kết thúc"
                    renderEndingSoon(endingSoon);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.out.println("❌ Lỗi tải trang chủ: " + e.getMessage());
                    statAuctionCount.setText("–");
                    statRunningCount.setText("–");
                    statFinishedCount.setText("–");
                    endingSoonPlaceholder.setText("Không thể tải dữ liệu. Server có thể đang tắt.");
                });
            }
        }).start();
    }

    /**
     * Render danh sách các Card sản phẩm sắp kết thúc vào HBox.
     * Tạo giao diện giống y hệt AuctionListController.createAuctionCard()
     */
    private void renderEndingSoon(List<AuctionDTO> endingSoon) {
        // Xóa placeholder
        endingSoonBox.getChildren().clear();

        if (endingSoon.isEmpty()) {
            Label empty = new Label("Hiện chưa có phiên đấu giá nào đang diễn ra.");
            empty.setStyle("-fx-font-style: italic; -fx-font-size: 16px;");
            empty.getStyleClass().add("text-secondary");
            endingSoonBox.getChildren().add(empty);
            return;
        }

        for (AuctionDTO auction : endingSoon) {
            endingSoonBox.getChildren().add(createMiniCard(auction));
        }
    }

    /**
     * Tạo một thẻ Card nhỏ gọn cho mục "Sắp kết thúc", giống style của AuctionList.
     */
    private VBox createMiniCard(AuctionDTO auction) {
        VBox card = new VBox(10);
        card.setPrefWidth(220);
        card.getStyleClass().add("card-bg");
        card.setStyle(
            "-fx-background-radius: 12; " +
            "-fx-padding: 20; " +
            "-fx-border-color: #0f3460; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1;"
        );

        // Tiêu đề
        Label title = new Label(auction.getTitle() != null ? auction.getTitle() : "Không có tiêu đề");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        title.getStyleClass().add("text-primary");
        title.setWrapText(true);

        // Giá cao nhất
        Label price = new Label(String.format("💰 %,.0f VNĐ", auction.getCurrentHighestBid()));
        price.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        price.getStyleClass().add("text-accent");

        // Thời gian kết thúc
        String endTime = auction.getEndTime() != null ? auction.getEndTime() : "Chưa rõ";
        Label timeLabel = new Label("⏰ " + endTime);
        timeLabel.setStyle("-fx-font-size: 13px;");
        timeLabel.getStyleClass().add("text-detail");

        // Seller
        String seller = auction.getSellerUsername() != null ? auction.getSellerUsername() : "Chưa rõ";
        Label sellerLabel = new Label("👤 " + seller);
        sellerLabel.setStyle("-fx-font-size: 13px;");
        sellerLabel.getStyleClass().add("text-secondary");

        // Nút xem chi tiết
        Button btnDetail = new Button("Xem chi tiết");
        btnDetail.setMaxWidth(Double.MAX_VALUE);
        btnDetail.setStyle(
            "-fx-background-color: #4ecdc4; " +
            "-fx-text-fill: #1a1a2e; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 8 16; " +
            "-fx-font-size: 13px; " +
            "-fx-cursor: hand;"
        );

        btnDetail.setOnAction(e -> {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                        getClass().getResource("/fxml/AuctionDetailView.fxml"));
                javafx.scene.Parent detailRoot = loader.load();
                AuctionDetailController detailCtrl = loader.getController();
                detailCtrl.setAuctionData(auction.getId());

                // Tìm contentArea (StackPane) trong Dashboard để nhét vào
                javafx.scene.Parent currentRoot = btnDetail.getScene().getRoot();
                if (currentRoot instanceof javafx.scene.layout.BorderPane) {
                    ((javafx.scene.layout.BorderPane) currentRoot).setCenter(detailRoot);
                }
            } catch (Exception ex) {
                System.out.println("❌ Lỗi chuyển trang chi tiết: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        card.getChildren().addAll(title, price, timeLabel, sellerLabel, btnDetail);
        return card;
    }
}
