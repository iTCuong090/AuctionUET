package com.auctionuet.client.view;

import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.network.AuctionClient;
import com.auctionuet.protocol.dto.response.auction.AuctionDTO;
import com.auctionuet.protocol.enums.AuctionStatus;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AuctionListController {
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private FlowPane auctionGrid;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private TextField searchField;

    @FXML
    public void initialize() {
        filterComboBox.getItems().addAll("Tat ca", "OPEN", "RUNNING", "FINISHED", "WAITING_PAYMENT", "PAID", "CANCELED");
        filterComboBox.setValue("Tat ca");

        loadAuctionsFromServer();
    }

    private void loadAuctionsFromServer() {
        String currentToken = ClientSession.getInstance().getToken();
        if (currentToken == null) return;

        new Thread(() -> {
            try {
                AuctionClient client = new AuctionClient();
                List<AuctionDTO> list = client.getAuctions(currentToken);
                Platform.runLater(() -> renderGrid(list));
            } catch (Exception e) {
                Platform.runLater(() -> System.out.println("Loi tai danh sach dau gia: " + e.getMessage()));
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
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold;");
        title.getStyleClass().add("text-primary");
        title.setWrapText(true);

        Label price = new Label(String.format("%,.0f VND", item.getCurrentPrice()));
        price.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        price.getStyleClass().add("text-accent");

        AuctionStatus status = item.getStatus();
        String statusStr = status != null ? status.name() : "UNKNOWN";
        Label statusBadge = new Label(statusStr);

        if (status == AuctionStatus.RUNNING) {
            statusBadge.getStyleClass().add("status-badge-running");
        } else if (status == AuctionStatus.FINISHED || status == AuctionStatus.PAID) {
            statusBadge.getStyleClass().add("status-badge-finished");
        } else {
            statusBadge.getStyleClass().add("status-badge-open");
        }

        VBox infoBox = new VBox(5);
        Label timeLabel = new Label("Ket thuc: " + formatTime(item.getEndTime()));
        Label sellerLabel = new Label("Seller: " + valueOrUnknown(item.getSeller() != null ? item.getSeller().getUsername() : null));

        String subStyle = "-fx-font-size: 13px;";
        timeLabel.setStyle(subStyle);
        timeLabel.getStyleClass().add("text-secondary");
        sellerLabel.setStyle(subStyle);
        sellerLabel.getStyleClass().add("text-secondary");

        infoBox.getChildren().addAll(timeLabel, sellerLabel);

        Button btnDetail = new Button("Xem chi tiet");
        btnDetail.getStyleClass().add("button");
        btnDetail.setMaxWidth(Double.MAX_VALUE);

        btnDetail.setOnAction(e -> openAuctionView(btnDetail, item, status));

        card.getChildren().addAll(title, price, statusBadge, infoBox, btnDetail);
        return card;
    }

    private void openAuctionView(Button source, AuctionDTO item, AuctionStatus status) {
        try {
            String fxmlPath = status == AuctionStatus.RUNNING ? "/fxml/BiddingView.fxml" : "/fxml/AuctionDetailView.fxml";

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(fxmlPath));
            javafx.scene.Parent root = loader.load();

            if (status == AuctionStatus.RUNNING) {
                BiddingController biddingController = loader.getController();
                biddingController.setAuctionId(item.getId());
            } else {
                AuctionDetailController detailController = loader.getController();
                detailController.setAuctionData(item.getId());
            }

            javafx.scene.Parent currentRoot = source.getScene().getRoot();
            if (currentRoot instanceof javafx.scene.layout.HBox) {
                javafx.scene.layout.VBox mainCard =
                        (javafx.scene.layout.VBox) ((javafx.scene.layout.HBox) currentRoot).getChildren().get(1);
                javafx.scene.layout.StackPane contentArea =
                        (javafx.scene.layout.StackPane) mainCard.getChildren().get(1);
                contentArea.getChildren().setAll(root);
            } else if (currentRoot instanceof javafx.scene.layout.BorderPane) {
                ((javafx.scene.layout.BorderPane) currentRoot).setCenter(root);
            } else {
                System.out.println("Dashboard layout khong khop.");
            }
        } catch (Exception ex) {
            System.out.println("Loi chuyen man hinh: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private String formatTime(LocalDateTime time) {
        return time != null ? time.format(DISPLAY_TIME) : "Chua ro";
    }

    private String valueOrUnknown(String value) {
        return value != null ? value : "Chua ro";
    }
}
