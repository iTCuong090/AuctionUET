package com.auctionuet.client.view;

import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.network.AuctionClient;
import com.auctionuet.client.network.BidClient;
import com.auctionuet.client.network.ServerConnection;
import com.auctionuet.protocol.PushActionType;
import com.auctionuet.protocol.PushMessage;
import com.auctionuet.protocol.dto.push.PushEvents;
import com.auctionuet.protocol.dto.response.auction.AuctionDTO;
import com.auctionuet.protocol.dto.response.user.UserDTO;
import com.auctionuet.protocol.enums.AuctionStatus;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuctionDetailController {
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Label nameLabel, sellerLabel, typeLabel, specLabel, conditionLabel, descriptionLabel;
    @FXML private Label currentPriceLabel, leaderLabel, timeLeftLabel, statusLabel;
    @FXML private ImageView productImageView;

    @FXML private LineChart<String, Number> priceChart;
    @FXML private TableView<?> bidHistoryTable;
    @FXML private TableColumn<?, ?> bidderCol, amountCol, timeCol;

    @FXML private Button btnStartAuction;
    @FXML private HBox biddingArea;
    @FXML private TextField bidAmountField;
    @FXML private Button btnPlaceBid;
    @FXML private Label bidErrorLabel;

    private String currentAuctionId;
    private boolean subscribed;

    @FXML
    public void initialize() {
        btnPlaceBid.setOnAction(e -> handlePlaceBid());
    }

    public void setAuctionData(String auctionId) {
        this.currentAuctionId = auctionId;
        String token = ClientSession.getInstance().getToken();

        btnStartAuction.setVisible(false);
        btnStartAuction.setManaged(false);
        biddingArea.setVisible(false);
        biddingArea.setManaged(false);
        bidErrorLabel.setVisible(false);

        new Thread(() -> {
            try {
                AuctionClient client = new AuctionClient();
                AuctionDTO dto = client.getAuctionDetail(token, auctionId);

                Platform.runLater(() -> updateUI(dto));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Loi lay chi tiet: " + e.getMessage());
                    statusLabel.setTextFill(javafx.scene.paint.Color.RED);
                });
            }
        }).start();
    }

    private void updateUI(AuctionDTO dto) {
        nameLabel.setText(dto.getTitle());
        sellerLabel.setText("Seller: " + usernameOf(dto.getSeller()));
        currentPriceLabel.setText(String.format("Gia hien tai: %,.0f VND", dto.getCurrentPrice()));

        AuctionStatus status = dto.getStatus();
        String currentStatus = status != null ? status.name() : "UNKNOWN";
        statusLabel.setText("Trang thai: " + currentStatus);
        timeLeftLabel.setText("Ket thuc: " + formatTime(dto.getEndTime()));

        if (dto.getWinner() != null) {
            leaderLabel.setText("Nguoi dan dau: " + usernameOf(dto.getWinner()));
        }

        if (dto.getItem() != null) {
            typeLabel.setText("Loai: " + dto.getItem().getType());
            conditionLabel.setText("Tinh trang: " + valueOrEmpty(dto.getItem().getCondition()));
            descriptionLabel.setText(nullToEmpty(dto.getItem().getDescription()));
            specLabel.setText(dto.getItem().getExtraFields() != null ? dto.getItem().getExtraFields().toString() : "");
        }

        UserDTO currentUser = ClientSession.getInstance().getCurrentUser();
        String currentUserId = currentUser != null ? currentUser.getId() : "";
        boolean isMyItem = dto.getSeller() != null && currentUserId.equals(dto.getSeller().getId());

        if (isMyItem) {
            if (status == AuctionStatus.OPEN) {
                btnStartAuction.setVisible(true);
                btnStartAuction.setManaged(true);
            }
        } else if (status == AuctionStatus.RUNNING) {
            biddingArea.setVisible(true);
            biddingArea.setManaged(true);
        }

        if (status == AuctionStatus.OPEN) {
            subscribeToAuction(dto.getId());
            ServerConnection.getInstance().setPushListener(this::onPushMessage);
        }
    }

    @FXML
    private void handleStartAuction() {
        String token = ClientSession.getInstance().getToken();
        btnStartAuction.setDisable(true);
        btnStartAuction.setText("Dang xu ly...");

        new Thread(() -> {
            try {
                AuctionClient client = new AuctionClient();
                client.startAuction(token, currentAuctionId);

                Platform.runLater(() -> {
                    statusLabel.setText("Trang thai: RUNNING");
                    btnStartAuction.setVisible(false);
                    btnStartAuction.setManaged(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnStartAuction.setText("Loi, bam thu lai");
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
                showBidError("Gia phai lon hon 0.");
                return;
            }

            showBidError("Dang gui gia...", javafx.scene.paint.Color.ORANGE);
            String token = ClientSession.getInstance().getToken();

            new Thread(() -> {
                try {
                    new BidClient().placeBid(token, currentAuctionId, bidAmount);
                    Platform.runLater(() -> {
                        showBidError("Dat gia thanh cong.", javafx.scene.paint.Color.GREEN);
                        bidAmountField.clear();
                        setAuctionData(currentAuctionId);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showBidError("Loi: " + e.getMessage()));
                }
            }).start();
        } catch (NumberFormatException ex) {
            showBidError("Vui long nhap so hop le.");
        }
    }

    private void subscribeToAuction(String auctionId) {
        if (subscribed) {
            return;
        }
        subscribed = true;
        new Thread(() -> {
            try {
                new BidClient().subscribe(ClientSession.getInstance().getToken(), auctionId);
            } catch (Exception e) {
                Platform.runLater(() -> {
                    bidErrorLabel.setText("Loi subscribe: " + e.getMessage());
                    bidErrorLabel.setVisible(true);
                });
            }
        }).start();
    }

    private void onPushMessage(PushMessage push) {
        if (push == null || push.getPushType() != PushActionType.AUCTION_STARTED) {
            return;
        }

        PushEvents.AuctionStartedPush data = push.getDataAs(PushEvents.AuctionStartedPush.class);
        if (data == null || !currentAuctionId.equals(data.getAuctionId())) {
            return;
        }

        Platform.runLater(this::openBiddingView);
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
            statusLabel.setText("Loi chuyen sang man dau gia: " + e.getMessage());
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

    private String formatTime(LocalDateTime time) {
        return time != null ? time.format(DISPLAY_TIME) : "Chua ro";
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    private String usernameOf(UserDTO user) {
        return user != null ? user.getUsername() : "Chua ro";
    }

    private String valueOrEmpty(Object value) {
        return value != null ? value.toString() : "";
    }
}
