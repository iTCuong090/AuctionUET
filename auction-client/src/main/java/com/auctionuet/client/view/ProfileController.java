package com.auctionuet.client.view;

import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.network.AuctionClient;
import com.auctionuet.client.network.WalletClient;
import com.auctionuet.protocol.dto.response.auction.AuctionDTO;
import com.auctionuet.protocol.dto.response.user.UserDTO;
import com.auctionuet.protocol.dto.response.wallet.WalletResponseDTO;
import com.auctionuet.protocol.enums.AuctionStatus;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

public class ProfileController {

    @FXML private Label usernameLabel;
    @FXML private Label roleLabel;
    @FXML private Label balanceLabel;

    @FXML private Label statWonLabel;
    @FXML private Label statSoldLabel;
    @FXML private Label statReputationLabel;
    @FXML private Label statActiveLabel;

    @FXML
    public void initialize() {
        UserDTO currentUser = ClientSession.getInstance().getCurrentUser();

        if (currentUser != null) {
            usernameLabel.setText("@" + currentUser.getUsername());

            String role = currentUser.getRole() != null ? currentUser.getRole().name() : "GUEST";
            roleLabel.setText(role);

            if ("SELLER".equals(role)) {
                roleLabel.setStyle("-fx-background-color: #e94560; -fx-background-radius: 10; -fx-padding: 6 18;");
            } else {
                roleLabel.setStyle("-fx-background-color: #3498db; -fx-background-radius: 10; -fx-padding: 6 18;");
            }

            balanceLabel.setText("Đang tải...");
        } else {
            usernameLabel.setText("Chưa đăng nhập");
            roleLabel.setText("N/A");
            balanceLabel.setText("0 VND");
        }

        loadProfileStats();
        loadWalletBalance();
    }

    private void loadWalletBalance() {
        String token = ClientSession.getInstance().getToken();
        if (token == null) return;

        new Thread(() -> {
            try {
                WalletClient walletClient = new WalletClient();
                WalletResponseDTO walletInfo = walletClient.getWallet(token);
                double balance = walletInfo != null ? walletInfo.getBalance() : 0.0;
                Platform.runLater(() -> balanceLabel.setText(String.format("%,.0f VND", balance)));
            } catch (Exception e) {
                Platform.runLater(() -> balanceLabel.setText("Lỗi"));
            }
        }).start();
    }

    @FXML
    private void handleTopUp() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/WalletView.fxml"));
            Parent root = loader.load();

            Parent currentRoot = balanceLabel.getScene().getRoot();
            if (currentRoot instanceof HBox) {
                VBox mainCard = (VBox) ((HBox) currentRoot).getChildren().get(1);
                StackPane contentArea = (StackPane) mainCard.getChildren().get(1);
                contentArea.getChildren().setAll(root);
            } else if (currentRoot instanceof javafx.scene.layout.BorderPane) {
                ((javafx.scene.layout.BorderPane) currentRoot).setCenter(root);
            }
        } catch (Exception e) {
            System.err.println("Lỗi chuyển sang ví: " + e.getMessage());
        }
    }

    private void loadProfileStats() {
        String token = ClientSession.getInstance().getToken();
        UserDTO currentUser = ClientSession.getInstance().getCurrentUser();

        if (token == null || currentUser == null) {
            setStatsDefault();
            return;
        }

        String myUserId = currentUser.getId();

        new Thread(() -> {
            try {
                AuctionClient client = new AuctionClient();
                List<AuctionDTO> allAuctions = client.getAuctions(token);

                long wonCount = allAuctions.stream()
                        .filter(a -> a.getStatus() == AuctionStatus.FINISHED)
                        .filter(a -> a.getWinner() != null && myUserId.equals(a.getWinner().getId()))
                        .count();

                long soldCount = allAuctions.stream()
                        .filter(a -> a.getStatus() == AuctionStatus.FINISHED)
                        .filter(a -> a.getSeller() != null && myUserId.equals(a.getSeller().getId()))
                        .count();

                long activeCount = allAuctions.stream()
                        .filter(a -> a.getStatus() == AuctionStatus.RUNNING)
                        .count();

                String reputation = calculateReputation((int) (wonCount + soldCount));

                Platform.runLater(() -> {
                    statWonLabel.setText(String.valueOf(wonCount));
                    statSoldLabel.setText(String.valueOf(soldCount));
                    statActiveLabel.setText(String.valueOf(activeCount));
                    statReputationLabel.setText(reputation);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.out.println("Lỗi tải thống kê Profile: " + e.getMessage());
                    setStatsDefault();
                });
            }
        }).start();
    }

    private String calculateReputation(int totalTransactions) {
        int stars = Math.min(5, Math.max(1, (totalTransactions / 2) + 1));
        return "*".repeat(stars) + "-".repeat(5 - stars);
    }

    private void setStatsDefault() {
        statWonLabel.setText("0");
        statSoldLabel.setText("0");
        statActiveLabel.setText("0");
        statReputationLabel.setText("*----");
    }
}
