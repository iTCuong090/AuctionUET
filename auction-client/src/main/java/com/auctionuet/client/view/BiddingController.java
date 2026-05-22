package com.auctionuet.client.view;

import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.network.AuctionClient;
import com.auctionuet.client.network.BidClient;
import com.auctionuet.client.network.ServerConnection;
import com.auctionuet.client.network.WalletClient;
import com.auctionuet.protocol.PushActionType;
import com.auctionuet.protocol.PushMessage;
import com.auctionuet.protocol.dto.push.PushEvents;
import com.auctionuet.protocol.dto.response.auction.AuctionDTO;
import com.auctionuet.protocol.dto.response.bid.AutoBidConfigDTO;
import com.auctionuet.protocol.dto.response.bid.BidDTO;
import com.auctionuet.protocol.dto.response.user.UserDTO;
import com.auctionuet.protocol.dto.response.wallet.WalletResponseDTO;
import com.auctionuet.protocol.enums.AutoBidStatus;
import com.auctionuet.protocol.enums.BidType;
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
    private static final String AUTO_BID_INEFFECTIVE_NOTICE =
            "Auto-Bid của bạn không còn hiệu lực vì đã bị Auto-Bid khác vượt trần.";

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
    private final WalletClient walletClient = new WalletClient();

    private javafx.animation.Timeline countdownTimeline;
    private LocalDateTime endDateTime;
    private double auctionDepositAmount;
    private boolean currentUserDeposited;
    private AutoBidStatus lastAutoBidStatus;
    private boolean autoBidStateLoaded;
    private boolean pendingAutoBidLossNotice;

    public void setAuctionId(String auctionId) {
        this.currentAuctionId = auctionId;

        loadAuctionDetail();
        loadWalletInfo();
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
                    sellerLabel.setText("Seller: " + usernameOf(auction.getSeller()));
                    priceLabel.setText(CurrencyFormatter.format(auction.getCurrentPrice()));
                    auctionDepositAmount = auction.getDepositAmount() > 0
                            ? auction.getDepositAmount()
                            : auction.getItem() != null ? auction.getItem().getStartingPrice() * 0.10 : 0;
                    currentUserDeposited = auction.isCurrentUserDeposited();
                    renderAuctionDeposit();

                    if (auction.getCurrentHighestBid() != null) {
                        leaderLabel.setText("Người dẫn đầu: " + usernameOf(auction.getCurrentHighestBid().getBidder()));
                    }

                    if (auction.getEndTime() != null) {
                        endDateTime = auction.getEndTime();
                        startCountdown();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> titleLabel.setText("Lỗi tải chi tiết: " + e.getMessage()));
            }
        }).start();
    }

    private void loadWalletInfo() {
        String token = ClientSession.getInstance().getToken();
        if (token == null) return;

        new Thread(() -> {
            try {
                WalletResponseDTO wallet = walletClient.getWallet(token);
                Platform.runLater(() -> renderWallet(wallet));
            } catch (Exception e) {
                Platform.runLater(() -> balanceLabel.setText("Lỗi tải ví"));
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

                        BidDTO latest = history.get(history.size() - 1);
                        leaderLabel.setText("Người dẫn đầu: " + usernameOf(latest.getBidder()));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> bidHistoryList.getItems().add("Lỗi tải lịch sử"));
            }
        }).start();
    }

    private void onPushMessage(PushMessage push) {
        if (push == null || push.getPushType() == null) return;

        Platform.runLater(() -> {
            if (push.getPushType() == PushActionType.BID_UPDATE) {
                PushEvents.BidUpdatePush data = push.getDataAs(PushEvents.BidUpdatePush.class);
                if (data == null || !currentAuctionId.equals(data.getAuctionId())) return;
                BidDTO bid = data.getBid();
                if (bid == null) return;

                priceLabel.setText(CurrencyFormatter.format(bid.getAmount()));
                leaderLabel.setText("Người dẫn đầu: " + usernameOf(bid.getBidder()));
                bidHistoryList.getItems().add(0, "Vừa xong - " + formatBid(bid));
                if (isCurrentUser(bid.getBidder())) {
                    currentUserDeposited = true;
                    renderAuctionDeposit();
                    loadWalletInfo();
                }
                pendingAutoBidLossNotice = bid.getBidType() == BidType.AUTO && !isCurrentUser(bid.getBidder());
                checkAutoBidState();
                return;
            }

            if (push.getPushType() == PushActionType.AUCTION_EXTENDED) {
                PushEvents.AuctionExtendedPush data = push.getDataAs(PushEvents.AuctionExtendedPush.class);
                if (data == null || !currentAuctionId.equals(data.getAuctionId())) return;

                try {
                    endDateTime = data.getNewEndTime();
                    startCountdown();
                } catch (Exception ignored) {}
                return;
            }

            if (push.getPushType() == PushActionType.AUCTION_ENDED) {
                PushEvents.AuctionEndedPush data = push.getDataAs(PushEvents.AuctionEndedPush.class);
                if (data == null || !currentAuctionId.equals(data.getAuctionId())) return;

                if (countdownTimeline != null) countdownTimeline.stop();
                timeLeftLabel.setText("Đã kết thúc");
                statusBadge.setText("ĐÃ KẾT THÚC");
                statusBadge.getStyleClass().remove("status-badge-running");
                statusBadge.getStyleClass().add("status-badge-finished");
                leaderLabel.setText("Người thắng: " + usernameOf(data.getWinner())
                        + " (Giá: " + CurrencyFormatter.format(data.getFinalPrice()) + ")");
                currentUserDeposited = currentUserDeposited && isCurrentUser(data.getWinner());
                renderAuctionDeposit();
                loadWalletInfo();

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
                Platform.runLater(() -> bidStatusLabel.setText("Lỗi subscribe: " + e.getMessage()));
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
                timeLeftLabel.setText("Đã kết thúc");
                countdownTimeline.stop();
            } else {
                long h = secs / 3600;
                long m = (secs % 3600) / 60;
                long s = secs % 60;
                timeLeftLabel.setText(String.format("Còn: %02d:%02d:%02d", h, m, s));
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
                        maxBidField.setText(String.format("%.0f", state.getMaxBid()));
                        incrementField.setText(String.format("%.0f", state.getIncrement()));
                        renderAutoBidState(state);
                    } else {
                        enableAutoBidBtn.setDisable(false);
                        cancelAutoBidBtn.setDisable(true);
                        autoBidStatusLabel.setText("");
                        lastAutoBidStatus = null;
                        autoBidStateLoaded = true;
                        pendingAutoBidLossNotice = false;
                    }
                });
            } catch (Exception ignored) {}
        }).start();
    }

    @FXML
    private void handlePlaceBid() {
        String amountText = bidAmountField.getText().replace(",", "").trim();
        if (amountText.isEmpty()) {
            bidStatusLabel.setText("Vui lòng nhập số tiền.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);
            String token = ClientSession.getInstance().getToken();

            bidStatusLabel.setText("Đang xử lý...");
            bidStatusLabel.setStyle("-fx-text-fill: #f39c12;");

            new Thread(() -> {
                try {
                    bidClient.placeBid(token, currentAuctionId, amount);
                    Platform.runLater(() -> {
                        bidStatusLabel.setText("Đặt giá thành công!");
                        bidStatusLabel.setStyle("-fx-text-fill: #22c55e;");
                        bidAmountField.clear();
                        currentUserDeposited = true;
                        renderAuctionDeposit();
                        loadWalletInfo();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        bidStatusLabel.setText("Lỗi: " + e.getMessage());
                        bidStatusLabel.setStyle("-fx-text-fill: #dc2626;");
                    });
                }
            }).start();
        } catch (NumberFormatException ex) {
            bidStatusLabel.setText("Số tiền không hợp lệ.");
            bidStatusLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    @FXML
    private void handleSetAutoBid() {
        String maxText = maxBidField.getText().replace(",", "").trim();
        String incText = incrementField.getText().replace(",", "").trim();

        if (maxText.isEmpty() || incText.isEmpty()) {
            autoBidStatusLabel.setText("Vui lòng nhập đủ thông tin.");
            return;
        }

        try {
            double maxBid = Double.parseDouble(maxText);
            double increment = Double.parseDouble(incText);
            String token = ClientSession.getInstance().getToken();

            autoBidStatusLabel.setText("Đang thiết lập...");

            new Thread(() -> {
                try {
                    bidClient.setAutoBid(token, currentAuctionId, maxBid, increment);
                    Platform.runLater(() -> {
                        currentUserDeposited = true;
                        renderAuctionDeposit();
                        loadWalletInfo();
                        checkAutoBidState();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        autoBidStatusLabel.setText("Lỗi: " + e.getMessage());
                        autoBidStatusLabel.setStyle("-fx-text-fill: #dc2626;");
                    });
                }
            }).start();
        } catch (NumberFormatException ex) {
            autoBidStatusLabel.setText("Số tiền không hợp lệ.");
        }
    }

    @FXML
    private void handleCancelAutoBid() {
        String token = ClientSession.getInstance().getToken();
        new Thread(() -> {
            try {
                bidClient.cancelAutoBid(token, currentAuctionId);
                Platform.runLater(() -> {
                    autoBidStatusLabel.setText("Đã hủy Auto-Bid.");
                    autoBidStatusLabel.setStyle("-fx-text-fill: #f39c12;");
                    enableAutoBidBtn.setDisable(false);
                    cancelAutoBidBtn.setDisable(true);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    autoBidStatusLabel.setText("Lỗi hủy Auto-Bid: " + e.getMessage());
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
        String type = bid.getBidType() != null ? " - " + bid.getBidType().name() : "";
        return time + " - " + usernameOf(bid.getBidder()) + " - "
                + CurrencyFormatter.format(bid.getAmount()) + type;
    }

    private void renderWallet(WalletResponseDTO wallet) {
        double balance = wallet != null ? wallet.getBalance() : 0.0;
        CurrencyFormatter.setMoneyText(balanceLabel, balance);
    }

    private void renderAuctionDeposit() {
        double deposit = currentUserDeposited ? auctionDepositAmount : 0.0;
        CurrencyFormatter.setMoneyText(depositLabel, deposit);
    }

    private void renderAutoBidState(AutoBidConfigDTO state) {
        AutoBidStatus status = state.getStatus();
        boolean shouldNotifyIneffective = autoBidStateLoaded
                && status == AutoBidStatus.INEFFECTIVE
                && lastAutoBidStatus != null
                && lastAutoBidStatus != AutoBidStatus.INEFFECTIVE
                && pendingAutoBidLossNotice;

        if (status == AutoBidStatus.PROTECTING) {
            enableAutoBidBtn.setDisable(true);
            cancelAutoBidBtn.setDisable(false);
            autoBidStatusLabel.setText("Bạn đang dẫn đầu. Auto-Bid sẽ bảo vệ tới "
                    + formatMoney(state.getProtectedUntil()) + ".");
            autoBidStatusLabel.setStyle("-fx-text-fill: #22c55e;");
            lastAutoBidStatus = status;
            autoBidStateLoaded = true;
            pendingAutoBidLossNotice = false;
            return;
        }

        if (status == AutoBidStatus.INEFFECTIVE) {
            enableAutoBidBtn.setDisable(false);
            cancelAutoBidBtn.setDisable(false);
            autoBidStatusLabel.setText(AUTO_BID_INEFFECTIVE_NOTICE);
            autoBidStatusLabel.setStyle("-fx-text-fill: #dc2626;");
            if (shouldNotifyIneffective) {
                bidHistoryList.getItems().add(0, "Thông báo - " + AUTO_BID_INEFFECTIVE_NOTICE);
            }
            lastAutoBidStatus = status;
            autoBidStateLoaded = true;
            pendingAutoBidLossNotice = false;
            return;
        }

        enableAutoBidBtn.setDisable(true);
        cancelAutoBidBtn.setDisable(false);
        autoBidStatusLabel.setText("Auto-Bid đang chờ. Hệ thống sẽ tự đặt giá tới "
                + formatMoney(state.getProtectedUntil()) + " khi cần.");
        autoBidStatusLabel.setStyle("-fx-text-fill: #f39c12;");
        lastAutoBidStatus = status;
        autoBidStateLoaded = true;
        pendingAutoBidLossNotice = false;
    }

    private String formatMoney(double amount) {
        return CurrencyFormatter.format(amount);
    }

    private boolean isCurrentUser(UserDTO user) {
        UserDTO currentUser = ClientSession.getInstance().getCurrentUser();
        return user != null && currentUser != null && user.getId().equals(currentUser.getId());
    }

    private String usernameOf(UserDTO user) {
        return user != null ? user.getUsername() : "Chưa rõ";
    }
}
