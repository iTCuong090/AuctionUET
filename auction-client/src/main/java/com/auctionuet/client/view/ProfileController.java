package com.auctionuet.client.view;

import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.network.AuctionClient;
import com.auctionuet.client.network.BidClient;
import com.auctionuet.client.network.WalletClient;
import com.auctionuet.protocol.dto.response.auction.AuctionDTO;
import com.auctionuet.protocol.dto.response.bid.BidDTO;
import com.auctionuet.protocol.dto.response.user.UserDTO;
import com.auctionuet.protocol.dto.response.wallet.WalletResponseDTO;
import com.auctionuet.protocol.enums.AuctionStatus;
import com.auctionuet.protocol.enums.UserRole;
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
    @FXML private Label statSecondTitleLabel;
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
            statSecondTitleLabel.setText(getSecondStatTitle(currentUser));

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
                Platform.runLater(() -> CurrencyFormatter.setMoneyText(balanceLabel, balance));
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
        boolean isSeller = isSeller(currentUser);

        new Thread(() -> {
            try {
                AuctionClient client = new AuctionClient();
                List<AuctionDTO> allAuctions = client.getAuctions(token);

                long wonCount = allAuctions.stream()
                        .filter(this::isWonAuction)
                        .filter(a -> a.getWinner() != null && myUserId.equals(a.getWinner().getId()))
                        .count();

                long soldCount = allAuctions.stream()
                        .filter(a -> a.getStatus() == AuctionStatus.PAID)
                        .filter(a -> a.getSeller() != null && myUserId.equals(a.getSeller().getId()))
                        .count();

                long activeCount = allAuctions.stream()
                        .filter(a -> isActiveForCurrentUser(a, myUserId))
                        .count();

                long secondStatCount = isSeller
                        ? soldCount
                        : countParticipatedAuctions(token, myUserId, allAuctions);

                String reputation = calculateReputation((int) (wonCount + soldCount));

                Platform.runLater(() -> {
                    statWonLabel.setText(String.valueOf(wonCount));
                    statSoldLabel.setText(String.valueOf(secondStatCount));
                    statSecondTitleLabel.setText(isSeller ? "Sản phẩm đã bán" : "Phiên đã tham gia");
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

    private long countParticipatedAuctions(String token, String userId, List<AuctionDTO> auctions) {
        BidClient bidClient = new BidClient();
        return auctions.stream()
                .filter(auction -> hasParticipation(token, userId, auction, bidClient))
                .count();
    }

    private boolean hasParticipation(
            String token, String userId, AuctionDTO auction, BidClient bidClient) {
        if (isCurrentUserKnownParticipant(auction, userId)) {
            return true;
        }

        try {
            List<BidDTO> bids = bidClient.getBidHistory(token, auction.getId());
            return bids != null && bids.stream()
                    .anyMatch(bid -> bid.getBidder() != null && userId.equals(bid.getBidder().getId()));
        } catch (Exception ignored) {
            return false;
        }
    }

    private String getSecondStatTitle(UserDTO user) {
        return isSeller(user) ? "Sản phẩm đã bán" : "Phiên đã tham gia";
    }

    private boolean isSeller(UserDTO user) {
        return user != null && user.getRole() == UserRole.SELLER;
    }

    private boolean isCurrentUserKnownParticipant(AuctionDTO auction, String userId) {
        boolean isWinner = auction.getWinner() != null && userId.equals(auction.getWinner().getId());
        boolean isHighestBidder = auction.getCurrentHighestBid() != null
                && auction.getCurrentHighestBid().getBidder() != null
                && userId.equals(auction.getCurrentHighestBid().getBidder().getId());
        return auction.isCurrentUserDeposited() || isWinner || isHighestBidder;
    }

    private boolean isWonAuction(AuctionDTO auction) {
        return auction.getStatus() == AuctionStatus.WAITING_PAYMENT
                || auction.getStatus() == AuctionStatus.PAID;
    }

    private boolean isActiveForCurrentUser(AuctionDTO auction, String userId) {
        AuctionStatus status = auction.getStatus();
        boolean isSeller = auction.getSeller() != null && userId.equals(auction.getSeller().getId());
        boolean isWinnerWaitingPayment = status == AuctionStatus.WAITING_PAYMENT
                && auction.getWinner() != null
                && userId.equals(auction.getWinner().getId());
        boolean isRunningBidder = status == AuctionStatus.RUNNING && auction.isCurrentUserDeposited();

        return (isSeller && isBlockingSellerStatus(status))
                || isWinnerWaitingPayment
                || isRunningBidder;
    }

    private boolean isBlockingSellerStatus(AuctionStatus status) {
        return status == AuctionStatus.OPEN
                || status == AuctionStatus.RUNNING
                || status == AuctionStatus.WAITING_PAYMENT;
    }

    private String calculateReputation(int totalTransactions) {
        int stars = Math.min(5, Math.max(1, (totalTransactions / 2) + 1));
        return "★".repeat(stars) + "☆".repeat(5 - stars);
    }

    private void setStatsDefault() {
        statWonLabel.setText("0");
        statSoldLabel.setText("0");
        statSecondTitleLabel.setText(getSecondStatTitle(ClientSession.getInstance().getCurrentUser()));
        statActiveLabel.setText("0");
        statReputationLabel.setText("★☆☆☆☆");
    }
}
