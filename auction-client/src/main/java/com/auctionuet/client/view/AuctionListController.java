package com.auctionuet.client.view;

import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.network.AuctionClient;
import com.auctionuet.protocol.dto.response.auction.AuctionDTO;
import com.auctionuet.protocol.enums.AuctionStatus;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AuctionListController {
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final double CARD_GAP = 20.0;
    private static final double MIN_CARD_WIDTH = 220.0;
    private static final int MAX_CARD_COLUMNS = 4;

    @FXML private FlowPane auctionGrid;
    @FXML private ScrollPane auctionScrollPane;
    @FXML private TextField searchField;
    @FXML private Button btnMyAuctions;

    private List<AuctionDTO> allAuctions = new ArrayList<>();
    private double currentCardWidth = 300.0;
    private boolean myAuctionsOnly;

    @FXML
    public void initialize() {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        searchField.setOnAction(e -> applyFilters());
        boolean seller = ClientSession.getInstance().isSeller();
        btnMyAuctions.setVisible(seller);
        btnMyAuctions.setManaged(seller);
        if (seller) {
            btnMyAuctions.setOnAction(e -> toggleMyAuctionsFilter());
            updateMyAuctionsButtonStyle();
        }
        setupStableGridWidth();

        loadAuctionsFromServer();
    }

    private void setupStableGridWidth() {
        auctionScrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) ->
                syncGridWidth(newBounds));
        Platform.runLater(() -> syncGridWidth(auctionScrollPane.getViewportBounds()));
    }

    private void syncGridWidth(Bounds viewportBounds) {
        if (viewportBounds == null || viewportBounds.getWidth() <= 0) {
            return;
        }

        double width = viewportBounds.getWidth();
        auctionGrid.setMinWidth(width);
        auctionGrid.setPrefWidth(width);
        auctionGrid.setMaxWidth(width);
        auctionGrid.setPrefWrapLength(width);
        currentCardWidth = calculateCardWidth(width);
        auctionGrid.getChildren().forEach(node -> {
            if (node instanceof Region region) {
                applyCardWidth(region);
            }
        });
    }

    private double calculateCardWidth(double availableWidth) {
        int columns = (int) ((availableWidth + CARD_GAP) / (MIN_CARD_WIDTH + CARD_GAP));
        columns = Math.max(1, Math.min(MAX_CARD_COLUMNS, columns));
        return Math.floor((availableWidth - ((columns - 1) * CARD_GAP) - 2) / columns);
    }

    private void applyCardWidth(Region card) {
        card.setMinWidth(currentCardWidth);
        card.setPrefWidth(currentCardWidth);
        card.setMaxWidth(currentCardWidth);
    }

    private void loadAuctionsFromServer() {
        String currentToken = ClientSession.getInstance().getToken();
        if (currentToken == null) return;

        new Thread(() -> {
            try {
                AuctionClient client = new AuctionClient();
                List<AuctionDTO> list = client.getAuctions(currentToken);
                Platform.runLater(() -> {
                    allAuctions = AuctionViewFilter.representativeAuctions(list);
                    applyFilters();
                });
            } catch (Exception e) {
                Platform.runLater(() -> System.out.println("Lỗi tải danh sách đấu giá: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleSearch() {
        applyFilters();
    }

    private void applyFilters() {
        String keyword = searchField.getText() != null
                ? searchField.getText().trim().toLowerCase(Locale.ROOT)
                : "";

        List<AuctionDTO> filtered = allAuctions.stream()
                .filter(auction -> !myAuctionsOnly || matchesCurrentUser(auction))
                .filter(auction -> matchesKeyword(auction, keyword))
                .toList();
        renderGrid(filtered);
    }

    private void toggleMyAuctionsFilter() {
        myAuctionsOnly = !myAuctionsOnly;
        updateMyAuctionsButtonStyle();
        applyFilters();
    }

    private void updateMyAuctionsButtonStyle() {
        if (btnMyAuctions == null) {
            return;
        }
        btnMyAuctions.getStyleClass().remove("active");
        if (myAuctionsOnly) {
            btnMyAuctions.getStyleClass().add("active");
        }
    }

    private boolean matchesKeyword(AuctionDTO auction, String keyword) {
        if (keyword.isEmpty()) {
            return true;
        }

        String title = auction.getTitle() != null ? auction.getTitle() : "";
        String itemName = auction.getItem() != null && auction.getItem().getName() != null
                ? auction.getItem().getName()
                : "";
        String seller = auction.getSeller() != null && auction.getSeller().getUsername() != null
                ? auction.getSeller().getUsername()
                : "";
        return title.toLowerCase(Locale.ROOT).contains(keyword)
                || itemName.toLowerCase(Locale.ROOT).contains(keyword)
                || seller.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private boolean matchesCurrentUser(AuctionDTO auction) {
        String currentUserId = currentUserId();
        if (auction == null || currentUserId == null || currentUserId.isBlank()) {
            return false;
        }
        return ClientSession.getInstance().isSeller()
                && auction.getSeller() != null
                && currentUserId.equals(auction.getSeller().getId());
    }

    private String currentUserId() {
        return ClientSession.getInstance().getCurrentUser() != null
                ? ClientSession.getInstance().getCurrentUser().getId()
                : null;
    }

    private void renderGrid(List<AuctionDTO> auctions) {
        auctionGrid.getChildren().clear();
        for (AuctionDTO item : auctions) {
            auctionGrid.getChildren().add(createAuctionCard(item));
        }
        syncGridWidth(auctionScrollPane.getViewportBounds());
    }

    private VBox createAuctionCard(AuctionDTO item) {
        VBox card = new VBox(12);
        card.getStyleClass().add("auction-card");
        applyCardWidth(card);

        Label title = new Label(item.getTitle());
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold;");
        title.getStyleClass().add("text-primary");
        title.setWrapText(true);

        Label price = new Label();
        CurrencyFormatter.setMoneyText(price, item.getCurrentPrice());
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
        Label timeLabel = new Label("Kết thúc: " + formatTime(item.getEndTime()));
        Label sellerLabel = new Label("Seller: " + valueOrUnknown(item.getSeller() != null ? item.getSeller().getUsername() : null));

        String subStyle = "-fx-font-size: 13px;";
        timeLabel.setStyle(subStyle);
        timeLabel.getStyleClass().add("text-secondary");
        sellerLabel.setStyle(subStyle);
        sellerLabel.getStyleClass().add("text-secondary");

        infoBox.getChildren().addAll(timeLabel, sellerLabel);

        card.getChildren().addAll(title, price, statusBadge, infoBox);
        if (ClientSession.getInstance().isBidder()) {
            card.getChildren().add(createBidderActions(item, status));
        } else {
            Button btnDetail = new Button("Xem chi tiết");
            btnDetail.getStyleClass().add("button");
            btnDetail.setMaxWidth(Double.MAX_VALUE);
            btnDetail.setOnAction(e -> openAuctionDetail(btnDetail, item));
            card.getChildren().add(btnDetail);
        }
        return card;
    }

    private VBox createBidderActions(AuctionDTO item, AuctionStatus status) {
        VBox actions = new VBox(6);

        Button registerButton = new Button("Đăng ký đấu giá");
        registerButton.getStyleClass().add("button");
        registerButton.setMaxWidth(Double.MAX_VALUE);
        registerButton.setDisable(status != AuctionStatus.RUNNING);
        registerButton.setOnAction(e -> openBiddingView(registerButton, item));

        Hyperlink detailLink = new Hyperlink("Xem chi tiết thông tin");
        detailLink.setStyle(
                "-fx-text-fill: #14b8a6; " +
                "-fx-font-weight: bold; " +
                "-fx-border-color: transparent; " +
                "-fx-padding: 0;");
        detailLink.setOnAction(e -> openAuctionDetail(detailLink, item));

        HBox detailLinkBox = new HBox(detailLink);
        detailLinkBox.setAlignment(javafx.geometry.Pos.CENTER);
        detailLinkBox.setMaxWidth(Double.MAX_VALUE);

        actions.getChildren().addAll(registerButton, detailLinkBox);
        return actions;
    }

    private void openBiddingView(Node source, AuctionDTO item) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/BiddingView.fxml"));
            javafx.scene.Parent root = loader.load();

            BiddingController biddingController = loader.getController();
            biddingController.setAuctionId(item.getId());

            replaceContent(source, root);
        } catch (Exception ex) {
            System.out.println("Lỗi chuyển màn hình: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void openAuctionDetail(Node source, AuctionDTO item) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/AuctionDetailView.fxml"));
            javafx.scene.Parent root = loader.load();

            AuctionDetailController detailController = loader.getController();
            detailController.setAuctionData(item.getId());

            replaceContent(source, root);
        } catch (Exception ex) {
            System.out.println("Lỗi chuyển màn hình: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void replaceContent(Node source, javafx.scene.Parent root) {
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
    }

    private String formatTime(LocalDateTime time) {
        return time != null ? time.format(DISPLAY_TIME) : "Chưa rõ";
    }

    private String valueOrUnknown(String value) {
        return value != null ? value : "Chưa rõ";
    }
}
