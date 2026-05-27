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
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import com.auctionuet.client.util.AudioRecorder;
import com.auctionuet.client.network.GeminiVoiceServiceClient;

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
    @FXML private Button btnMic;
    @FXML private Button placeBidBtn;
    @FXML private Label bidStatusLabel;

    @FXML private TextField maxBidField;
    @FXML private TextField incrementField;
    @FXML private Button enableAutoBidBtn;
    @FXML private Button cancelAutoBidBtn;
    @FXML private Label autoBidStatusLabel;

    @FXML private Button btnToggleGeminiConfig;
    @FXML private VBox geminiConfigContent;
    @FXML private ComboBox<String> geminiModelComboBox;
    @FXML private Label geminiModelDescLabel;
    @FXML private TextField geminiApiKeyField;
    @FXML private Button btnSaveGeminiConfig;
    @FXML private Label geminiConfigStatusLabel;

    @FXML private LineChart<Number, Number> bidPriceChart;
    @FXML private ListView<String> bidHistoryList;

    private String currentAuctionId;
    private String auctionDescription;
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

    private boolean isRecording = false;
    private AudioRecorder audioRecorder;
    private File tempVoiceFile;
    private final GeminiVoiceServiceClient voiceServiceClient = new GeminiVoiceServiceClient();
    private javafx.event.EventHandler<KeyEvent> keyHandler;

    @FXML
    public void initialize() {
        setupBidPriceChart();
        setupGeminiConfig();
    }

    public void setAuctionId(String auctionId) {
        this.currentAuctionId = auctionId;

        loadAuctionDetail();
        loadWalletInfo();
        loadBidHistory();
        subscribeToAuction(auctionId);
        ServerConnection.getInstance().setPushListener(this::onPushMessage);
        checkAutoBidState();

        this.tempVoiceFile = new File(System.getProperty("java.io.tmpdir"), "uet_voice_bid.wav");
        Platform.runLater(() -> {
            if (btnMic != null && btnMic.getScene() != null) {
                keyHandler = event -> {
                    if (event.getCode() == KeyCode.F) {
                        handleVoiceBiddingToggle();
                        event.consume();
                    }
                };
                btnMic.getScene().addEventHandler(KeyEvent.KEY_PRESSED, keyHandler);
            }
        });
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
                    auctionDescription = auction.getDescription();
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
                            // Thêm vào đầu danh sách để hiển thị mới nhất lên trên
                            bidHistoryList.getItems().add(0, formatBid(entry));
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

        if (keyHandler != null && btnMic != null && btnMic.getScene() != null) {
            btnMic.getScene().removeEventHandler(KeyEvent.KEY_PRESSED, keyHandler);
        }
        if (tempVoiceFile != null && tempVoiceFile.exists()) {
            tempVoiceFile.delete();
        }
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
    private void handleMicClicked() {
        handleVoiceBiddingToggle();
    }

    private void handleVoiceBiddingToggle() {
        // 1. Kiểm tra API Key trong RAM
        String apiKey = ClientSession.getInstance().getGeminiApiKey();
        if (apiKey.isEmpty()) {
            // Hiển thị Dialog yêu cầu nhập Key lần đầu
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Cấu hình Google Gemini API Key");
            dialog.setHeaderText("Tính năng Voice Bidding yêu cầu API Key của Gemini.");
            dialog.setContentText("Vui lòng nhập API Key của bạn (Key sẽ chỉ lưu trong RAM phiên chạy này):");
            
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent() && !result.get().isBlank()) {
                ClientSession.getInstance().setGeminiApiKey(result.get());
                apiKey = ClientSession.getInstance().getGeminiApiKey();
                if (geminiApiKeyField != null) {
                    geminiApiKeyField.setText(apiKey);
                }
            } else {
                bidStatusLabel.setText("Đã hủy cấu hình API Key.");
                bidStatusLabel.setStyle("-fx-text-fill: #dc2626;");
                return;
            }
        }

        // 2. Chuyển đổi trạng thái thu âm (Toggle)
        if (!isRecording) {
            startVoiceRecording();
        } else {
            stopVoiceRecordingAndProcess(apiKey);
        }
    }

    private void startVoiceRecording() {
        try {
            isRecording = true;
            bidStatusLabel.setText("🎙️ Đang thu âm... Ấn F hoặc click Mic để dừng");
            bidStatusLabel.setStyle("-fx-text-fill: #3b82f6;");
            btnMic.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white;"); // Đổi màu nút sang đỏ khi đang ghi âm
            btnMic.setText("🎙️ Đang ghi âm...");
            
            audioRecorder = new AudioRecorder(tempVoiceFile);
            audioRecorder.startRecording();
        } catch (Exception e) {
            isRecording = false;
            btnMic.setStyle("-fx-background-color: #4f46e5; -fx-text-fill: white;");
            btnMic.setText("🎙️ Ấn F để nói");
            bidStatusLabel.setText("Lỗi khởi động mic: " + e.getMessage());
            bidStatusLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    private void stopVoiceRecordingAndProcess(String apiKey) {
        if (audioRecorder == null) return;
        
        try {
            isRecording = false;
            btnMic.setStyle("-fx-background-color: #4f46e5; -fx-text-fill: white;");
            btnMic.setText("🎙️ Ấn F để nói");
            audioRecorder.stopRecording();
            
            bidStatusLabel.setText("⏳ Đang gửi âm thanh phân tích qua Gemini...");
            bidStatusLabel.setStyle("-fx-text-fill: #f59e0b;");

            String selectedModel = ClientSession.getInstance().getGeminiModel();

            new Thread(() -> {
                try {
                    long amount = voiceServiceClient.processVoiceBid(tempVoiceFile, apiKey, selectedModel);
                    Platform.runLater(() -> {
                        if (amount > 0) {
                            bidAmountField.setText(String.valueOf(amount));
                            bidStatusLabel.setText("✅ Đã điền giá: " + CurrencyFormatter.format(amount));
                            bidStatusLabel.setStyle("-fx-text-fill: #10b981;");
                        } else {
                            bidStatusLabel.setText("❌ Gemini không phát hiện số tiền hợp lệ. Hãy thử lại!");
                            bidStatusLabel.setStyle("-fx-text-fill: #dc2626;");
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        bidStatusLabel.setText("Lỗi: " + e.getMessage());
                        bidStatusLabel.setStyle("-fx-text-fill: #dc2626;");
                    });
                } finally {
                    if (tempVoiceFile.exists()) {
                        tempVoiceFile.delete();
                    }
                }
            }).start();
            
        } catch (Exception e) {
            isRecording = false;
            btnMic.setStyle("-fx-background-color: #4f46e5; -fx-text-fill: white;");
            btnMic.setText("🎙️ Ấn F để nói");
            bidStatusLabel.setText("Lỗi dừng mic: " + e.getMessage());
            bidStatusLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    private boolean confirmDepositIfNeeded() {
        if (currentUserDeposited) {
            return true;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận Đăng ký Đấu giá & Đặt cọc");
        alert.setHeaderText("ĐĂNG KÝ THAM GIA PHIÊN ĐẤU GIÁ");

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label ruleLabel = new Label("Bằng việc đặt giá, bạn đồng ý với các điều khoản tham gia:");
        ruleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label rule1 = new Label("• Tuân thủ toàn bộ quy định và thời gian của phiên đấu giá.");
        Label rule2 = new Label("• Đồng ý đặt cọc 10% giá khởi điểm của sản phẩm.");

        // Tính toán và hiển thị số tiền đặt cọc cụ thể
        String depositMoneyStr = CurrencyFormatter.format(auctionDepositAmount);
        Label depositDetail = new Label("• Số tiền đặt cọc cần cọc cho phiên này: " + depositMoneyStr);
        depositDetail.setStyle("-fx-font-weight: bold; -fx-text-fill: #dc2626; -fx-font-size: 14px;"); // Màu đỏ cảnh báo

        Label rule3 = new Label("• Nếu thắng: Tiền cọc được khấu trừ khi thanh toán hóa đơn sản phẩm.");
        Label rule4 = new Label("• Nếu thua: Tiền cọc được hoàn trả đầy đủ vào ví tài khoản.");

        content.getChildren().addAll(ruleLabel, rule1, rule2, depositDetail, rule3, rule4);

        // Nếu có mô tả phiên (quy định phiên từ DTO)
        if (auctionDescription != null && !auctionDescription.isBlank()) {
            Label descTitle = new Label("Quy định cụ thể của phiên này:");
            descTitle.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 0 0; -fx-font-size: 13px;");
            Label descDetail = new Label(auctionDescription);
            descDetail.setWrapText(true);
            descDetail.setMaxWidth(450);
            content.getChildren().addAll(descTitle, descDetail);
        }

        alert.getDialogPane().setContent(content);
        alert.getDialogPane().getStyleClass().add("card-bg");

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    @FXML
    private void handlePlaceBid() {
        String amountText = bidAmountField.getText().replace(",", "").trim();
        if (amountText.isEmpty()) {
            bidStatusLabel.setText("Vui lòng nhập số tiền.");
            return;
        }

        // Kiểm tra và yêu cầu cọc cho lần bid đầu tiên
        if (!confirmDepositIfNeeded()) {
            bidStatusLabel.setText("Đã hủy đặt giá do không đồng ý điều khoản đặt cọc.");
            bidStatusLabel.setStyle("-fx-text-fill: #dc2626;");
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

        // Kiểm tra và yêu cầu cọc trước khi thiết lập Auto-Bid
        if (!confirmDepositIfNeeded()) {
            autoBidStatusLabel.setText("Đã hủy thiết lập Auto-Bid do không đồng ý điều khoản đặt cọc.");
            autoBidStatusLabel.setStyle("-fx-text-fill: #dc2626;");
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

    private void setupGeminiConfig() {
        if (geminiModelComboBox != null) {
            geminiModelComboBox.getItems().clear();
            geminiModelComboBox.getItems().addAll(
                "Gemini 3.1 Pro",
                "Gemini 3.5 Flash",
                "Gemini 3 Flash",
                "Gemini 3.1 Flash-Lite",
                "Gemini 2.5 Flash",
                "Gemini 2.5 Flash-Lite"
            );

            // Tải giá trị từ ClientSession
            String currentModel = ClientSession.getInstance().getGeminiModel();
            String displayModel = mapModelIdToDisplay(currentModel);
            geminiModelComboBox.setValue(displayModel);
            updateModelDescription(displayModel);

            String currentApiKey = ClientSession.getInstance().getGeminiApiKey();
            if (geminiApiKeyField != null) {
                geminiApiKeyField.setText(currentApiKey);
            }
        }
    }

    private String mapModelIdToDisplay(String modelId) {
        if (modelId == null) return "Gemini 2.5 Flash-Lite";
        return switch (modelId) {
            case "gemini-3.1-pro" -> "Gemini 3.1 Pro";
            case "gemini-3.5-flash" -> "Gemini 3.5 Flash";
            case "gemini-3-flash" -> "Gemini 3 Flash";
            case "gemini-3.1-flash-lite" -> "Gemini 3.1 Flash-Lite";
            case "gemini-2.5-flash" -> "Gemini 2.5 Flash";
            default -> "Gemini 2.5 Flash-Lite";
        };
    }

    private String mapDisplayToModelId(String display) {
        if (display == null) return "gemini-2.5-flash-lite";
        return switch (display) {
            case "Gemini 3.1 Pro" -> "gemini-3.1-pro";
            case "Gemini 3.5 Flash" -> "gemini-3.5-flash";
            case "Gemini 3 Flash" -> "gemini-3-flash";
            case "Gemini 3.1 Flash-Lite" -> "gemini-3.1-flash-lite";
            case "Gemini 2.5 Flash" -> "gemini-2.5-flash";
            default -> "gemini-2.5-flash-lite";
        };
    }

    @FXML
    private void handleModelChanged() {
        if (geminiModelComboBox != null) {
            String selected = geminiModelComboBox.getValue();
            updateModelDescription(selected);
        }
    }

    private void updateModelDescription(String displayModel) {
        if (geminiModelDescLabel == null) return;
        if (displayModel == null) {
            geminiModelDescLabel.setText("");
            return;
        }

        String desc = switch (displayModel) {
            case "Gemini 3.1 Pro" -> "Trí tuệ tiên tiến, kỹ năng giải quyết vấn đề phức tạp và khả năng tác nhân mạnh mẽ cùng khả năng lập trình theo cảm hứng. [Xem trước]";
            case "Gemini 3.5 Flash" -> "Mô hình thông minh nhất để duy trì hiệu suất tiên tiến cho các tác vụ tác nhân và lập trình. [Ổn định]";
            case "Gemini 3 Flash" -> "Hiệu suất ở cấp độ tiên tiến, ngang bằng với các mô hình lớn hơn nhưng chỉ tốn một phần chi phí. [Xem trước]";
            case "Gemini 3.1 Flash-Lite" -> "Hiệu suất ở cấp độ tiên tiến, ngang bằng với các mô hình lớn hơn nhưng chỉ tốn một phần chi phí. [Ổn định]";
            case "Gemini 2.5 Flash" -> "Cân bằng xuất sắc giữa tốc độ xử lý nhanh và độ chính xác cao. [Ổn định]";
            case "Gemini 2.5 Flash-Lite" -> "Mô hình cực kỳ nhanh, nhẹ và tiết kiệm chi phí, giảm thiểu tối đa giới hạn cuộc gọi. [Ổn định]";
            default -> "";
        };
        geminiModelDescLabel.setText(desc);
    }

    @FXML
    private void handleSaveGeminiConfig() {
        if (geminiApiKeyField == null || geminiModelComboBox == null) return;
        
        String apiKey = geminiApiKeyField.getText().trim();
        String displayModel = geminiModelComboBox.getValue();
        String modelId = mapDisplayToModelId(displayModel);

        ClientSession.getInstance().setGeminiApiKey(apiKey);
        ClientSession.getInstance().setGeminiModel(modelId);

        if (geminiConfigStatusLabel != null) {
            if (apiKey.isEmpty()) {
                geminiConfigStatusLabel.setText("⚠️ Đã cập nhật mô hình: " + displayModel + ". Chưa cấu hình API Key.");
                geminiConfigStatusLabel.setStyle("-fx-text-fill: #f59e0b;");
            } else {
                geminiConfigStatusLabel.setText("✅ Đã lưu cấu hình trợ lý giọng nói Gemini thành công!");
                geminiConfigStatusLabel.setStyle("-fx-text-fill: #10b981;");
            }
        }
    }

    @FXML
    private void handleToggleGeminiConfig() {
        if (geminiConfigContent == null) return;
        boolean currentlyVisible = geminiConfigContent.isVisible();
        geminiConfigContent.setVisible(!currentlyVisible);
        geminiConfigContent.setManaged(!currentlyVisible);
        if (btnToggleGeminiConfig != null) {
            btnToggleGeminiConfig.setText(currentlyVisible ? "🛠️ Hiện cấu hình" : "Ẩn cấu hình");
        }
    }
}
