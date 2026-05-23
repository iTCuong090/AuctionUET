package com.auctionuet.client.view;

import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.network.AuctionClient;
import com.auctionuet.protocol.dto.response.auction.AuctionDTO;
import com.auctionuet.protocol.enums.AuctionStatus;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class HomeController {
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Label statAuctionCount;
    @FXML private Label statRunningCount;
    @FXML private Label statFinishedCount;

    @FXML private HBox endingSoonBox;
    @FXML private Label endingSoonPlaceholder;
    @FXML private Button btnExplore;

    @FXML
    public void initialize() {
        btnExplore.setOnAction(e -> openAuctionList());
        loadHomeData();
    }

    private void loadHomeData() {
        String token = ClientSession.getInstance().getToken();
        if (token == null) {
            statAuctionCount.setText("0");
            statRunningCount.setText("0");
            statFinishedCount.setText("0");
            endingSoonPlaceholder.setText("Vui lòng đăng nhập để xem dữ liệu.");
            return;
        }

        new Thread(() -> {
            try {
                AuctionClient client = new AuctionClient();
                List<AuctionDTO> allAuctions = client.getAuctions(token);
                List<AuctionDTO> publicAuctions = AuctionViewFilter.representativeAuctions(allAuctions);

                int total = publicAuctions.size();
                long running = publicAuctions.stream()
                        .filter(a -> a.getStatus() == AuctionStatus.RUNNING)
                        .count();
                long finished = publicAuctions.stream()
                        .filter(a -> a.getStatus() == AuctionStatus.FINISHED)
                        .count();

                List<AuctionDTO> endingSoon = publicAuctions.stream()
                        .filter(a -> a.getStatus() == AuctionStatus.RUNNING)
                        .filter(a -> a.getEndTime() != null)
                        .sorted(Comparator.comparing(AuctionDTO::getEndTime))
                        .limit(3)
                        .collect(Collectors.toList());

                Platform.runLater(() -> {
                    statAuctionCount.setText(String.valueOf(total));
                    statRunningCount.setText(String.valueOf(running));
                    statFinishedCount.setText(String.valueOf(finished));
                    renderEndingSoon(endingSoon);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.out.println("Lỗi tải trang chủ: " + e.getMessage());
                    statAuctionCount.setText("-");
                    statRunningCount.setText("-");
                    statFinishedCount.setText("-");
                    endingSoonPlaceholder.setText("Không thể tải dữ liệu.");
                });
            }
        }).start();
    }

    private void renderEndingSoon(List<AuctionDTO> endingSoon) {
        endingSoonBox.getChildren().clear();

        if (endingSoon.isEmpty()) {
            Label empty = new Label("Hiện chưa có phiên đấu giá nào đang diễn ra.");
            empty.setStyle("-fx-font-style: italic; -fx-font-size: 16px;");
            empty.getStyleClass().add("text-secondary");
            endingSoonBox.getChildren().add(empty);
            return;
        }

        for (AuctionDTO auction : endingSoon) {
            endingSoonBox.getChildren().add(createMiniCard(auction));
        }
    }

    private VBox createMiniCard(AuctionDTO auction) {
        VBox card = new VBox(10);
        card.setPrefWidth(220);
        card.getStyleClass().add("card-bg");
        card.setStyle(
            "-fx-background-radius: 12; " +
            "-fx-padding: 20; " +
            "-fx-border-color: #0f3460; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1;"
        );

        Label title = new Label(auction.getTitle() != null ? auction.getTitle() : "Không có tiêu đề");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        title.getStyleClass().add("text-primary");
        title.setWrapText(true);

        Label price = new Label();
        CurrencyFormatter.setMoneyText(price, auction.getCurrentPrice());
        price.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        price.getStyleClass().add("text-accent");

        Label timeLabel = new Label("Kết thúc: " + formatTime(auction.getEndTime()));
        timeLabel.setStyle("-fx-font-size: 13px;");
        timeLabel.getStyleClass().add("text-detail");

        String seller = auction.getSeller() != null ? auction.getSeller().getUsername() : "Chưa rõ";
        Label sellerLabel = new Label("Seller: " + seller);
        sellerLabel.setStyle("-fx-font-size: 13px;");
        sellerLabel.getStyleClass().add("text-secondary");

        card.getChildren().addAll(title, price, timeLabel, sellerLabel);
        if (ClientSession.getInstance().isBidder()) {
            card.getChildren().add(createBidderActions(auction));
        } else {
            Button btnDetail = new Button("Xem chi tiết");
            btnDetail.setMaxWidth(Double.MAX_VALUE);
            btnDetail.setStyle(
                "-fx-background-color: #4ecdc4; " +
                "-fx-text-fill: #1a1a2e; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 8; " +
                "-fx-padding: 8 16; " +
                "-fx-font-size: 13px; " +
                "-fx-cursor: hand;"
            );
            btnDetail.setOnAction(e -> openAuctionDetail(btnDetail, auction));
            card.getChildren().add(btnDetail);
        }
        return card;
    }

    private VBox createBidderActions(AuctionDTO auction) {
        VBox actions = new VBox(6);

        Button registerButton = new Button("Đăng ký đấu giá");
        registerButton.getStyleClass().add("button");
        registerButton.setMaxWidth(Double.MAX_VALUE);
        registerButton.setDisable(auction.getStatus() != AuctionStatus.RUNNING);
        registerButton.setOnAction(e -> openBiddingView(registerButton, auction));

        Hyperlink detailLink = new Hyperlink("Xem chi tiết thông tin");
        detailLink.setStyle(
                "-fx-text-fill: #14b8a6; " +
                "-fx-font-weight: bold; " +
                "-fx-border-color: transparent; " +
                "-fx-padding: 0;");
        detailLink.setOnAction(e -> openAuctionDetail(detailLink, auction));

        HBox detailLinkBox = new HBox(detailLink);
        detailLinkBox.setAlignment(javafx.geometry.Pos.CENTER);
        detailLinkBox.setMaxWidth(Double.MAX_VALUE);

        actions.getChildren().addAll(registerButton, detailLinkBox);
        return actions;
    }

    private void openBiddingView(Node source, AuctionDTO auction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/BiddingView.fxml"));
            Parent root = loader.load();

            BiddingController biddingController = loader.getController();
            biddingController.setAuctionId(auction.getId());

            replaceContent(source, root);
        } catch (Exception ex) {
            System.out.println("Lỗi chuyển màn hình: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void openAuctionDetail(Node source, AuctionDTO auction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AuctionDetailView.fxml"));
            Parent root = loader.load();

            AuctionDetailController detailController = loader.getController();
            detailController.setAuctionData(auction.getId());

            replaceContent(source, root);
        } catch (Exception ex) {
            System.out.println("Lỗi chuyển màn hình: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void replaceContent(Node source, Parent root) {
        Parent currentRoot = source.getScene().getRoot();
        if (currentRoot instanceof HBox) {
            VBox mainCard = (VBox) ((HBox) currentRoot).getChildren().get(1);
            javafx.scene.layout.StackPane contentArea =
                    (javafx.scene.layout.StackPane) mainCard.getChildren().get(1);
            contentArea.getChildren().setAll(root);
        } else if (currentRoot instanceof javafx.scene.layout.BorderPane) {
            ((javafx.scene.layout.BorderPane) currentRoot).setCenter(root);
        } else {
            System.out.println("Dashboard layout không khớp.");
        }
    }

    private String formatTime(LocalDateTime time) {
        return time != null ? time.format(DISPLAY_TIME) : "Chưa rõ";
    }

    private void openAuctionList() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AuctionListView.fxml"));
            Parent root = loader.load();

            Parent currentRoot = btnExplore.getScene().getRoot();
            if (currentRoot instanceof HBox) {
                VBox mainCard = (VBox) ((HBox) currentRoot).getChildren().get(1);
                javafx.scene.layout.StackPane contentArea =
                        (javafx.scene.layout.StackPane) mainCard.getChildren().get(1);
                contentArea.getChildren().setAll(root);
            } else if (currentRoot instanceof javafx.scene.layout.BorderPane) {
                ((javafx.scene.layout.BorderPane) currentRoot).setCenter(root);
            }
        } catch (Exception e) {
            System.out.println("Lỗi chuyển sang danh sách đấu giá: " + e.getMessage());
        }
    }
}
