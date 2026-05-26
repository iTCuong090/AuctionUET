package com.auctionuet.client.view;

import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.network.WalletClient;
import com.auctionuet.protocol.dto.response.user.UserDTO;
import com.auctionuet.protocol.dto.response.wallet.WalletResponseDTO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.Consumer;

public class PaymentCheckoutController {
    private static final double CHECKOUT_GAP = 24.0;
    private static final double CHECKOUT_STACK_WIDTH = 820.0;
    private static final double CHECKOUT_RIGHT_WIDTH = 340.0;

    @FXML private FlowPane checkoutFlow;
    @FXML private Label checkoutTitleLabel;

    private final WalletClient walletClient = new WalletClient();
    private double depositAmount;
    private VBox checkoutLeftColumn;
    private VBox checkoutActionCard;
    private Label checkoutMessageLabel;
    private Button btnConfirmCheckout;
    private Consumer<WalletResponseDTO> walletDepositSuccessCallback;

    @FXML
    public void initialize() {
        checkoutFlow.widthProperty().addListener((obs, oldWidth, newWidth) ->
                syncCheckoutWidth(newWidth.doubleValue()));
    }

    public void setDepositData(double amount, Consumer<WalletResponseDTO> walletDepositSuccessCallback) {
        this.depositAmount = amount;
        this.walletDepositSuccessCallback = walletDepositSuccessCallback;
        renderCheckout();
        Platform.runLater(() -> syncCheckoutWidth(checkoutFlow.getWidth()));
    }

    private void renderCheckout() {
        if (depositAmount <= 0) {
            return;
        }

        checkoutTitleLabel.setText("💳 Nạp tiền");
        checkoutLeftColumn = createDepositInfoCard();
        checkoutActionCard = createDepositActionCard();
        checkoutFlow.setHgap(CHECKOUT_GAP);
        checkoutFlow.setVgap(CHECKOUT_GAP);
        checkoutFlow.getStyleClass().add("payment-checkout-flow");
        checkoutFlow.getChildren().setAll(checkoutLeftColumn, checkoutActionCard);
        syncCheckoutWidth(checkoutFlow.getWidth());
    }

    private void syncCheckoutWidth(double availableWidth) {
        if (checkoutLeftColumn == null || checkoutActionCard == null) {
            return;
        }

        double width = availableWidth > 0 ? availableWidth : checkoutFlow.getPrefWidth();
        boolean stacked = width < CHECKOUT_STACK_WIDTH;
        double leftWidth = stacked
                ? width
                : Math.max(360, width - CHECKOUT_RIGHT_WIDTH - CHECKOUT_GAP);
        double rightWidth = stacked ? width : CHECKOUT_RIGHT_WIDTH;

        applyRegionWidth(checkoutLeftColumn, leftWidth);
        applyRegionWidth(checkoutActionCard, rightWidth);
    }

    private void applyRegionWidth(Region region, double width) {
        region.setMinWidth(width);
        region.setPrefWidth(width);
        region.setMaxWidth(width);
    }

    private VBox createDepositInfoCard() {
        VBox card = new VBox(12);
        card.getStyleClass().addAll("payment-info-card", "payment-checkout-column");

        Label title = new Label("Thông tin nạp tiền");
        title.getStyleClass().add("payment-card-title");

        UserDTO currentUser = ClientSession.getInstance().getCurrentUser();
        card.getChildren().addAll(
                title,
                createInfoRow("Tài khoản", usernameOf(currentUser)),
                createInfoRow("Nội dung chuyển khoản", depositReference()),
                createDivider(),
                createAmountDueRow(
                        "Số tiền nạp",
                        CurrencyFormatter.format(depositAmount),
                        CurrencyFormatter.formatFull(depositAmount)));
        return card;
    }

    private HBox createInfoRow(String labelText, String valueText) {
        return createInfoRow(labelText, valueText, null);
    }

    private HBox createInfoRow(String labelText, String valueText, String tooltipText) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        Label label = new Label(labelText);
        label.getStyleClass().add("payment-info-label");
        Label value = new Label(valueText);
        value.getStyleClass().add("payment-info-value");
        value.setWrapText(true);
        if (tooltipText != null && !tooltipText.isBlank()) {
            CurrencyFormatter.installTooltip(value, tooltipText);
        }
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().addAll(label, spacer, value);
        return row;
    }

    private HBox createAmountDueRow(String labelText, String valueText, String tooltipText) {
        HBox row = createInfoRow(labelText, valueText, tooltipText);
        row.getStyleClass().add("payment-total-row");
        return row;
    }

    private Region createDivider() {
        Region divider = new Region();
        divider.getStyleClass().add("payment-divider");
        divider.setMinHeight(1);
        divider.setPrefHeight(1);
        return divider;
    }

    private VBox createDepositActionCard() {
        VBox card = new VBox(12);
        card.setAlignment(Pos.TOP_CENTER);
        card.getStyleClass().add("payment-action-card");
        card.getChildren().addAll(createDepositQrContent());

        checkoutMessageLabel = new Label("");
        checkoutMessageLabel.getStyleClass().add("payment-message");
        checkoutMessageLabel.setWrapText(true);

        btnConfirmCheckout = new Button("Xác nhận đã chuyển khoản");
        btnConfirmCheckout.getStyleClass().addAll("button", "payment-confirm-button");
        btnConfirmCheckout.setMaxWidth(Double.MAX_VALUE);
        btnConfirmCheckout.setOnAction(e -> confirmWalletDeposit());

        Button btnBack = new Button("Quay về");
        btnBack.getStyleClass().add("payment-back-button");
        btnBack.setMaxWidth(Double.MAX_VALUE);
        btnBack.setOnAction(e -> closePopup());

        card.getChildren().addAll(checkoutMessageLabel, btnConfirmCheckout, btnBack);
        return card;
    }

    private List<Node> createDepositQrContent() {
        Label title = new Label("Quét mã QR để nạp tiền");
        title.getStyleClass().add("payment-card-title");

        Label brand = new Label("VIETQR");
        brand.getStyleClass().add("payment-qr-brand");

        GridPane qrMock = createQrMock(depositReference());
        HBox qrHolder = new HBox(qrMock);
        qrHolder.setAlignment(Pos.CENTER);
        qrHolder.setMaxWidth(Double.MAX_VALUE);

        Label amount = new Label();
        CurrencyFormatter.setMoneyText(amount, "Số tiền nạp: ", depositAmount);
        amount.getStyleClass().add("payment-action-amount");
        amount.setWrapText(true);

        Label ref = new Label("Nội dung chuyển khoản: " + depositReference());
        ref.getStyleClass().add("text-secondary");
        ref.setWrapText(true);

        return List.of(title, brand, qrHolder, amount, ref);
    }

    private GridPane createQrMock(String paymentReference) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("payment-qr-grid");
        grid.setAlignment(Pos.CENTER);
        grid.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        int seed = paymentReference != null ? Math.abs(paymentReference.hashCode()) : 1;
        for (int row = 0; row < 17; row++) {
            for (int col = 0; col < 17; col++) {
                Region cell = new Region();
                cell.setMinSize(7, 7);
                cell.setPrefSize(7, 7);
                cell.setMaxSize(7, 7);
                boolean finder = isQrFinderCell(row, col);
                boolean dark = finder || ((row * 31 + col * 17 + seed) % 5 < 2);
                cell.getStyleClass().add(dark ? "payment-qr-cell-dark" : "payment-qr-cell-light");
                grid.add(cell, col, row);
            }
        }
        return grid;
    }

    private boolean isQrFinderCell(int row, int col) {
        return isFinderBlock(row, col, 0, 0)
                || isFinderBlock(row, col, 0, 12)
                || isFinderBlock(row, col, 12, 0);
    }

    private boolean isFinderBlock(int row, int col, int startRow, int startCol) {
        int localRow = row - startRow;
        int localCol = col - startCol;
        if (localRow < 0 || localRow > 4 || localCol < 0 || localCol > 4) {
            return false;
        }
        return localRow == 0 || localRow == 4 || localCol == 0 || localCol == 4
                || (localRow == 2 && localCol == 2);
    }

    private void confirmWalletDeposit() {
        String token = ClientSession.getInstance().getToken();
        btnConfirmCheckout.setDisable(true);
        checkoutMessageLabel.setText("Đang xác nhận nạp tiền...");
        checkoutMessageLabel.getStyleClass().removeAll("success", "error");

        new Thread(() -> {
            try {
                WalletResponseDTO wallet = walletClient.deposit(token, depositAmount);
                Platform.runLater(() -> {
                    if (walletDepositSuccessCallback != null) {
                        walletDepositSuccessCallback.accept(wallet);
                    }
                    closePopup();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    checkoutMessageLabel.setText("Lỗi nạp tiền: " + e.getMessage());
                    checkoutMessageLabel.getStyleClass().removeAll("success", "error");
                    checkoutMessageLabel.getStyleClass().add("error");
                    btnConfirmCheckout.setDisable(false);
                });
            }
        }).start();
    }

    private void closePopup() {
        if (checkoutFlow != null && checkoutFlow.getScene() != null
                && checkoutFlow.getScene().getWindow() instanceof Stage stage) {
            stage.close();
        }
    }

    private String usernameOf(UserDTO user) {
        return user != null && user.getUsername() != null ? user.getUsername() : "Chưa rõ";
    }

    private String shortId(String id) {
        if (id == null || id.isBlank()) {
            return "UNKNOWN";
        }
        return id.length() <= 8 ? id : id.substring(0, 8);
    }

    private String depositReference() {
        UserDTO user = ClientSession.getInstance().getCurrentUser();
        String userId = user != null ? shortId(user.getId()) : "UNKNOWN";
        return "NAPVI-" + userId;
    }
}
