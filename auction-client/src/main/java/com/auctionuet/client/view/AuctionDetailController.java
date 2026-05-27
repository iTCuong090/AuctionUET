package com.auctionuet.client.view;

import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.network.AdminClient;
import com.auctionuet.client.network.AuctionClient;
import com.auctionuet.client.network.ImageLoader;
import com.auctionuet.client.network.BidClient;
import com.auctionuet.client.network.ServerConnection;
import com.auctionuet.protocol.PushActionType;
import com.auctionuet.protocol.PushMessage;
import com.auctionuet.protocol.dto.push.PushEvents;
import com.auctionuet.protocol.dto.response.auction.AuctionDTO;
import com.auctionuet.protocol.dto.response.bid.BidDTO;
import com.auctionuet.protocol.dto.response.user.UserDTO;
import com.auctionuet.protocol.enums.AuctionStatus;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AuctionDetailController {
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Label nameLabel, sellerLabel, typeLabel, conditionLabel, descriptionLabel;
    @FXML private Label currentPriceLabel, leaderLabel, timeLeftLabel, statusLabel;
    @FXML private Label cancelReasonLabel, imagePlaceholderLabel;
    @FXML private ImageView productImageView;

    @FXML private TableView<BidDTO> bidHistoryTable;
    @FXML private TableColumn<BidDTO, String> bidderCol, amountCol, timeCol;
    @FXML private LineChart<Number, Number> bidPriceChart;

    @FXML private Button btnStartAuction;
    @FXML private HBox actionArea;
    @FXML private HBox biddingArea;
    @FXML private VBox specBox;
    @FXML private TextField bidAmountField;
    @FXML private Button btnPlaceBid;
    @FXML private Label bidErrorLabel;
    @FXML private VBox paymentArea;
    @FXML private Button btnPayAuction;
    @FXML private Label paymentInfoLabel, paymentErrorLabel;
    @FXML private VBox adminCancelArea;
    @FXML private Button btnAdminCancelAuction;
    @FXML private Label adminCancelErrorLabel;

    private String currentAuctionId;
    private boolean subscribed;
    private final BidClient bidClient = new BidClient();
    private final AuctionClient auctionClient = new AuctionClient();
    private final AdminClient adminClient = new AdminClient();
    private final XYChart.Series<Number, Number> bidPriceSeries = new XYChart.Series<>();
    private AuctionDTO currentAuction;

    @FXML
    public void initialize() {
        btnPlaceBid.setOnAction(e -> handlePlaceBid());
        btnPayAuction.setOnAction(e -> handlePayAuction());
        setupBidHistoryTable();
        setupBidPriceChart();
    }

    public void setAuctionData(String auctionId) {
        this.currentAuctionId = auctionId;
        String token = ClientSession.getInstance().getToken();

        btnStartAuction.setVisible(false);
        btnStartAuction.setManaged(false);
        actionArea.setVisible(false);
        actionArea.setManaged(false);
        biddingArea.setVisible(false);
        biddingArea.setManaged(false);
        bidErrorLabel.setVisible(false);
        paymentArea.setVisible(false);
        paymentArea.setManaged(false);
        paymentErrorLabel.setText("");
        adminCancelArea.setVisible(false);
        adminCancelArea.setManaged(false);
        btnAdminCancelAuction.setDisable(false);
        adminCancelErrorLabel.setText("");
        cancelReasonLabel.setVisible(false);
        cancelReasonLabel.setManaged(false);
        specBox.getChildren().clear();
        bidHistoryTable.getItems().clear();
        bidPriceSeries.getData().clear();

        new Thread(() -> {
            try {
                AuctionDTO dto = auctionClient.getAuctionDetail(token, auctionId);

                Platform.runLater(() -> {
                    updateUI(dto);
                    loadBidHistory();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    clearStatusBadgeStyle();
                    statusLabel.setText("Lỗi lấy chi tiết: " + e.getMessage());
                    statusLabel.setTextFill(javafx.scene.paint.Color.RED);
                });
            }
        }).start();
    }

    private void updateUI(AuctionDTO dto) {
        this.currentAuction = dto;
        nameLabel.setText(dto.getTitle());
        sellerLabel.setText("Seller: " + usernameOf(dto.getSeller()));
        CurrencyFormatter.setMoneyText(currentPriceLabel, "Giá hiện tại: ", dto.getCurrentPrice());

        AuctionStatus status = dto.getStatus();
        renderStatusBadge(status);
        if (status == AuctionStatus.OPEN) {
            timeLeftLabel.setText("Bắt đầu: " + formatTime(dto.getStartTime()) + " | Kết thúc: " + formatTime(dto.getEndTime()));
        } else {
            timeLeftLabel.setText("Kết thúc: " + formatTime(dto.getEndTime()));
        }

        if (status == AuctionStatus.CANCELED && dto.getWinner() != null) {
            leaderLabel.setText("Dẫn đầu trước khi hủy: " + usernameOf(dto.getWinner()));
        } else if (dto.getWinner() != null) {
            leaderLabel.setText("Người dẫn đầu: " + usernameOf(dto.getWinner()));
        } else {
            leaderLabel.setText("Người dẫn đầu: Chưa có");
        }
        renderCancellationInfo(dto);

        if (dto.getItem() != null) {
            typeLabel.setText(valueOrEmpty(dto.getItem().getType()));
            conditionLabel.setText("Tình trạng: " + valueOrEmpty(dto.getItem().getCondition()));
            descriptionLabel.setText(nullToEmpty(dto.getItem().getDescription()));
            renderExtraFields(dto.getItem().getExtraFields());

            // Tải hình ảnh sản phẩm thực tế và quản lý placeholder qua ImageLoader
            ImageLoader.loadImage(productImageView, imagePlaceholderLabel, dto.getItem().getImageUrl());
        }

        UserDTO currentUser = ClientSession.getInstance().getCurrentUser();
        String currentUserId = currentUser != null ? currentUser.getId() : "";
        boolean isMyItem = dto.getSeller() != null && currentUserId.equals(dto.getSeller().getId());

        boolean canStartAuction = isMyItem && status == AuctionStatus.OPEN;
        actionArea.setVisible(canStartAuction);
        actionArea.setManaged(canStartAuction);
        btnStartAuction.setVisible(canStartAuction);
        btnStartAuction.setManaged(canStartAuction);
        btnStartAuction.setDisable(false);
        btnStartAuction.setText("🚀 Bắt đầu phiên đấu giá");

        renderPaymentArea(dto, currentUserId);
        renderAdminCancelArea(dto);

        if (status == AuctionStatus.OPEN
                || status == AuctionStatus.RUNNING
                || status == AuctionStatus.WAITING_PAYMENT) {
            subscribeToAuction(dto.getId());
            ServerConnection.getInstance().setPushListener(this::onPushMessage);
        }
    }

    @FXML
    private void handleStartAuction() {
        String token = ClientSession.getInstance().getToken();
        btnStartAuction.setDisable(true);
        btnStartAuction.setText("Đang xử lý...");

        new Thread(() -> {
            try {
                auctionClient.startAuction(token, currentAuctionId);

                Platform.runLater(() -> {
                    renderStatusBadge(AuctionStatus.RUNNING);
                    actionArea.setVisible(false);
                    actionArea.setManaged(false);
                    btnStartAuction.setVisible(false);
                    btnStartAuction.setManaged(false);
                    setAuctionData(currentAuctionId);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnStartAuction.setText("Lỗi, bấm thử lại");
                    btnStartAuction.setDisable(false);
                });
            }
        }).start();
    }

    private void handlePlaceBid() {
        String amountText = bidAmountField.getText().replace(",", "").trim();

        try {
            double bidAmount = Double.parseDouble(amountText);
            if (bidAmount <= 0) {
                showBidError("Giá phải lớn hơn 0.");
                return;
            }

            showBidError("Đang gửi giá...", javafx.scene.paint.Color.ORANGE);
            String token = ClientSession.getInstance().getToken();

            new Thread(() -> {
                try {
                    bidClient.placeBid(token, currentAuctionId, bidAmount);
                    Platform.runLater(() -> {
                        showBidError("Đặt giá thành công.", javafx.scene.paint.Color.GREEN);
                        bidAmountField.clear();
                        setAuctionData(currentAuctionId);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showBidError("Lỗi: " + e.getMessage()));
                }
            }).start();
        } catch (NumberFormatException ex) {
            showBidError("Vui lòng nhập số hợp lệ.");
        }
    }

    private void handlePayAuction() {
        if (currentAuction == null) return;

        double deposit = currentAuction.getDepositAmount();
        double remaining = Math.max(0, currentAuction.getCurrentPrice() - deposit);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận thanh toán");
        alert.setHeaderText("XÁC NHẬN THANH TOÁN PHIÊN ĐẤU GIÁ");
        alert.setContentText("Bạn có chắc chắn muốn thực hiện thanh toán cho phiên đấu giá này?\n\n"
                + "• Tên phiên: " + currentAuction.getTitle() + "\n"
                + "• Giá thắng thầu: " + CurrencyFormatter.format(currentAuction.getCurrentPrice()) + "\n"
                + "• Tiền cọc đã giữ: " + CurrencyFormatter.format(deposit) + "\n"
                + "• Số tiền cần thanh toán thêm: " + CurrencyFormatter.format(remaining) + "\n\n"
                + "Hệ thống sẽ khấu trừ số tiền thực tế phải trả (" + CurrencyFormatter.format(remaining) 
                + ") trực tiếp từ số dư ví tài khoản của bạn.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        String token = ClientSession.getInstance().getToken();
        btnPayAuction.setDisable(true);
        paymentErrorLabel.setText("Đang thanh toán...");
        paymentErrorLabel.setTextFill(javafx.scene.paint.Color.ORANGE);

        new Thread(() -> {
            try {
                auctionClient.payAuction(token, currentAuctionId);
                Platform.runLater(() -> {
                    paymentErrorLabel.setText("Thanh toán thành công. Vật phẩm đã thuộc về bạn.");
                    paymentErrorLabel.setTextFill(javafx.scene.paint.Color.web("#22c55e"));
                    btnPayAuction.setVisible(false);
                    btnPayAuction.setManaged(false);
                    setAuctionData(currentAuctionId);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    paymentErrorLabel.setText("Lỗi thanh toán: " + e.getMessage());
                    paymentErrorLabel.setTextFill(javafx.scene.paint.Color.web("#ff4757"));
                    btnPayAuction.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    private void handleAdminCancelAuction() {
        if (currentAuction == null || !canAdminCancel(currentAuction.getStatus())) {
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Hủy phiên đấu giá");
        dialog.setHeaderText("Nhập lý do hủy phiên: " + currentAuction.getTitle());
        dialog.setContentText("Lý do:");
        Optional<String> answer = dialog.showAndWait();
        if (answer.isEmpty() || answer.get().isBlank()) {
            showAdminCancelError("Lý do hủy không được để trống.");
            return;
        }

        btnAdminCancelAuction.setDisable(true);
        showAdminCancelError("Đang hủy phiên...");
        String token = ClientSession.getInstance().getToken();
        String reason = answer.get().trim();
        new Thread(() -> {
            try {
                adminClient.cancelAuction(token, currentAuctionId, reason);
                Platform.runLater(() -> setAuctionData(currentAuctionId));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnAdminCancelAuction.setDisable(false);
                    showAdminCancelError("Lỗi hủy phiên: " + e.getMessage());
                });
            }
        }, "admin-cancel-auction").start();
    }

    private void setupBidHistoryTable() {
        bidderCol.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(usernameOf(cell.getValue().getBidder())));
        amountCol.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(CurrencyFormatter.format(cell.getValue().getAmount())));
        timeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatTime(cell.getValue().getTimestamp())));
    }

    private void setupBidPriceChart() {
        bidPriceSeries.setName("Giá bid");
        bidPriceChart.setAnimated(false);
        bidPriceChart.setCreateSymbols(true);
        bidPriceChart.getData().clear();
        bidPriceChart.getData().add(bidPriceSeries);
    }

    private void loadBidHistory() {
        String token = ClientSession.getInstance().getToken();
        if (token == null || currentAuctionId == null) {
            return;
        }

        new Thread(() -> {
            try {
                List<BidDTO> history = bidClient.getBidHistory(token, currentAuctionId);
                Platform.runLater(() -> {
                    bidHistoryTable.getItems().clear();
                    bidPriceSeries.getData().clear();
                    if (history != null) {
                        for (BidDTO bid : history) {
                            appendBid(bid);
                        }
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showBidError("Lỗi tải lịch sử bid: " + e.getMessage()));
            }
        }).start();
    }

    private void appendBid(BidDTO bid) {
        bidHistoryTable.getItems().add(bid);
        int bidIndex = bidPriceSeries.getData().size() + 1;
        bidPriceSeries.getData().add(new XYChart.Data<>(bidIndex, bid.getAmount()));
        CurrencyFormatter.setMoneyText(currentPriceLabel, "Giá hiện tại: ", bid.getAmount());
        leaderLabel.setText("Người dẫn đầu: " + usernameOf(bid.getBidder()));
    }

    private void renderPaymentArea(AuctionDTO dto, String currentUserId) {
        boolean canPay = dto.getStatus() == AuctionStatus.WAITING_PAYMENT
                && dto.getWinner() != null
                && currentUserId.equals(dto.getWinner().getId());
        paymentArea.setVisible(canPay);
        paymentArea.setManaged(canPay);
        btnPayAuction.setVisible(canPay);
        btnPayAuction.setManaged(canPay);
        btnPayAuction.setDisable(false);

        if (!canPay) {
            return;
        }

        double deposit = dto.getDepositAmount();
        double remaining = Math.max(0, dto.getCurrentPrice() - deposit);
        paymentInfoLabel.setText(String.format(
                "Cần thanh toán: %s | Cọc đã giữ: %s | Hạn: %s",
                CurrencyFormatter.format(remaining),
                CurrencyFormatter.format(deposit),
                formatTime(dto.getPaymentDeadlineAt())));
        CurrencyFormatter.installTooltip(paymentInfoLabel, String.format(
                "Cần thanh toán: %s%nCọc đã giữ: %s",
                CurrencyFormatter.formatFull(remaining),
                CurrencyFormatter.formatFull(deposit)));
    }

    private void subscribeToAuction(String auctionId) {
        if (subscribed) {
            return;
        }
        subscribed = true;
        new Thread(() -> {
            try {
                bidClient.subscribe(ClientSession.getInstance().getToken(), auctionId);
            } catch (Exception e) {
                Platform.runLater(() -> {
                    bidErrorLabel.setText("Lỗi subscribe: " + e.getMessage());
                    bidErrorLabel.setVisible(true);
                });
            }
        }).start();
    }

    private void onPushMessage(PushMessage push) {
        if (push == null || push.getPushType() == null) {
            return;
        }

        if (push.getPushType() == PushActionType.AUCTION_STARTED) {
            PushEvents.AuctionStartedPush data = push.getDataAs(PushEvents.AuctionStartedPush.class);
            if (data == null || !currentAuctionId.equals(data.getAuctionId())) {
                return;
            }

            Platform.runLater(() -> {
                if (ClientSession.getInstance().isBidder()) {
                    openBiddingView();
                } else {
                    setAuctionData(currentAuctionId);
                }
            });
            return;
        }

        if (push.getPushType() == PushActionType.BID_UPDATE) {
            PushEvents.BidUpdatePush data = push.getDataAs(PushEvents.BidUpdatePush.class);
            if (data == null || !currentAuctionId.equals(data.getAuctionId()) || data.getBid() == null) {
                return;
            }

            Platform.runLater(() -> appendBid(data.getBid()));
            return;
        }

        if (push.getPushType() == PushActionType.AUCTION_ENDED) {
            PushEvents.AuctionEndedPush data = push.getDataAs(PushEvents.AuctionEndedPush.class);
            if (data == null || !currentAuctionId.equals(data.getAuctionId())) {
                return;
            }

            Platform.runLater(() -> {
                renderStatusBadge(AuctionStatus.WAITING_PAYMENT);
                leaderLabel.setText("Người thắng: " + usernameOf(data.getWinner()));
                setAuctionData(currentAuctionId);
            });
            return;
        }

        if (push.getPushType() == PushActionType.AUCTION_CANCELED) {
            PushEvents.AuctionCanceledPush data = push.getDataAs(PushEvents.AuctionCanceledPush.class);
            if (data == null || !currentAuctionId.equals(data.getAuctionId())) {
                return;
            }

            Platform.runLater(() -> {
                renderStatusBadge(AuctionStatus.CANCELED);
                cancelReasonLabel.setText("Lý do hủy: " + data.getReason());
                cancelReasonLabel.setVisible(true);
                cancelReasonLabel.setManaged(true);
                setAuctionData(currentAuctionId);
            });
        }
    }

    private void renderAdminCancelArea(AuctionDTO dto) {
        boolean visible = ClientSession.getInstance().isAdmin() && canAdminCancel(dto.getStatus());
        adminCancelArea.setVisible(visible);
        adminCancelArea.setManaged(visible);
        btnAdminCancelAuction.setDisable(false);
        if (!visible) {
            adminCancelErrorLabel.setText("");
        }
    }

    private boolean canAdminCancel(AuctionStatus status) {
        return status == AuctionStatus.OPEN
                || status == AuctionStatus.RUNNING
                || status == AuctionStatus.WAITING_PAYMENT;
    }

    private void showAdminCancelError(String message) {
        adminCancelErrorLabel.setText(message);
    }

    private void renderCancellationInfo(AuctionDTO dto) {
        boolean canceled = dto.getStatus() == AuctionStatus.CANCELED
                && dto.getCanceledReason() != null
                && !dto.getCanceledReason().isBlank();
        cancelReasonLabel.setVisible(canceled);
        cancelReasonLabel.setManaged(canceled);
        cancelReasonLabel.setText(canceled ? "Lý do hủy: " + dto.getCanceledReason() : "");
    }

    private void openBiddingView() {
        cleanup();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/BiddingView.fxml"));
            Parent root = loader.load();
            BiddingController biddingController = loader.getController();
            biddingController.setAuctionId(currentAuctionId);

            Parent currentRoot = nameLabel.getScene().getRoot();
            if (currentRoot instanceof HBox) {
                VBox mainCard = (VBox) ((HBox) currentRoot).getChildren().get(1);
                javafx.scene.layout.StackPane contentArea =
                        (javafx.scene.layout.StackPane) mainCard.getChildren().get(1);
                contentArea.getChildren().setAll(root);
            }
        } catch (Exception e) {
            statusLabel.setText("Lỗi chuyển sang màn đấu giá: " + e.getMessage());
            statusLabel.setTextFill(javafx.scene.paint.Color.RED);
        }
    }

    public void cleanup() {
        if (subscribed && currentAuctionId != null) {
            new Thread(() -> new BidClient().unsubscribe(
                    ClientSession.getInstance().getToken(),
                    currentAuctionId)).start();
            subscribed = false;
        }
        ServerConnection.getInstance().setPushListener(null);
    }

    private void showBidError(String message) {
        showBidError(message, javafx.scene.paint.Color.web("#ff4757"));
    }

    private void showBidError(String message, javafx.scene.paint.Color color) {
        bidErrorLabel.setText(message);
        bidErrorLabel.setTextFill(color);
        bidErrorLabel.setVisible(true);
    }

    private void renderStatusBadge(AuctionStatus status) {
        clearStatusBadgeStyle();
        statusLabel.setTextFill(null);
        
        String statusText = status != null ? status.name() : "UNKNOWN";
        if (status == AuctionStatus.OPEN) {
            statusText = "SẮP DIỄN RA (OPEN)";
        } else if (status == AuctionStatus.RUNNING) {
            statusText = "ĐANG DIỄN RA (RUNNING)";
        } else if (status == AuctionStatus.FINISHED) {
            statusText = "ĐÃ KẾT THÚC (FINISHED)";
        } else if (status == AuctionStatus.PAID) {
            statusText = "ĐÃ THANH TOÁN (PAID)";
        } else if (status == AuctionStatus.CANCELED) {
            statusText = "ĐÃ HỦY (CANCELED)";
        }
        statusLabel.setText(statusText);
        
        if (status == AuctionStatus.RUNNING) {
            statusLabel.getStyleClass().add("status-badge-running");
        } else if (status == AuctionStatus.FINISHED || status == AuctionStatus.PAID) {
            statusLabel.getStyleClass().add("status-badge-finished");
        } else if (status == AuctionStatus.CANCELED) {
            statusLabel.getStyleClass().add("status-badge-canceled");
        } else {
            statusLabel.getStyleClass().add("status-badge-open");
        }
    }

    private void clearStatusBadgeStyle() {
        statusLabel.getStyleClass().removeAll(
                "status-badge-running",
                "status-badge-open",
                "status-badge-finished",
                "status-badge-canceled");
    }

    private void renderExtraFields(Map<String, Object> extraFields) {
        specBox.getChildren().clear();
        if (extraFields == null || extraFields.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : extraFields.entrySet()) {
            Label label = new Label(formatExtraFieldName(entry.getKey()) + ": "
                    + formatExtraFieldValue(entry.getValue()));
            label.getStyleClass().add("text-detail");
            label.setWrapText(true);
            specBox.getChildren().add(label);
        }
    }

    private String formatExtraFieldName(String key) {
        if ("brand".equals(key)) {
            return "Brand";
        }
        if ("warrantyMonths".equals(key)) {
            return "Warranty";
        }
        if ("vehicleYear".equals(key)) {
            return "Vehicle year";
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < key.length(); i++) {
            char ch = key.charAt(i);
            if (i > 0 && Character.isUpperCase(ch)) {
                result.append(' ');
            }
            result.append(i == 0 ? Character.toUpperCase(ch) : ch);
        }
        return result.toString();
    }

    private String formatExtraFieldValue(Object value) {
        if (value instanceof Number number) {
            double doubleValue = number.doubleValue();
            if (doubleValue == Math.rint(doubleValue)) {
                return String.format("%.0f", doubleValue);
            }
        }
        return value != null ? value.toString() : "";
    }

    private String formatTime(LocalDateTime time) {
        return time != null ? time.format(DISPLAY_TIME) : "Chưa rõ";
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    private String usernameOf(UserDTO user) {
        return user != null ? user.getUsername() : "Chưa rõ";
    }

    private String valueOrEmpty(Object value) {
        return value != null ? value.toString() : "";
    }
}
