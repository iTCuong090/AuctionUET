package com.auctionuet.client.view;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.chart.LineChart;

import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.model.UserDTO;
import com.auctionuet.client.model.AuctionDTO;
import com.auctionuet.client.network.AuctionClient;

public class AuctionDetailController {

    // 1. CÁC BIẾN CŨ
    @FXML
    private Label nameLabel, sellerLabel, typeLabel, specLabel, conditionLabel, descriptionLabel;
    @FXML
    private Label currentPriceLabel, leaderLabel, timeLeftLabel, statusLabel;
    @FXML
    private ImageView productImageView;

    // 2. CÁC BIẾN MỚI (Lịch sử & Biểu đồ)
    // Tạm thời dùng <?> vì anh em mình chưa tạo file BidDTO cho lịch sử
    @FXML
    private LineChart<String, Number> priceChart;
    @FXML
    private TableView<?> bidHistoryTable;
    @FXML
    private TableColumn<?, ?> bidderCol, amountCol, timeCol;

    // 3. VÙNG TƯƠNG TÁC
    @FXML
    private Button btnStartAuction;
    @FXML
    private HBox biddingArea;
    @FXML
    private TextField bidAmountField;
    @FXML
    private Button btnPlaceBid;
    @FXML
    private Label bidErrorLabel;

    private String currentAuctionId; // Lưu lại ID phiên đang xem

    @FXML
    public void initialize() {
        // Gắn sự kiện cho nút "Ra giá" (Vì trong FXML chưa gắn onAction)
        btnPlaceBid.setOnAction(e -> handlePlaceBid());
    }

    // Đón dữ liệu từ màn hình khác truyền sang
    public void setAuctionData(String auctionId) {
        this.currentAuctionId = auctionId;
        String token = ClientSession.getInstance().getToken();

        // Mặc định giấu sạch sành sanh vùng tương tác đi cho an toàn
        btnStartAuction.setVisible(false);
        btnStartAuction.setManaged(false);
        biddingArea.setVisible(false);
        biddingArea.setManaged(false);
        bidErrorLabel.setVisible(false);

        // Gọi mạng lấy thông tin chi tiết
        new Thread(() -> {
            try {
                AuctionClient client = new AuctionClient();
                AuctionDTO dto = client.getAuctionDetail(token, auctionId);

                Platform.runLater(() -> updateUI(dto));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("❌ Lỗi lấy chi tiết: " + e.getMessage());
                    statusLabel.setTextFill(javafx.scene.paint.Color.RED);
                });
            }
        }).start();
    }

    // Hàm phụ trợ đẩy dữ liệu lên Giao diện
    private void updateUI(AuctionDTO dto) {
        // 1. Điền thông tin cơ bản
        nameLabel.setText(dto.getTitle());
        sellerLabel.setText("👤 Seller: " + dto.getSellerUsername());
        currentPriceLabel.setText(String.format("💰 Giá hiện tại: %,.0f VNĐ", dto.getCurrentHighestBid()));

        String currentStatus = dto.getStatus() != null ? dto.getStatus().name() : "UNKNOWN";
        statusLabel.setText("📊 Trạng thái: " + currentStatus);
        timeLeftLabel.setText("⏰ Kết thúc: " + dto.getEndTime());

        // Chỗ này sau này nếu Server trả về Leader, Spec, Condition thì ông setText vào
        // đây.
        // leaderLabel.setText("👑 Người dẫn đầu: " + dto.getLeaderUsername());

        // 2. MA THUẬT PHÂN QUYỀN (SELLER vs BIDDER)
        UserDTO currentUser = ClientSession.getInstance().getCurrentUser();
        String currentUsername = (currentUser != null) ? currentUser.getUsername() : "";

        // Kiểm tra xem người đang cầm chuột có phải là chủ món hàng này không?
        boolean isMyItem = currentUsername.equals(dto.getSellerUsername());

        if (isMyItem) {
            // NẾU LÀ CHỦ HÀNG: Chỉ hiện nút Start khi phiên đang OPEN
            if ("OPEN".equals(currentStatus)) {
                btnStartAuction.setVisible(true);
                btnStartAuction.setManaged(true);
            }
        } else {
            // NẾU LÀ KHÁCH MUA (BIDDER): Chỉ hiện ô Nhập tiền khi phiên đang RUNNING
            if ("RUNNING".equals(currentStatus)) {
                biddingArea.setVisible(true);
                biddingArea.setManaged(true);
            }
        }
    }

    @FXML
    private void handleStartAuction() {
        String token = ClientSession.getInstance().getToken();
        btnStartAuction.setDisable(true);
        btnStartAuction.setText("⏳ Đang xử lý...");

        // Gọi API mở phiên
        new Thread(() -> {
            try {
                AuctionClient client = new AuctionClient();
                client.startAuction(token, currentAuctionId);

                Platform.runLater(() -> {
                    statusLabel.setText("📊 Trạng thái: RUNNING");
                    btnStartAuction.setVisible(false); // Bắt đầu rồi thì giấu nút đi
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnStartAuction.setText("❌ Lỗi, bấm thử lại");
                    btnStartAuction.setDisable(false);
                });
            }
        }).start();
    }

    // Tính năng mới: Xử lý khi khách bấm nút "Ra giá"
    private void handlePlaceBid() {
        String amountText = bidAmountField.getText();

        // 1. Kiểm tra khách có nhập linh tinh chữ cái không
        try {
            double bidAmount = Double.parseDouble(amountText);

            if (bidAmount <= 0) {
                showBidError("Giá phải lớn hơn 0!");
                return;
            }

            // 2. Chỗ này sẽ gọi luồng Mạng (Thread) bắn API PlaceBid lên Server
            showBidError("⏳ Đang gửi giá...", javafx.scene.paint.Color.ORANGE);

            // TODO: Tạo API PlaceBid trong AuctionClient và gọi ở đây (sẽ làm ở bước sau)
            System.out.println("Sắp gửi giá: " + bidAmount + " cho ID: " + currentAuctionId);

        } catch (NumberFormatException ex) {
            showBidError("❌ Lỗi: Vui lòng nhập số hợp lệ!");
        }
    }

    // Hàm tiện ích để hiện màu chữ báo lỗi cho ô Ra giá
    private void showBidError(String message) {
        showBidError(message, javafx.scene.paint.Color.web("#ff4757")); // Màu đỏ mặc định
    }

    private void showBidError(String message, javafx.scene.paint.Color color) {
        bidErrorLabel.setText(message);
        bidErrorLabel.setTextFill(color);
        bidErrorLabel.setVisible(true);
    }
}