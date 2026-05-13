package com.auctionuet.client.view;

import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.network.AuctionClient;
import com.auctionuet.client.network.BidClient;
import com.auctionuet.client.network.ServerConnection;
import com.auctionuet.protocol.PushActionType;
import com.auctionuet.protocol.PushMessage;
import com.auctionuet.protocol.dto.push.PushEvents;
import com.auctionuet.protocol.dto.response.auction.AuctionDTO;
import com.auctionuet.protocol.dto.response.bid.AutoBidConfigDTO;
import com.auctionuet.protocol.dto.response.bid.BidDTO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BiddingController {
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

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
    private LocalDateTime endDateTime;

    public void setAuctionId(String auctionId) {
        this.currentAuctionId = auctionId;

        loadAuctionDetail();
        loadBidHistory();
        subscribeToAuction(auctionId);
        ServerConnection.getInstance().setPushListener(this::onPushMessage);
        checkAutoBidState();
    }

    private void loadAuctionDetail() {
        String token = ClientSession.getInstance().getToken();
        new Thread(() -> {
            try {
                AuctionDTO auction = auctionClient.getAuctionDetail(token, currentAuctionId);
                Platform.runLater(() -> {
                    titleLabel.setText(auction.getTitle());
                    sellerLabel.setText("Seller: " + auction.getSellerUsername());
                    priceLabel.setText(String.format("%,.0f VND", auction.getCurrentHighestBid()));

                    if (auction.getEndTime() != null) {
                        endDateTime = auction.getEndTime();
                        startCountdown();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> titleLabel.setText("Loi tai chi tiet: " + e.getMessage()));
            }
        }).start();
    }

    private void loadBidHistory() {
        String token = ClientSession.getInstance().getToken();
        new Thread(() -> {
            try {
                List<BidDTO> history = bidClient.getBidHistory(token, currentAuctionId);
                Platform.runLater(() -> {
                    bidHistoryList.getItems().clear();
                    if (history != null && !history.isEmpty()) {
                        for (BidDTO entry : history) {
                            bidHistoryList.getItems().add(formatBid(entry));
                        }

                        BidDTO latest = history.get(0);
                        leaderLabel.setText("Leader: " + latest.getBidderUsername());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> bidHistoryList.getItems().add("Loi tai lich su"));
            }
        }).start();
    }

    private void onPushMessage(PushMessage push) {
        if (push == null || push.getPushType() == null) return;

        Platform.runLater(() -> {
            if (push.getPushType() == PushActionType.BID_UPDATE) {
                PushEvents.BidUpdatePush data = push.getDataAs(PushEvents.BidUpdatePush.class);
                if (data == null || !currentAuctionId.equals(data.getAuctionId())) return;

                priceLabel.setText(String.format("%,.0f VND", data.getAmount()));
                leaderLabel.setText("Leader: " + data.getBidderUsername());
                bidHistoryList.getItems().add(0, "Vua xong - " + data.getBidderUsername()
                        + " - " + String.format("%,.0f VND", data.getAmount()));
                return;
            }

            if (push.getPushType() == PushActionType.AUCTION_EXTENDED) {
                PushEvents.AuctionExtendedPush data = push.getDataAs(PushEvents.AuctionExtendedPush.class);
                if (data == null || !currentAuctionId.equals(data.getAuctionId())) return;

                try {
                    endDateTime = LocalDateTime.parse(data.getNewEndTime());
                    startCountdown();
                } catch (Exception ignored) {}
                return;
            }

            if (push.getPushType() == PushActionType.AUCTION_ENDED) {
                PushEvents.AuctionEndedPush data = push.getDataAs(PushEvents.AuctionEndedPush.class);
                if (data == null || !currentAuctionId.equals(data.getAuctionId())) return;

                if (countdownTimeline != null) countdownTimeline.stop();
                timeLeftLabel.setText("Da ket thuc");
                statusBadge.setText("DA KET THUC");
                statusBadge.getStyleClass().remove("status-badge-running");
                statusBadge.getStyleClass().add("status-badge-finished");
                leaderLabel.setText("Winner: " + data.getWinner()
                        + " (Gia: " + String.format("%,.0f VND", data.getFinalPrice()) + ")");

                bidAmountField.setDisable(true);
                placeBidBtn.setDisable(true);
                enableAutoBidBtn.setDisable(true);
                cancelAutoBidBtn.setDisable(true);
            }
        });
    }

    private void subscribeToAuction(String auctionId) {
        new Thread(() -> {
            try {
                bidClient.subscribe(ClientSession.getInstance().getToken(), auctionId);
            } catch (Exception e) {
                Platform.runLater(() -> bidStatusLabel.setText("Loi subscribe: " + e.getMessage()));
            }
        }).start();
    }

    public void cleanup() {
        if (countdownTimeline != null) countdownTimeline.stop();
        ServerConnection.getInstance().setPushListener(null);
        new Thread(() -> bidClient.unsubscribe(ClientSession.getInstance().getToken(), currentAuctionId)).start();
    }

    private void startCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        countdownTimeline = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
            long secs = Duration.between(LocalDateTime.now(), endDateTime).getSeconds();
            if (secs <= 0) {
                timeLeftLabel.setText("Da ket thuc");
                countdownTimeline.stop();
            } else {
                long h = secs / 3600;
                long m = (secs % 3600) / 60;
                long s = secs % 60;
                timeLeftLabel.setText(String.format("Con: %02d:%02d:%02d", h, m, s));
            }
        }));
        countdownTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        countdownTimeline.play();
    }

    private void checkAutoBidState() {
        String token = ClientSession.getInstance().getToken();
        new Thread(() -> {
            try {
                AutoBidConfigDTO state = bidClient.checkAutoBid(token, currentAuctionId);
                Platform.runLater(() -> {
                    if (state != null) {
                        enableAutoBidBtn.setDisable(true);
                        cancelAutoBidBtn.setDisable(false);
                        maxBidField.setText(String.format("%.0f", state.getMaxBid()));
                        incrementField.setText(String.format("%.0f", state.getIncrement()));
                        autoBidStatusLabel.setText("Dang bat Auto-Bid");
                        autoBidStatusLabel.setStyle("-fx-text-fill: #22c55e;");
                    } else {
                        enableAutoBidBtn.setDisable(false);
                        cancelAutoBidBtn.setDisable(true);
                    }
                });
            } catch (Exception ignored) {}
        }).start();
    }

    @FXML
    private void handlePlaceBid() {
        String amountText = bidAmountField.getText().replace(",", "").trim();
        if (amountText.isEmpty()) {
            bidStatusLabel.setText("Vui long nhap so tien.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);
            String token = ClientSession.getInstance().getToken();

            bidStatusLabel.setText("Dang xu ly...");
            bidStatusLabel.setStyle("-fx-text-fill: #f39c12;");

            new Thread(() -> {
                try {
                    bidClient.placeBid(token, currentAuctionId, amount);
                    Platform.runLater(() -> {
                        bidStatusLabel.setText("Dat gia thanh cong!");
                        bidStatusLabel.setStyle("-fx-text-fill: #22c55e;");
                        bidAmountField.clear();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        bidStatusLabel.setText("Loi: " + e.getMessage());
                        bidStatusLabel.setStyle("-fx-text-fill: #dc2626;");
                    });
                }
            }).start();
        } catch (NumberFormatException ex) {
            bidStatusLabel.setText("So tien khong hop le.");
            bidStatusLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    @FXML
    private void handleSetAutoBid() {
        String maxText = maxBidField.getText().replace(",", "").trim();
        String incText = incrementField.getText().replace(",", "").trim();

        if (maxText.isEmpty() || incText.isEmpty()) {
            autoBidStatusLabel.setText("Vui long nhap du thong tin.");
            return;
        }

        try {
            double maxBid = Double.parseDouble(maxText);
            double increment = Double.parseDouble(incText);
            String token = ClientSession.getInstance().getToken();

            autoBidStatusLabel.setText("Dang thiet lap...");

            new Thread(() -> {
                try {
                    bidClient.setAutoBid(token, currentAuctionId, maxBid, increment);
                    Platform.runLater(() -> {
                        autoBidStatusLabel.setText("Da bat Auto-Bid!");
                        autoBidStatusLabel.setStyle("-fx-text-fill: #22c55e;");
                        enableAutoBidBtn.setDisable(true);
                        cancelAutoBidBtn.setDisable(false);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        autoBidStatusLabel.setText("Loi: " + e.getMessage());
                        autoBidStatusLabel.setStyle("-fx-text-fill: #dc2626;");
                    });
                }
            }).start();
        } catch (NumberFormatException ex) {
            autoBidStatusLabel.setText("So tien khong hop le.");
        }
    }

    @FXML
    private void handleCancelAutoBid() {
        String token = ClientSession.getInstance().getToken();
        new Thread(() -> {
            try {
                bidClient.cancelAutoBid(token, currentAuctionId);
                Platform.runLater(() -> {
                    autoBidStatusLabel.setText("Da huy Auto-Bid.");
                    autoBidStatusLabel.setStyle("-fx-text-fill: #f39c12;");
                    enableAutoBidBtn.setDisable(false);
                    cancelAutoBidBtn.setDisable(true);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    autoBidStatusLabel.setText("Loi huy Auto-Bid: " + e.getMessage());
                    autoBidStatusLabel.setStyle("-fx-text-fill: #dc2626;");
                });
            }
        }).start();
    }

    @FXML
    private void handleBack() {
        cleanup();
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/AuctionListView.fxml"));
            Parent listRoot = loader.load();

            Parent currentRoot = titleLabel.getScene().getRoot();
            if (currentRoot instanceof HBox) {
                VBox mainCard = (VBox) ((HBox) currentRoot).getChildren().get(1);
                javafx.scene.layout.StackPane contentArea =
                        (javafx.scene.layout.StackPane) mainCard.getChildren().get(1);
                contentArea.getChildren().setAll(listRoot);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String formatBid(BidDTO bid) {
        String time = bid.getTimestamp() != null ? bid.getTimestamp().format(DISPLAY_TIME) : "";
        return time + " - " + bid.getBidderUsername() + " - " + String.format("%,.0f VND", bid.getAmount());
    }
}
