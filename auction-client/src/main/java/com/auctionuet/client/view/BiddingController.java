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
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
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
    private static final double AUTO_BID_STABILITY_CHECK_SECONDS = 3.2;
    private static final String AUTO_BID_INEFFECTIVE_NOTICE =
            "Auto-Bid đã hết hiệu lực vì giá hiện tại vượt trần. Hãy dẫn đầu lại, giữ vị trí 3 giây, rồi bật Auto-Bid mới.";
    private static final String AUTO_BID_REGAIN_LEAD_HINT =
            "Bạn đang dẫn đầu trở lại. Giữ vị trí đủ 3 giây rồi bật lại Auto-Bid.";
    private static final String AUTO_BID_REENABLE_HINT =
            "Bạn đang dẫn đầu ổn định. Có thể bật Auto-Bid mới.";

    @FXML private Label titleLabel;
    @FXML private Label priceLabel;
    @FXML private Label leaderLabel;
    @FXML private Label timeLeftLabel;
    @FXML private Label sellerLabel;
    @FXML private Label statusBadge;
    @FXML private Label cancelReasonLabel;

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

    @FXML private LineChart<Number, Number> bidPriceChart;
    @FXML private ListView<String> bidHistoryList;

    private String currentAuctionId;
    private final BidClient bidClient = new BidClient();
    private final AuctionClient auctionClient = new AuctionClient();
    private final WalletClient walletClient = new WalletClient();
    private final XYChart.Series<Number, Number> bidPriceSeries = new XYChart.Series<>();

    private javafx.animation.Timeline countdownTimeline;
    private LocalDateTime endDateTime;
    private double auctionDepositAmount;
    private boolean currentUserDeposited;
    private AutoBidStatus lastAutoBidStatus;
    private boolean autoBidStateLoaded;
    private boolean pendingAutoBidLossNotice;
    private String currentLeaderId;
    private LocalDateTime currentUserLeaderObservedAt;
    private javafx.animation.PauseTransition delayedAutoBidCheck;

    @FXML
    public void initialize() {
        setupBidPriceChart();
    }

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
                    CurrencyFormatter.setMoneyText(priceLabel, auction.getCurrentPrice());
                    auctionDepositAmount = auction.getDepositAmount() > 0
                            ? auction.getDepositAmount()
                            : auction.getItem() != null ? auction.getItem().getStartingPrice() * 0.10 : 0;
                    currentUserDeposited = auction.isCurrentUserDeposited();
                    renderAuctionDeposit();

                    if (auction.getCurrentHighestBid() != null) {
                        updateCurrentLeader(auction.getCurrentHighestBid().getBidder());
                    }

                    if (auction.getEndTime() != null) {
                        endDateTime = auction.getEndTime();
                        startCountdown();
                    }
                    if (auction.getStatus() == com.auctionuet.protocol.enums.AuctionStatus.CANCELED) {
                        showCanceledState(auction.getCanceledReason());
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
                    bidPriceSeries.getData().clear();
                    if (history != null && !history.isEmpty()) {
                        for (BidDTO entry : history) {
                            bidHistoryList.getItems().add(formatBid(entry));
                            appendBidPricePoint(entry);
                        }

                        BidDTO latest = history.get(history.size() - 1);
                        updateCurrentLeader(latest.getBidder());
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

                CurrencyFormatter.setMoneyText(priceLabel, bid.getAmount());
                updateCurrentLeader(bid.getBidder(), true);
                bidHistoryList.getItems().add(0, "Vừa xong - " + formatBid(bid));
                appendBidPricePoint(bid);
                if (isCurrentUser(bid.getBidder())) {
                    currentUserDeposited = true;
                    renderAuctionDeposit();
                    loadWalletInfo();
                }
                boolean externalBid = !isCurrentUser(bid.getBidder());
                pendingAutoBidLossNotice = externalBid;
                checkAutoBidState();
                if (externalBid && bid.getBidType() == BidType.MANUAL) {
                    scheduleDelayedAutoBidCheck();
                }
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
                return;
            }

            if (push.getPushType() == PushActionType.AUCTION_CANCELED) {
                PushEvents.AuctionCanceledPush data = push.getDataAs(PushEvents.AuctionCanceledPush.class);
                if (data == null || !currentAuctionId.equals(data.getAuctionId())) return;

                showCanceledState(data.getReason());
                currentUserDeposited = false;
                renderAuctionDeposit();
                loadWalletInfo();
            }
        });
    }

    private void showCanceledState(String reason) {
        if (countdownTimeline != null) countdownTimeline.stop();
        timeLeftLabel.setText("Đã hủy");
        statusBadge.setText("ĐÃ HỦY");
        statusBadge.getStyleClass().remove("status-badge-running");
        statusBadge.getStyleClass().add("status-badge-canceled");
        cancelReasonLabel.setText("Lý do hủy: " + (reason != null ? reason : "Chưa rõ"));
        cancelReasonLabel.setVisible(true);
        cancelReasonLabel.setManaged(true);
        bidAmountField.setDisable(true);
        placeBidBtn.setDisable(true);
        maxBidField.setDisable(true);
        incrementField.setDisable(true);
        enableAutoBidBtn.setDisable(true);
        cancelAutoBidBtn.setDisable(true);
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
        if (delayedAutoBidCheck != null) delayedAutoBidCheck.stop();
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

    private void setupBidPriceChart() {
        bidPriceSeries.setName("Giá bid");
        bidPriceChart.setAnimated(false);
        bidPriceChart.setCreateSymbols(true);
        bidPriceChart.getData().clear();
        bidPriceChart.getData().add(bidPriceSeries);
    }

    private void appendBidPricePoint(BidDTO bid) {
        int bidIndex = bidPriceSeries.getData().size() + 1;
        bidPriceSeries.getData().add(new XYChart.Data<>(bidIndex, bid.getAmount()));
    }

    private void updateCurrentLeader(UserDTO leader) {
        updateCurrentLeader(leader, false);
    }

    private void updateCurrentLeader(UserDTO leader, boolean leaderBidChanged) {
        String previousLeaderId = currentLeaderId;
        currentLeaderId = leader != null ? leader.getId() : null;
        leaderLabel.setText("Người dẫn đầu: " + usernameOf(leader));

        if (isCurrentUser(leader)) {
            boolean justBecameLeader = previousLeaderId == null || !previousLeaderId.equals(currentLeaderId);
            if (justBecameLeader || leaderBidChanged) {
                currentUserLeaderObservedAt = LocalDateTime.now();
                scheduleDelayedAutoBidCheck(AUTO_BID_STABILITY_CHECK_SECONDS);
            }
            return;
        }

        currentUserLeaderObservedAt = null;
    }

    private void scheduleDelayedAutoBidCheck() {
        scheduleDelayedAutoBidCheck(3.5);
    }

    private void scheduleDelayedAutoBidCheck(double seconds) {
        if (delayedAutoBidCheck != null) {
            delayedAutoBidCheck.stop();
        }
        delayedAutoBidCheck = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(seconds));
        delayedAutoBidCheck.setOnFinished(e -> checkAutoBidState());
        delayedAutoBidCheck.playFromStart();
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
            boolean currentUserLeader = isCurrentUserLeader();
            boolean canTryEnable = currentUserLeader && hasObservedCurrentUserLeadLongEnough();
            enableAutoBidBtn.setDisable(!canTryEnable);
            cancelAutoBidBtn.setDisable(false);
            autoBidStatusLabel.setText(currentUserLeader
                    ? (canTryEnable ? AUTO_BID_REENABLE_HINT : AUTO_BID_REGAIN_LEAD_HINT)
                    : AUTO_BID_INEFFECTIVE_NOTICE);
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

    private boolean isCurrentUserLeader() {
        UserDTO currentUser = ClientSession.getInstance().getCurrentUser();
        return currentUser != null && currentLeaderId != null && currentLeaderId.equals(currentUser.getId());
    }

    private boolean hasObservedCurrentUserLeadLongEnough() {
        return currentUserLeaderObservedAt != null
                && Duration.between(currentUserLeaderObservedAt, LocalDateTime.now()).toMillis()
                >= Math.round(AUTO_BID_STABILITY_CHECK_SECONDS * 1000);
    }

    private boolean isCurrentUser(UserDTO user) {
        UserDTO currentUser = ClientSession.getInstance().getCurrentUser();
        return user != null && currentUser != null && user.getId().equals(currentUser.getId());
    }

    private String usernameOf(UserDTO user) {
        return user != null ? user.getUsername() : "Chưa rõ";
    }
}
