package com.auctionuet.client.view;

import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.network.AuctionClient;
import com.auctionuet.client.network.BidClient;
import com.auctionuet.client.network.ProfileClient;
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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

public class ProfileController {

    @FXML private Label usernameLabel;
    @FXML private Label roleLabel;
    @FXML private Label balanceLabel;
    @FXML private Button btnToggleEditProfile;
    @FXML private Button btnSaveProfile;
    @FXML private Button btnChooseUsernameEdit;
    @FXML private Button btnChoosePasswordEdit;
    @FXML private VBox editProfileBox;
    @FXML private VBox usernameEditBox;
    @FXML private VBox passwordEditBox;
    @FXML private TextField editUsernameField;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label profileMessageLabel;

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
            editUsernameField.setText(currentUser.getUsername());

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
        refreshProfileFromServer();
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
    private void handleToggleEditProfile() {
        boolean shouldShow = !editProfileBox.isVisible();
        editProfileBox.setVisible(shouldShow);
        editProfileBox.setManaged(shouldShow);
        btnToggleEditProfile.setText(shouldShow ? "Ẩn chỉnh sửa" : "Chỉnh sửa Profile");
        if (shouldShow) {
            UserDTO currentUser = ClientSession.getInstance().getCurrentUser();
            editUsernameField.setText(currentUser != null ? currentUser.getUsername() : "");
            clearPasswordFields();
            profileMessageLabel.setText("");
            showUsernameEdit(false);
            showPasswordEdit(false);
        }
    }

    @FXML
    private void handleChooseUsernameEdit() {
        showUsernameEdit(true);
        showPasswordEdit(false);
        clearPasswordFields();
        profileMessageLabel.setText("");
    }

    @FXML
    private void handleChoosePasswordEdit() {
        showUsernameEdit(false);
        showPasswordEdit(true);
        UserDTO currentUser = ClientSession.getInstance().getCurrentUser();
        editUsernameField.setText(currentUser != null ? currentUser.getUsername() : "");
        profileMessageLabel.setText("");
    }

    @FXML
    private void handleSaveProfile() {
        String token = ClientSession.getInstance().getToken();
        UserDTO currentUser = ClientSession.getInstance().getCurrentUser();
        if (token == null || currentUser == null) {
            showProfileMessage("Bạn cần đăng nhập để cập nhật profile.", true);
            return;
        }

        String username = editUsernameField.getText() != null ? editUsernameField.getText().trim() : "";
        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        boolean editingUsername = usernameEditBox.isVisible();
        boolean editingPassword = passwordEditBox.isVisible();
        boolean usernameChanged = editingUsername && !username.isBlank() && !username.equals(currentUser.getUsername());
        boolean passwordTouched = editingPassword;

        if (!editingUsername && !editingPassword) {
            showProfileMessage("Vui lòng chọn Đổi tên hoặc Đổi mật khẩu.", true);
            return;
        }
        if (!usernameChanged && !passwordTouched) {
            showProfileMessage("Chưa có thay đổi nào để lưu.", true);
            return;
        }
        if (passwordTouched && (isBlank(currentPassword) || isBlank(newPassword) || isBlank(confirmPassword))) {
            showProfileMessage("Vui lòng nhập đủ các trường mật khẩu.", true);
            return;
        }
        if (passwordTouched && !newPassword.equals(confirmPassword)) {
            showProfileMessage("Xác nhận mật khẩu mới không khớp.", true);
            return;
        }

        btnSaveProfile.setDisable(true);
        showProfileMessage("Đang cập nhật...", false);
        String requestedUsername = usernameChanged ? username : null;
        String requestedCurrentPassword = passwordTouched ? currentPassword : null;
        String requestedNewPassword = passwordTouched ? newPassword : null;

        new Thread(() -> {
            try {
                UserDTO updatedUser = new ProfileClient().updateProfile(
                        token,
                        requestedUsername,
                        requestedCurrentPassword,
                        requestedNewPassword);
                Platform.runLater(() -> {
                    ClientSession.getInstance().updateCurrentUser(updatedUser);
                    renderCurrentUser(updatedUser);
                    clearPasswordFields();
                    showProfileMessage("Cập nhật profile thành công.", false);
                    if (requestedUsername != null) {
                        showUsernameEdit(false);
                    }
                    if (requestedNewPassword != null) {
                        showPasswordEdit(false);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showProfileMessage("Lỗi: " + e.getMessage(), true));
            } finally {
                Platform.runLater(() -> btnSaveProfile.setDisable(false));
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

    private void refreshProfileFromServer() {
        String token = ClientSession.getInstance().getToken();
        if (token == null) {
            return;
        }

        new Thread(() -> {
            try {
                UserDTO user = new ProfileClient().getProfile(token);
                Platform.runLater(() -> {
                    ClientSession.getInstance().updateCurrentUser(user);
                    renderCurrentUser(user);
                });
            } catch (Exception ignored) {
            }
        }).start();
    }

    private void renderCurrentUser(UserDTO user) {
        if (user == null) {
            return;
        }
        usernameLabel.setText("@" + user.getUsername());
        roleLabel.setText(user.getRole() != null ? user.getRole().name() : "GUEST");
        editUsernameField.setText(user.getUsername());
    }

    private void clearPasswordFields() {
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
    }

    private void showProfileMessage(String message, boolean error) {
        profileMessageLabel.setText(message);
        profileMessageLabel.setTextFill(javafx.scene.paint.Color.web(error ? "#ff4757" : "#22c55e"));
    }

    private void showUsernameEdit(boolean visible) {
        usernameEditBox.setVisible(visible);
        usernameEditBox.setManaged(visible);
        setChoiceButtonActive(btnChooseUsernameEdit, visible);
    }

    private void showPasswordEdit(boolean visible) {
        passwordEditBox.setVisible(visible);
        passwordEditBox.setManaged(visible);
        setChoiceButtonActive(btnChoosePasswordEdit, visible);
    }

    private void setChoiceButtonActive(Button button, boolean active) {
        String color = active ? "#4ecdc4" : "transparent";
        String textColor = active ? "#1a1a2e" : "#4ecdc4";
        button.setStyle("-fx-background-color: " + color
                + "; -fx-border-color: #4ecdc4; -fx-border-radius: 8; -fx-background-radius: 8;"
                + " -fx-text-fill: " + textColor + "; -fx-padding: 8; -fx-font-weight: bold;");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
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
