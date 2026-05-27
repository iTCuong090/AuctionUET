package com.auctionuet.client.view;

import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.network.AdminClient;
import com.auctionuet.protocol.dto.response.item.ItemDTO;
import com.auctionuet.protocol.enums.ItemApprovalStatus;
import javafx.application.Platform;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;
import javafx.geometry.Bounds;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class AdminItemsController {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int CARD_COLUMNS = 3;
    private static final double CARD_GAP = 18.0;
    private static final double CARD_HORIZONTAL_PADDING = 12.0;

    @FXML private StackPane approvalSwitch;
    @FXML private Region approvalThumb;
    @FXML private FlowPane itemGrid;
    @FXML private ScrollPane itemScrollPane;
    @FXML private Label statusLabel;

    private final AdminClient adminClient = new AdminClient();
    private boolean approvalEnabled;
    private boolean settingUpdating;

    @FXML
    public void initialize() {
        approvalSwitch.setFocusTraversable(true);
        approvalSwitch.setAccessibleText("Bật phê duyệt sản phẩm");
        approvalSwitch.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                handleApprovalToggle();
            }
        });
        itemScrollPane.viewportBoundsProperty().addListener((observable, oldBounds, newBounds) ->
                syncCardWidths(newBounds));
        Platform.runLater(() -> syncCardWidths(itemScrollPane.getViewportBounds()));
        refreshData();
    }

    @FXML
    private void handleRefresh() {
        refreshData();
    }

    @FXML
    private void handleApprovalToggle() {
        if (settingUpdating) {
            return;
        }
        boolean enabled = !approvalEnabled;
        if (!enabled && !confirmDisableApproval()) {
            return;
        }
        // Cập nhật giao diện phản hồi tức thì
        renderSwitch(enabled);
        updateSetting(enabled);
    }

    private boolean confirmDisableApproval() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Tắt phê duyệt sản phẩm");
        confirmation.setHeaderText("Tất cả sản phẩm đang chờ sẽ được duyệt.");
        confirmation.setContentText("Bạn có chắc muốn tắt chế độ phê duyệt?");
        Optional<ButtonType> answer = confirmation.showAndWait();
        return answer.isPresent() && answer.get() == ButtonType.OK;
    }

    private void refreshData() {
        String token = ClientSession.getInstance().getToken();
        statusLabel.setText("Đang tải...");
        new Thread(() -> {
            try {
                boolean enabled = adminClient.getItemApprovalSettings(token).isEnabled();
                List<ItemDTO> items = adminClient.getItemsForApproval(token);
                Platform.runLater(() -> {
                    renderSwitch(enabled);
                    renderItems(items);
                    if (!items.isEmpty()) {
                        statusLabel.setText("Có " + items.size() + " sản phẩm.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Lỗi: " + e.getMessage()));
            }
        }, "admin-item-approval-load").start();
    }

    private void renderSwitch(boolean enabled) {
        approvalEnabled = enabled;
        approvalSwitch.getStyleClass().remove("enabled");
        if (enabled) {
            approvalSwitch.getStyleClass().add("enabled");
        }
        
        // Tạo hiệu ứng trượt mượt mà (micro-animation) cho toggle switch
        TranslateTransition transition = new TranslateTransition(Duration.millis(150), approvalThumb);
        transition.setToX(enabled ? 24 : 0);
        transition.play();
    }

    private void renderItems(List<ItemDTO> items) {
        itemGrid.getChildren().clear();
        if (items == null || items.isEmpty()) {
            statusLabel.setText("Chưa có sản phẩm để kiểm duyệt.");
            return;
        }
        for (ItemDTO item : items) {
            itemGrid.getChildren().add(createItemCard(item));
        }
        syncCardWidths(itemScrollPane.getViewportBounds());
    }

    private VBox createItemCard(ItemDTO item) {
        VBox card = new VBox(10);
        card.getStyleClass().addAll("auction-card", "approval-item-card");

        Label title = new Label(item.getName());
        title.getStyleClass().add("approval-card-title");
        title.setWrapText(true);

        Label seller = createDetailLabel("Seller: " + item.getSeller().getUsername());
        Label type = createDetailLabel("Loại: " + item.getType());
        Label price = new Label(CurrencyFormatter.format(item.getStartingPrice()));
        price.getStyleClass().add("approval-card-price");
        Label created = createDetailLabel("Tạo lúc: " + formatCreatedAt(item));

        Label badge = new Label(formatStatus(item.getApprovalStatus()));
        applyStatusStyle(badge, item.getApprovalStatus());

        HBox actions = new HBox(8);
        actions.getStyleClass().add("approval-card-actions");
        Button detailButton = createActionButton("Chi tiết");
        detailButton.setOnAction(event -> openDetail(item));
        actions.getChildren().add(detailButton);
        if (item.getApprovalStatus() == ItemApprovalStatus.PENDING) {
            Button approveButton = createActionButton("Duyệt");
            approveButton.setOnAction(event -> updateStatus(item, ItemApprovalStatus.APPROVED));
            Button rejectButton = createActionButton("Từ chối");
            rejectButton.getStyleClass().add("danger-action-button");
            rejectButton.setOnAction(event -> updateStatus(item, ItemApprovalStatus.REJECTED));
            actions.getChildren().addAll(approveButton, rejectButton);
        }

        card.getChildren().addAll(title, price, badge, seller, type, created, actions);
        return card;
    }

    private void syncCardWidths(Bounds viewportBounds) {
        if (viewportBounds == null || viewportBounds.getWidth() <= 0) {
            return;
        }
        double availableWidth = viewportBounds.getWidth() - CARD_HORIZONTAL_PADDING;
        double cardWidth = Math.floor(
                (availableWidth - (CARD_GAP * (CARD_COLUMNS - 1)) - 2) / CARD_COLUMNS);
        itemGrid.setMinWidth(viewportBounds.getWidth());
        itemGrid.setPrefWidth(viewportBounds.getWidth());
        itemGrid.setPrefWrapLength(viewportBounds.getWidth());
        itemGrid.getChildren().forEach(node -> {
            if (node instanceof Region card) {
                card.setMinWidth(cardWidth);
                card.setPrefWidth(cardWidth);
                card.setMaxWidth(cardWidth);
            }
        });
    }

    private Label createDetailLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("text-secondary");
        label.setWrapText(true);
        return label;
    }

    private Button createActionButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("approval-action-button");
        return button;
    }

    private void openDetail(ItemDTO item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminItemDetailView.fxml"));
            Parent root = loader.load();
            AdminItemDetailController controller = loader.getController();
            controller.setItem(item);
            replaceContent(root);
        } catch (Exception e) {
            statusLabel.setText("Lỗi mở chi tiết sản phẩm: " + e.getMessage());
        }
    }

    private void replaceContent(Parent content) {
        Parent root = itemGrid.getScene().getRoot();
        if (root instanceof HBox dashboard) {
            VBox mainCard = (VBox) dashboard.getChildren().get(1);
            StackPane contentArea = (StackPane) mainCard.getChildren().get(1);
            contentArea.getChildren().setAll(content);
        }
    }

    private void updateSetting(boolean enabled) {
        String token = ClientSession.getInstance().getToken();
        settingUpdating = true;
        approvalSwitch.setDisable(true);
        statusLabel.setText("Đang cập nhật chế độ phê duyệt...");
        new Thread(() -> {
            try {
                adminClient.updateItemApprovalSettings(token, enabled);
                Platform.runLater(() -> {
                    settingUpdating = false;
                    approvalSwitch.setDisable(false);
                    refreshData();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    settingUpdating = false;
                    approvalSwitch.setDisable(false);
                    renderSwitch(!enabled);
                    statusLabel.setText("Lỗi: " + e.getMessage());
                });
            }
        }, "admin-item-approval-setting").start();
    }

    private void updateStatus(ItemDTO item, ItemApprovalStatus status) {
        String token = ClientSession.getInstance().getToken();
        statusLabel.setText("Đang cập nhật sản phẩm...");
        new Thread(() -> {
            try {
                adminClient.updateItemApprovalStatus(token, item.getId(), status);
                Platform.runLater(this::refreshData);
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Lỗi: " + e.getMessage()));
            }
        }, "admin-item-approval-update").start();
    }

    private String formatCreatedAt(ItemDTO item) {
        return item.getCreatedAt() != null ? item.getCreatedAt().format(DATE_FORMAT) : "Chưa rõ";
    }

    private String formatStatus(ItemApprovalStatus status) {
        return switch (status) {
            case PENDING -> "Chờ duyệt";
            case APPROVED -> "Đã duyệt";
            case REJECTED -> "Bị từ chối";
        };
    }

    private void applyStatusStyle(Label badge, ItemApprovalStatus status) {
        switch (status) {
            case PENDING -> badge.getStyleClass().add("status-badge-open");
            case APPROVED -> badge.getStyleClass().add("status-badge-running");
            case REJECTED -> badge.getStyleClass().add("status-badge-canceled");
        }
    }
}
