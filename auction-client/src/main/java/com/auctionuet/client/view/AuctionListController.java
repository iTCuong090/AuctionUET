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
                // 1. Tải bản vẽ của màn hình Chi tiết (File FXML)
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/AuctionDetailView.fxml"));
                javafx.scene.Parent root = loader.load();

                // 2. Tóm lấy thằng Quản lý của phòng VIP (AuctionDetailController)
                AuctionDetailController detailController = loader.getController();

                // 3. TRUYỀN ID SANG PHÒNG VIP!
                // (Chính hành động này sẽ làm cái hàm màu xám bên kia SÁNG LÊN!)
                detailController.setAuctionData(item.getId());

                // 4. Đổi ruột màn hình hiện tại thành màn hình Chi tiết
                btnDetail.getScene().setRoot(root);

            } catch (Exception ex) {
                System.out.println("❌ Lỗi chuyển màn hình: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        card.getChildren().addAll(title, price, statusBadge, infoBox, btnDetail);
        return card;
    }
}