package com.auctionuet.client.view;

import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.network.AuctionClient;
import com.auctionuet.protocol.dto.response.auction.AuctionDTO;
import com.auctionuet.protocol.dto.response.user.UserDTO;
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

public class PaymentCheckoutController {
    private static final double CHECKOUT_GAP = 24.0;
    private static final double CHECKOUT_STACK_WIDTH = 820.0;
    private static final double CHECKOUT_RIGHT_WIDTH = 340.0;

    @FXML private FlowPane checkoutFlow;

    private final AuctionClient auctionClient = new AuctionClient();
    private AuctionDTO selectedAuction;
    private PaymentMethod selectedPaymentMethod = PaymentMethod.QR;
    private boolean methodMenuOpen;
    private VBox checkoutLeftColumn;
    private VBox checkoutActionCard;
    private Label checkoutMessageLabel;
    private Button btnConfirmCheckout;
    private Runnable paymentSuccessCallback;

    @FXML
    public void initialize() {
        checkoutFlow.widthProperty().addListener((obs, oldWidth, newWidth) ->
                syncCheckoutWidth(newWidth.doubleValue()));
    }

    public void setAuctionData(AuctionDTO auction, Runnable paymentSuccessCallback) {
        this.selectedAuction = auction;
        this.paymentSuccessCallback = paymentSuccessCallback;
        renderCheckout();
        Platform.runLater(() -> syncCheckoutWidth(checkoutFlow.getWidth()));
    }

    private void renderCheckout() {
        if (selectedAuction == null) {
            return;
        }

        checkoutLeftColumn = new VBox(16);
        checkoutLeftColumn.getStyleClass().add("payment-checkout-column");
        checkoutLeftColumn.getChildren().addAll(
                createPaymentMethodCard(),
                createPaymentInfoCard(selectedAuction));

        checkoutActionCard = createPaymentActionCard(selectedAuction);
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

    private VBox createPaymentMethodCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("payment-method-card");

        Label title = new Label("Chọn phương thức thanh toán");
        title.getStyleClass().add("payment-card-title");

        if (methodMenuOpen) {
            VBox menu = new VBox(8);
            menu.getStyleClass().add("payment-method-menu");
            for (PaymentMethod method : PaymentMethod.values()) {
                HBox option = createPaymentMethodRow(method, method == selectedPaymentMethod);
                option.getStyleClass().add("payment-method-option");
                option.setOnMouseClicked(e -> {
                    selectedPaymentMethod = method;
                    methodMenuOpen = false;
                    renderCheckout();
                });
                menu.getChildren().add(option);
            }
            card.getChildren().addAll(title, menu);
        } else {
            HBox selectedRow = createPaymentMethodRow(selectedPaymentMethod, true);
            selectedRow.getStyleClass().add("payment-method-selected");
            selectedRow.setOnMouseClicked(e -> {
                methodMenuOpen = true;
                renderCheckout();
            });
            card.getChildren().addAll(title, selectedRow);
        }
        return card;
    }

    private HBox createPaymentMethodRow(PaymentMethod method, boolean selected) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("payment-method-row");
        if (selected) {
            row.getStyleClass().add("selected");
        }

        Label check = new Label(selected ? "✓" : "");
        check.getStyleClass().add("payment-method-check");

        VBox textBox = new VBox(2);
        Label name = new Label(method.displayName);
        name.getStyleClass().add("text-primary");
        name.setStyle("-fx-font-weight: bold;");
        Label note = new Label(method.note);
        note.getStyleClass().add("text-secondary");
        note.setWrapText(true);
        textBox.getChildren().addAll(name, note);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label brand = new Label(method.badge);
        brand.getStyleClass().add(method == PaymentMethod.QR ? "payment-brand-vietqr" : "payment-brand-cash");

        row.getChildren().addAll(check, textBox, spacer, brand);
        return row;
    }

    private VBox createPaymentInfoCard(AuctionDTO auction) {
        VBox card = new VBox(12);
        card.getStyleClass().add("payment-info-card");

        Label title = new Label("Thông tin thanh toán");
        title.getStyleClass().add("payment-card-title");

        double deposit = auction.getDepositAmount();
        double totalPrice = auction.getCurrentPrice();
        double remaining = remainingPayment(auction);

        card.getChildren().addAll(
                title,
                createInfoRow("Vật phẩm", itemNameOf(auction)),
                createInfoRow("Người bán", usernameOf(auction.getSeller())),
                createInfoRow("Mã phiên", shortId(auction.getId())),
                createDivider(),
                createInfoRow(
                        "Tổng tiền phải trả",
                        CurrencyFormatter.format(totalPrice) + " (đã bao gồm cọc)",
                        CurrencyFormatter.formatFull(totalPrice) + " (đã bao gồm cọc)"),
                createInfoRow("Cọc đã giữ", CurrencyFormatter.format(deposit), CurrencyFormatter.formatFull(deposit)),
                createAmountDueRow(
                        "Cần thanh toán thêm",
                        CurrencyFormatter.format(remaining),
                        CurrencyFormatter.formatFull(remaining)));
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

    private HBox createAmountDueRow(String labelText, String valueText) {
        return createAmountDueRow(labelText, valueText, null);
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

    private VBox createPaymentActionCard(AuctionDTO auction) {
        VBox card = new VBox(12);
        card.setAlignment(Pos.TOP_CENTER);
        card.getStyleClass().add("payment-action-card");

        if (selectedPaymentMethod == PaymentMethod.QR) {
            card.getChildren().addAll(createQrPaymentContent(auction));
        } else {
            card.getChildren().addAll(createCashPaymentContent(auction));
        }

        checkoutMessageLabel = new Label("");
        checkoutMessageLabel.getStyleClass().add("payment-message");
        checkoutMessageLabel.setWrapText(true);

        btnConfirmCheckout = new Button("Xác nhận đã thanh toán");
        btnConfirmCheckout.getStyleClass().addAll("button", "payment-confirm-button");
        btnConfirmCheckout.setMaxWidth(Double.MAX_VALUE);
        btnConfirmCheckout.setOnAction(e -> confirmCheckoutPayment());

        Button btnBack = new Button("Quay về");
        btnBack.getStyleClass().add("payment-back-button");
        btnBack.setMaxWidth(Double.MAX_VALUE);
        btnBack.setOnAction(e -> closePopup());

        card.getChildren().addAll(checkoutMessageLabel, btnConfirmCheckout, btnBack);
        return card;
    }

    private List<Node> createQrPaymentContent(AuctionDTO auction) {
        Label title = new Label("Quét mã QR để thanh toán");
        title.getStyleClass().add("payment-card-title");

        Label brand = new Label("VIETQR");
        brand.getStyleClass().add("payment-qr-brand");

        GridPane qrMock = createQrMock(auction.getId());
        HBox qrHolder = new HBox(qrMock);
        qrHolder.setAlignment(Pos.CENTER);
        qrHolder.setMaxWidth(Double.MAX_VALUE);

        double remaining = remainingPayment(auction);
        Label amount = new Label();
        CurrencyFormatter.setMoneyText(amount, "Số tiền: ", remaining);
        amount.getStyleClass().add("payment-action-amount");
        amount.setWrapText(true);

        Label ref = new Label("Mã phiên: " + shortId(auction.getId()));
        ref.getStyleClass().add("text-secondary");

        return List.of(title, brand, qrHolder, amount, ref);
    }

    private List<Node> createCashPaymentContent(AuctionDTO auction) {
        Label title = new Label("Thanh toán trực tiếp");
        title.getStyleClass().add("payment-card-title");

        VBox instruction = new VBox(8);
        instruction.getStyleClass().add("payment-cash-box");
        Label first = new Label("Giao dịch trực tiếp với người bán.");
        first.getStyleClass().add("text-primary");
        first.setWrapText(true);
        Label second = new Label("Sau khi hoàn tất giao dịch, bấm xác nhận để hệ thống ghi nhận thanh toán.");
        second.getStyleClass().add("text-secondary");
        second.setWrapText(true);
        instruction.getChildren().addAll(first, second);

        double remaining = remainingPayment(auction);
        Label amount = new Label();
        CurrencyFormatter.setMoneyText(amount, "Số tiền cần thanh toán thêm: ", remaining);
        amount.getStyleClass().add("payment-action-amount");
        amount.setWrapText(true);

        Label note = new Label("Tổng tiền phải trả: " + CurrencyFormatter.format(auction.getCurrentPrice())
                + " (đã bao gồm cọc)");
        note.getStyleClass().add("text-secondary");
        note.setWrapText(true);
        CurrencyFormatter.installTooltip(
                note,
                "Tổng tiền phải trả: " + CurrencyFormatter.formatFull(auction.getCurrentPrice())
                        + " (đã bao gồm cọc)");

        return List.of(title, instruction, amount, note);
    }

    private GridPane createQrMock(String auctionId) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("payment-qr-grid");
        grid.setAlignment(Pos.CENTER);
        grid.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        int seed = auctionId != null ? Math.abs(auctionId.hashCode()) : 1;
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

    private void confirmCheckoutPayment() {
        if (selectedAuction == null) {
            return;
        }

        String token = ClientSession.getInstance().getToken();
        String auctionId = selectedAuction.getId();
        btnConfirmCheckout.setDisable(true);
        checkoutMessageLabel.setText("Đang xác nhận thanh toán...");
        checkoutMessageLabel.getStyleClass().removeAll("success", "error");

        new Thread(() -> {
            try {
                auctionClient.payAuction(token, auctionId);
                Platform.runLater(() -> {
                    if (paymentSuccessCallback != null) {
                        paymentSuccessCallback.run();
                    }
                    closePopup();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    checkoutMessageLabel.setText("Lỗi thanh toán: " + e.getMessage());
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

    private double remainingPayment(AuctionDTO auction) {
        if (auction == null) {
            return 0;
        }
        return Math.max(0, auction.getCurrentPrice() - auction.getDepositAmount());
    }

    private String itemNameOf(AuctionDTO auction) {
        if (auction == null) {
            return "Chưa rõ";
        }
        if (auction.getItem() != null && auction.getItem().getName() != null
                && !auction.getItem().getName().isBlank()) {
            return auction.getItem().getName();
        }
        return auction.getTitle() != null ? auction.getTitle() : "Chưa rõ";
    }

    private String usernameOf(UserDTO user) {
        return user != null && user.getUsername() != null ? user.getUsername() : "Chưa rõ";
    }

    private String shortId(String id) {
        if (id == null || id.isBlank()) {
            return "Chưa rõ";
        }
        return id.length() <= 8 ? id : id.substring(0, 8);
    }

    private enum PaymentMethod {
        QR("Quét mã QR", "Với ứng dụng Ngân hàng hoặc Ví điện tử", "VIETQR"),
        CASH("Thanh toán bằng tiền mặt", "Giao dịch trực tiếp với người bán", "TRỰC TIẾP");

        private final String displayName;
        private final String note;
        private final String badge;

        PaymentMethod(String displayName, String note, String badge) {
            this.displayName = displayName;
            this.note = note;
            this.badge = badge;
        }
    }
}
