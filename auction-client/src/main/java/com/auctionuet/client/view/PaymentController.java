package com.auctionuet.client.view;

import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.network.AuctionClient;
import com.auctionuet.protocol.dto.response.auction.AuctionDTO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PaymentController {
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private FlowPane paymentGrid;
    @FXML private Label statusLabel;

    private final AuctionClient auctionClient = new AuctionClient();

    @FXML
    public void initialize() {
        loadPendingPayments();
    }

    private void loadPendingPayments() {
        String token = ClientSession.getInstance().getToken();
        if (token == null) {
            statusLabel.setText("Bạn chưa đăng nhập.");
            return;
        }

        new Thread(() -> {
            try {
                List<AuctionDTO> auctions = auctionClient.getMyPendingPayments(token);
                Platform.runLater(() -> renderPayments(auctions));
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Lỗi tải danh sách thanh toán: " + e.getMessage()));
            }
        }).start();
    }

    private void renderPayments(List<AuctionDTO> auctions) {
        paymentGrid.getChildren().clear();
        if (auctions == null || auctions.isEmpty()) {
            statusLabel.setText("Không có phiên đấu giá nào đang chờ thanh toán.");
            return;
        }

        statusLabel.setText("Có " + auctions.size() + " phiên đang chờ thanh toán.");
        for (AuctionDTO auction : auctions) {
            paymentGrid.getChildren().add(createPaymentCard(auction));
        }
    }

    private VBox createPaymentCard(AuctionDTO auction) {
        VBox card = new VBox(12);
        card.getStyleClass().add("auction-card");
        card.setPrefWidth(260);

        Label title = new Label(auction.getTitle());
        title.getStyleClass().add("text-primary");
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold;");
        title.setWrapText(true);

        double deposit = auction.getDepositAmount();
        double remaining = Math.max(0, auction.getCurrentPrice() - deposit);
        Label price = new Label(String.format("Còn cần trả: %,.0f VND", remaining));
        price.getStyleClass().add("text-accent");
        price.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Label total = new Label(String.format("Giá thắng: %,.0f VND | Cọc: %,.0f VND",
                auction.getCurrentPrice(), deposit));
        total.getStyleClass().add("text-secondary");
        total.setWrapText(true);

        Label deadline = new Label("Hạn: " + formatTime(auction.getPaymentDeadlineAt()));
        deadline.getStyleClass().add("text-secondary");

        Button payButton = new Button("Thanh toán");
        payButton.getStyleClass().add("button");
        payButton.setMaxWidth(Double.MAX_VALUE);
        payButton.setOnAction(e -> openAuctionDetail(payButton, auction.getId()));

        card.getChildren().addAll(title, price, total, deadline, payButton);
        return card;
    }

    private void openAuctionDetail(Button source, String auctionId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AuctionDetailView.fxml"));
            Parent root = loader.load();
            AuctionDetailController controller = loader.getController();
            controller.setAuctionData(auctionId);

            Parent currentRoot = source.getScene().getRoot();
            if (currentRoot instanceof HBox) {
                VBox mainCard = (VBox) ((HBox) currentRoot).getChildren().get(1);
                javafx.scene.layout.StackPane contentArea =
                        (javafx.scene.layout.StackPane) mainCard.getChildren().get(1);
                contentArea.getChildren().setAll(root);
            }
        } catch (Exception e) {
            statusLabel.setText("Lỗi mở chi tiết phiên đấu giá: " + e.getMessage());
        }
    }

    private String formatTime(LocalDateTime time) {
        return time != null ? time.format(DISPLAY_TIME) : "Chưa rõ";
    }
}
