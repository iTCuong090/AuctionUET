package com.auctionuet.client.view;

import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.network.AuctionClient;
import com.auctionuet.protocol.dto.response.auction.AuctionDTO;
import com.auctionuet.protocol.dto.response.user.UserDTO;
import com.auctionuet.protocol.enums.AuctionStatus;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PaymentController {
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int PAID_VISIBLE_DAYS = 30;

    @FXML private FlowPane paymentGrid;
    @FXML private Label statusLabel;
    @FXML private Button btnAllPayments, btnPendingPayments, btnPaidPayments;

    private final AuctionClient auctionClient = new AuctionClient();
    private List<AuctionDTO> pendingPayments = new ArrayList<>();
    private List<AuctionDTO> paidPayments = new ArrayList<>();
    private PaymentFilter currentFilter = PaymentFilter.ALL;

    @FXML
    public void initialize() {
        setupFilterButtons();
        loadPaymentData();
    }

    private void setupFilterButtons() {
        btnAllPayments.setOnAction(e -> selectFilter(PaymentFilter.ALL));
        btnPendingPayments.setOnAction(e -> selectFilter(PaymentFilter.PENDING));
        btnPaidPayments.setOnAction(e -> selectFilter(PaymentFilter.PAID));
        updateFilterButtonStyles();
    }

    private void selectFilter(PaymentFilter filter) {
        currentFilter = filter;
        updateFilterButtonStyles();
        renderPayments();
    }

    private void updateFilterButtonStyles() {
        updateFilterButtonStyle(btnAllPayments, currentFilter == PaymentFilter.ALL);
        updateFilterButtonStyle(btnPendingPayments, currentFilter == PaymentFilter.PENDING);
        updateFilterButtonStyle(btnPaidPayments, currentFilter == PaymentFilter.PAID);
    }

    private void updateFilterButtonStyle(Button button, boolean active) {
        if (button == null) {
            return;
        }
        button.getStyleClass().remove("active");
        if (active) {
            button.getStyleClass().add("active");
        }
    }

    private void loadPaymentData() {
        String token = ClientSession.getInstance().getToken();
        if (token == null) {
            statusLabel.setText("Bạn chưa đăng nhập.");
            return;
        }

        new Thread(() -> {
            try {
                List<AuctionDTO> pending = auctionClient.getMyPendingPayments(token);
                List<AuctionDTO> paid = loadRecentPaidPayments(token);
                Platform.runLater(() -> {
                    pendingPayments = sortedPendingPayments(pending);
                    paidPayments = sortedPaidPayments(paid);
                    renderPayments();
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Lỗi tải danh sách thanh toán: " + e.getMessage()));
            }
        }).start();
    }

    private List<AuctionDTO> loadRecentPaidPayments(String token) throws Exception {
        UserDTO currentUser = ClientSession.getInstance().getCurrentUser();
        String currentUserId = currentUser != null ? currentUser.getId() : null;
        if (currentUserId == null || currentUserId.isBlank()) {
            return List.of();
        }

        LocalDateTime threshold = LocalDateTime.now().minusDays(PAID_VISIBLE_DAYS);
        return auctionClient.getAuctions(token).stream()
                .filter(auction -> auction.getStatus() == AuctionStatus.PAID)
                .filter(auction -> auction.getWinner() != null
                        && currentUserId.equals(auction.getWinner().getId()))
                .filter(auction -> paidTimeOf(auction) != null && !paidTimeOf(auction).isBefore(threshold))
                .toList();
    }

    private List<AuctionDTO> sortedPendingPayments(List<AuctionDTO> auctions) {
        if (auctions == null) {
            return List.of();
        }
        return auctions.stream()
                .sorted(Comparator.comparing(
                        this::deadlineTimeOf,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private List<AuctionDTO> sortedPaidPayments(List<AuctionDTO> auctions) {
        if (auctions == null) {
            return List.of();
        }
        return auctions.stream()
                .sorted(Comparator.comparing(
                        this::paidTimeOf,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private void renderPayments() {
        List<AuctionDTO> auctions = visiblePayments();
        paymentGrid.getChildren().clear();
        if (auctions == null || auctions.isEmpty()) {
            statusLabel.setText(emptyMessage());
            return;
        }

        statusLabel.setText(statusMessage(auctions.size()));
        for (AuctionDTO auction : auctions) {
            paymentGrid.getChildren().add(createPaymentCard(auction));
        }
    }

    private List<AuctionDTO> visiblePayments() {
        if (currentFilter == PaymentFilter.PENDING) {
            return pendingPayments;
        }
        if (currentFilter == PaymentFilter.PAID) {
            return paidPayments;
        }

        List<AuctionDTO> all = new ArrayList<>();
        all.addAll(pendingPayments);
        all.addAll(paidPayments);
        return all;
    }

    private String emptyMessage() {
        return switch (currentFilter) {
            case ALL -> "Không có phiên cần thanh toán hoặc đã thanh toán trong 30 ngày.";
            case PENDING -> "Không có phiên đấu giá nào đang chờ thanh toán.";
            case PAID -> "Không có phiên đấu giá nào đã thanh toán trong 30 ngày.";
        };
    }

    private String statusMessage(int count) {
        return switch (currentFilter) {
            case ALL -> "Có " + count + " phiên trong danh sách thanh toán.";
            case PENDING -> "Có " + count + " phiên đang chờ thanh toán.";
            case PAID -> "Có " + count + " phiên đã thanh toán trong 30 ngày.";
        };
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
        boolean paid = auction.getStatus() == AuctionStatus.PAID;
        Label price = new Label(paid
                ? "Đã thanh toán: " + CurrencyFormatter.format(auction.getCurrentPrice())
                : "Còn cần trả: " + CurrencyFormatter.format(remaining));
        price.getStyleClass().add("text-accent");
        price.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Label total = new Label("Giá thắng: " + CurrencyFormatter.format(auction.getCurrentPrice())
                + " | Cọc: " + CurrencyFormatter.format(deposit));
        total.getStyleClass().add("text-secondary");
        total.setWrapText(true);

        Label time = new Label(paid
                ? "Thanh toán: " + formatTime(paidTimeOf(auction))
                : "Hạn: " + formatTime(auction.getPaymentDeadlineAt()));
        time.getStyleClass().add("text-secondary");

        Button payButton = new Button(paid ? "Xem chi tiết" : "Thanh toán");
        payButton.getStyleClass().add("button");
        payButton.setMaxWidth(Double.MAX_VALUE);
        payButton.setOnAction(e -> openAuctionDetail(payButton, auction.getId()));

        card.getChildren().addAll(title, price, total, time, payButton);
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

    private LocalDateTime deadlineTimeOf(AuctionDTO auction) {
        return auction != null ? auction.getPaymentDeadlineAt() : null;
    }

    private LocalDateTime paidTimeOf(AuctionDTO auction) {
        if (auction == null) {
            return null;
        }
        return auction.getPaidAt() != null ? auction.getPaidAt() : auction.getEndTime();
    }

    private enum PaymentFilter {
        ALL,
        PENDING,
        PAID
    }
}
