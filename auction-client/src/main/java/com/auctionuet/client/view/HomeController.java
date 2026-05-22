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
import javafx.scene.control.Label;
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

                int total = allAuctions.size();
                long running = allAuctions.stream()
                        .filter(a -> a.getStatus() == AuctionStatus.RUNNING)
                        .count();
                long finished = allAuctions.stream()
                        .filter(a -> a.getStatus() == AuctionStatus.FINISHED)
                        .count();

                List<AuctionDTO> endingSoon = allAuctions.stream()
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

        Label price = new Label(CurrencyFormatter.format(auction.getCurrentPrice()));
        price.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        price.getStyleClass().add("text-accent");

        Label timeLabel = new Label("Kết thúc: " + formatTime(auction.getEndTime()));
        timeLabel.setStyle("-fx-font-size: 13px;");
        timeLabel.getStyleClass().add("text-detail");

        String seller = auction.getSeller() != null ? auction.getSeller().getUsername() : "Chưa rõ";
        Label sellerLabel = new Label("Seller: " + seller);
        sellerLabel.setStyle("-fx-font-size: 13px;");
        sellerLabel.getStyleClass().add("text-secondary");

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

        btnDetail.setOnAction(e -> {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                        getClass().getResource("/fxml/AuctionDetailView.fxml"));
                javafx.scene.Parent detailRoot = loader.load();
                AuctionDetailController detailCtrl = loader.getController();
                detailCtrl.setAuctionData(auction.getId());

                javafx.scene.Parent currentRoot = btnDetail.getScene().getRoot();
                if (currentRoot instanceof javafx.scene.layout.BorderPane) {
                    ((javafx.scene.layout.BorderPane) currentRoot).setCenter(detailRoot);
                }
            } catch (Exception ex) {
                System.out.println("Lỗi chuyển trang chi tiết: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        card.getChildren().addAll(title, price, timeLabel, sellerLabel, btnDetail);
        return card;
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
