package com.auctionuet.client.view;

import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.network.AuctionClient;
import com.auctionuet.client.network.BidClient;
import com.auctionuet.protocol.dto.response.auction.AuctionDTO;
import com.auctionuet.protocol.dto.response.user.UserDTO;
import com.auctionuet.protocol.enums.AuctionStatus;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

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
        sellerLabel.setText("Seller: " + dto.getSellerUsername());
        currentPriceLabel.setText(String.format("Gia hien tai: %,.0f VND", dto.getCurrentHighestBid()));

        AuctionStatus status = dto.getStatus();
        String currentStatus = status != null ? status.name() : "UNKNOWN";
        statusLabel.setText("Trang thai: " + currentStatus);
        timeLeftLabel.setText("Ket thuc: " + formatTime(dto.getEndTime()));

        if (dto.getCurrentWinnerUsername() != null) {
            leaderLabel.setText("Nguoi dan dau: " + dto.getCurrentWinnerUsername());
        }

        if (dto.getItem() != null) {
            typeLabel.setText("Loai: " + dto.getItem().getType());
            conditionLabel.setText("Tinh trang: " + nullToEmpty(dto.getItem().getCondition()));
            descriptionLabel.setText(nullToEmpty(dto.getItem().getDescription()));
            specLabel.setText(dto.getItem().getExtraFields() != null ? dto.getItem().getExtraFields().toString() : "");
        }

        UserDTO currentUser = ClientSession.getInstance().getCurrentUser();
        String currentUsername = currentUser != null ? currentUser.getUsername() : "";
        boolean isMyItem = currentUsername.equals(dto.getSellerUsername());

        if (isMyItem) {
            if (status == AuctionStatus.OPEN) {
                btnStartAuction.setVisible(true);
                btnStartAuction.setManaged(true);
            }
        } else if (status == AuctionStatus.RUNNING) {
            biddingArea.setVisible(true);
            biddingArea.setManaged(true);
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
}
