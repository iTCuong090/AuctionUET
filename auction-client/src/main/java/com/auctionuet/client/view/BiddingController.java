package com.auctionuet.client.view;

import com.auctionuet.client.model.AuctionDTO;
import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.network.AuctionClient;
import com.auctionuet.client.network.BidClient;
import com.auctionuet.client.network.ServerConnection;
import com.auctionuet.client.network.protocol.PushMessage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;

public class BiddingController {
    @FXML private Label titleLabel;
    @FXML private Label priceLabel;
    @FXML private Label leaderLabel;
    @FXML private Label timeLeftLabel;
    @FXML private Label sellerLabel;
    @FXML private Label statusBadge;

    @FXML private Label depositLabel;
    @FXML private Label balanceLabel;

    @FXML private TextField bidAmountField;
    @FXML private Button placeBidBtn;
    @FXML private Label bidStatusLabel;

    @FXML private TextField maxBidField;
    @FXML private TextField incrementField;
    @FXML private Button enableAutoBidBtn;
    @FXML private Button cancelAutoBidBtn;
    @FXML private Label autoBidStatusLabel;

    @FXML private ListView<String> bidHistoryList;

    private String currentAuctionId;
    private final BidClient bidClient = new BidClient();
    private final AuctionClient auctionClient = new AuctionClient();
    
    private javafx.animation.Timeline countdownTimeline;
    private java.time.LocalDateTime endDateTime;

    public void setAuctionId(String auctionId) {
        this.currentAuctionId = auctionId;

        // 1. Load dữ liệu ban đầu
        loadAuctionDetail();
        loadBidHistory();

        // 2. SUBSCRIBE phiên đấu giá
        subscribeToAuction(auctionId);

        // 3. Đăng ký listener nhận push message từ ServerConnection
        ServerConnection.getInstance().setPushListener(this::onPushMessage);
        
        // 4. Kiểm tra trạng thái Auto-Bid hiện tại
        checkAutoBidState();
    }

    private void loadAuctionDetail() {
        String token = ClientSession.getInstance().getToken();
        new Thread(() -> {
            try {
                AuctionDTO auction = auctionClient.getAuctionDetail(token, currentAuctionId);
                Platform.runLater(() -> {
                    titleLabel.setText("📦 " + auction.getTitle());
                    sellerLabel.setText("👤 Seller: " + auction.getSellerUsername());
                    priceLabel.setText(String.format("💰 %,.0f VNĐ", auction.getCurrentHighestBid()));
                    
                    if (auction.getEndTime() != null) {
                        try {
                            endDateTime = java.time.LocalDateTime.parse(auction.getEndTime());
                            startCountdown();
                        } catch (Exception e) {}
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> titleLabel.setText("❌ Lỗi tải chi tiết: " + e.getMessage()));
            }
        }).start();
    }

    private void loadBidHistory() {
        String token = ClientSession.getInstance().getToken();
        new Thread(() -> {
            try {
                List<Map<String, Object>> history = bidClient.getBidHistory(token, currentAuctionId);
                Platform.runLater(() -> {
                    bidHistoryList.getItems().clear();
                    if (history != null && !history.isEmpty()) {
                        for (Map<String, Object> entry : history) {
                            String bidder = (String) entry.getOrDefault("bidderUsername", "Unknown");
                            Double amount = (Double) entry.getOrDefault("amount", 0.0);
                            String time = (String) entry.getOrDefault("time", "");
                            bidHistoryList.getItems().add(time + " - " + bidder + " - " + String.format("%,.0f VNĐ", amount));
                        }
                        
                        // Set leader as the most recent bid
                        Map<String, Object> latest = history.get(0);
                        leaderLabel.setText("👑 " + latest.getOrDefault("bidderUsername", "Unknown"));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> bidHistoryList.getItems().add("❌ Lỗi tải lịch sử"));
            }
        }).start();
    }

    private void onPushMessage(PushMessage push) {
        if (!currentAuctionId.equals(push.getAuctionId())) return;

        Platform.runLater(() -> {
            switch (push.getPushType()) {
                case "BID_UPDATE":
                    priceLabel.setText(String.format("💰 %,.0f VNĐ", push.getAmount()));
                    leaderLabel.setText("👑 " + push.getBidderUsername());
                    bidHistoryList.getItems().add(0, "Vừa xong - " + push.getBidderUsername() + " - " + String.format("%,.0f VNĐ", push.getAmount()));
                    break;

                case "AUCTION_EXTENDED":
                    try {
                        endDateTime = java.time.LocalDateTime.parse(push.getNewEndTime());
                        startCountdown();
                    } catch (Exception e) {}
                    break;

                case "AUCTION_ENDED":
                    if (countdownTimeline != null) countdownTimeline.stop();
                    timeLeftLabel.setText("⏰ Đã kết thúc");
                    statusBadge.setText("🔴 ĐÃ KẾT THÚC");
                    statusBadge.getStyleClass().remove("status-badge-running");
                    statusBadge.getStyleClass().add("status-badge-finished");
                    leaderLabel.setText("🏆 Winner: " + push.getWinnerId() + " (Giá: " + String.format("%,.0f VNĐ", push.getFinalPrice()) + ")");
                    
                    bidAmountField.setDisable(true);
                    placeBidBtn.setDisable(true);
                    enableAutoBidBtn.setDisable(true);
                    cancelAutoBidBtn.setDisable(true);
                    break;
            }
        });
    }

    private void subscribeToAuction(String auctionId) {
        new Thread(() -> {
            try {
                bidClient.subscribe(ClientSession.getInstance().getToken(), auctionId);
            } catch (Exception e) {
                Platform.runLater(() -> bidStatusLabel.setText("❌ Lỗi subscribe: " + e.getMessage()));
            }
        }).start();
    }

    public void cleanup() {
        if (countdownTimeline != null) countdownTimeline.stop();
        ServerConnection.getInstance().setPushListener(null);
        new Thread(() -> {
            try {
                bidClient.unsubscribe(ClientSession.getInstance().getToken(), currentAuctionId);
            } catch (Exception ignored) {}
        }).start();
    }
    
    private void startCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        countdownTimeline = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
            long secs = java.time.Duration.between(java.time.LocalDateTime.now(), endDateTime).getSeconds();
            if (secs <= 0) {
                timeLeftLabel.setText("⏰ Đã kết thúc");
                countdownTimeline.stop();
            } else {
                long h = secs / 3600;
                long m = (secs % 3600) / 60;
                long s = secs % 60;
                timeLeftLabel.setText(String.format("⏰ Còn: %02d:%02d:%02d", h, m, s));
            }
        }));
        countdownTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        countdownTimeline.play();
    }

    private void checkAutoBidState() {
        String token = ClientSession.getInstance().getToken();
        new Thread(() -> {
            try {
                Map<String, Object> state = bidClient.checkAutoBid(token, currentAuctionId);
                Platform.runLater(() -> {
                    if (state != null) {
                        enableAutoBidBtn.setDisable(true);
                        cancelAutoBidBtn.setDisable(false);
                        maxBidField.setText(String.format("%.0f", ((Number)state.get("maxBid")).doubleValue()));
                        incrementField.setText(String.format("%.0f", ((Number)state.get("increment")).doubleValue()));
                        autoBidStatusLabel.setText("✅ Đang bật Auto-Bid");
                        autoBidStatusLabel.setStyle("-fx-text-fill: #22c55e;");
                    } else {
                        enableAutoBidBtn.setDisable(false);
                        cancelAutoBidBtn.setDisable(true);
                    }
                });
            } catch (Exception e) {}
        }).start();
    }

    @FXML
    private void handlePlaceBid() {
        String amountText = bidAmountField.getText().replace(",", "").trim();
        if (amountText.isEmpty()) {
            bidStatusLabel.setText("❌ Vui lòng nhập số tiền.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);
            String token = ClientSession.getInstance().getToken();
            
            bidStatusLabel.setText("⏳ Đang xử lý...");
            bidStatusLabel.setStyle("-fx-text-fill: #f39c12;");

            new Thread(() -> {
                try {
                    bidClient.placeBid(token, currentAuctionId, amount);
                    Platform.runLater(() -> {
                        bidStatusLabel.setText("✅ Đặt giá thành công!");
                        bidStatusLabel.setStyle("-fx-text-fill: #22c55e;");
                        bidAmountField.clear();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        bidStatusLabel.setText("❌ Lỗi: " + e.getMessage());
                        bidStatusLabel.setStyle("-fx-text-fill: #dc2626;");
                    });
                }
            }).start();
        } catch (NumberFormatException ex) {
            bidStatusLabel.setText("❌ Số tiền không hợp lệ.");
            bidStatusLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    @FXML
    private void handleSetAutoBid() {
        String maxText = maxBidField.getText().replace(",", "").trim();
        String incText = incrementField.getText().replace(",", "").trim();

        if (maxText.isEmpty() || incText.isEmpty()) {
            autoBidStatusLabel.setText("❌ Vui lòng nhập đủ thông tin.");
            return;
        }

        try {
            double maxBid = Double.parseDouble(maxText);
            double increment = Double.parseDouble(incText);
            String token = ClientSession.getInstance().getToken();

            autoBidStatusLabel.setText("⏳ Đang thiết lập...");
            
            new Thread(() -> {
                try {
                    bidClient.setAutoBid(token, currentAuctionId, maxBid, increment);
                    Platform.runLater(() -> {
                        autoBidStatusLabel.setText("✅ Đã bật Auto-Bid!");
                        autoBidStatusLabel.setStyle("-fx-text-fill: #22c55e;");
                        enableAutoBidBtn.setDisable(true);
                        cancelAutoBidBtn.setDisable(false);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        autoBidStatusLabel.setText("❌ Lỗi: " + e.getMessage());
                        autoBidStatusLabel.setStyle("-fx-text-fill: #dc2626;");
                    });
                }
            }).start();
        } catch (NumberFormatException ex) {
            autoBidStatusLabel.setText("❌ Số tiền không hợp lệ.");
        }
    }

    @FXML
    private void handleCancelAutoBid() {
        String token = ClientSession.getInstance().getToken();
        new Thread(() -> {
            try {
                bidClient.cancelAutoBid(token, currentAuctionId);
                Platform.runLater(() -> {
                    autoBidStatusLabel.setText("✅ Đã hủy Auto-Bid.");
                    autoBidStatusLabel.setStyle("-fx-text-fill: #f39c12;");
                    enableAutoBidBtn.setDisable(false);
                    cancelAutoBidBtn.setDisable(true);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    autoBidStatusLabel.setText("❌ Lỗi hủy Auto-Bid: " + e.getMessage());
                    autoBidStatusLabel.setStyle("-fx-text-fill: #dc2626;");
                });
            }
        }).start();
    }

    @FXML
    private void handleBack() {
        cleanup(); // Rất quan trọng: Hủy đăng ký lắng nghe Push trước khi rời đi
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/AuctionListView.fxml"));
            Parent listRoot = loader.load();
            
            // Tìm Dashboard HBox/StackPane
            Parent currentRoot = titleLabel.getScene().getRoot();
            if (currentRoot instanceof HBox) {
                // Trong DashboardView, contentArea nằm trong main-card VBox
                VBox mainCard = (VBox) ((HBox) currentRoot).getChildren().get(1);
                javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) mainCard.getChildren().get(1);
                contentArea.getChildren().setAll(listRoot);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
