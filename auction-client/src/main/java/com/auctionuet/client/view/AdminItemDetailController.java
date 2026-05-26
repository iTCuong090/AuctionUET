package com.auctionuet.client.view;

import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.network.AdminClient;
import com.auctionuet.client.network.ImageLoader;
import com.auctionuet.protocol.dto.response.item.ItemDTO;
import com.auctionuet.protocol.enums.ItemApprovalStatus;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.Map;

public class AdminItemDetailController {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Label nameLabel;
    @FXML private Label sellerLabel;
    @FXML private Label typeLabel;
    @FXML private Label conditionLabel;
    @FXML private Label priceLabel;
    @FXML private Label createdLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label specificationsLabel;
    @FXML private Label approvalStatusLabel;
    @FXML private Label statusMessage;
    @FXML private Label imagePlaceholderLabel;
    @FXML private ImageView productImageView;
    @FXML private HBox actionArea;

    private final AdminClient adminClient = new AdminClient();
    private ItemDTO currentItem;

    public void setItem(ItemDTO item) {
        currentItem = item;
        renderItem();
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminItemsView.fxml"));
            Parent root = loader.load();
            replaceContent(root);
        } catch (Exception e) {
            statusMessage.setText("Lỗi quay lại danh sách: " + e.getMessage());
        }
    }

    @FXML
    private void handleApprove() {
        updateStatus(ItemApprovalStatus.APPROVED);
    }

    @FXML
    private void handleReject() {
        updateStatus(ItemApprovalStatus.REJECTED);
    }

    private void renderItem() {
        if (currentItem == null) {
            statusMessage.setText("Không tìm thấy sản phẩm.");
            return;
        }
        nameLabel.setText(currentItem.getName());
        sellerLabel.setText("Seller: " + currentItem.getSeller().getUsername());
        typeLabel.setText("Loại: " + currentItem.getType());
        conditionLabel.setText("Tình trạng: " + currentItem.getCondition());
        priceLabel.setText(CurrencyFormatter.format(currentItem.getStartingPrice()));
        createdLabel.setText("Tạo lúc: " + formatCreatedAt());
        descriptionLabel.setText(valueOrDefault(currentItem.getDescription(), "Không có mô tả."));
        specificationsLabel.setText(formatExtraFields(currentItem.getExtraFields()));
        updateApprovalBadge(currentItem.getApprovalStatus());
        boolean pending = currentItem.getApprovalStatus() == ItemApprovalStatus.PENDING;
        actionArea.setVisible(pending);
        actionArea.setManaged(pending);
        loadImage();
    }

    private void loadImage() {
        ImageLoader.loadImage(productImageView, imagePlaceholderLabel, currentItem.getImageUrl());
    }

    private void updateStatus(ItemApprovalStatus status) {
        if (currentItem == null || currentItem.getApprovalStatus() != ItemApprovalStatus.PENDING) {
            return;
        }
        String token = ClientSession.getInstance().getToken();
        actionArea.setDisable(true);
        statusMessage.setText("Đang cập nhật sản phẩm...");
        new Thread(() -> {
            try {
                ItemDTO updated = adminClient.updateItemApprovalStatus(token, currentItem.getId(), status);
                Platform.runLater(() -> {
                    currentItem = updated;
                    actionArea.setDisable(false);
                    renderItem();
                    statusMessage.setText(status == ItemApprovalStatus.APPROVED
                            ? "Đã duyệt sản phẩm."
                            : "Đã từ chối sản phẩm.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    actionArea.setDisable(false);
                    statusMessage.setText("Lỗi: " + e.getMessage());
                });
            }
        }, "admin-item-approval-detail-update").start();
    }

    private void updateApprovalBadge(ItemApprovalStatus status) {
        approvalStatusLabel.getStyleClass().removeAll(
                "status-badge-open", "status-badge-running", "status-badge-canceled");
        switch (status) {
            case PENDING -> {
                approvalStatusLabel.setText("Chờ duyệt");
                approvalStatusLabel.getStyleClass().add("status-badge-open");
            }
            case APPROVED -> {
                approvalStatusLabel.setText("Đã duyệt");
                approvalStatusLabel.getStyleClass().add("status-badge-running");
            }
            case REJECTED -> {
                approvalStatusLabel.setText("Bị từ chối");
                approvalStatusLabel.getStyleClass().add("status-badge-canceled");
            }
        }
    }

    private String formatCreatedAt() {
        return currentItem.getCreatedAt() != null
                ? currentItem.getCreatedAt().format(DATE_FORMAT)
                : "Chưa rõ";
    }

    private String formatExtraFields(Map<String, Object> extraFields) {
        if (extraFields == null || extraFields.isEmpty()) {
            return "Không có thông số bổ sung.";
        }
        StringBuilder values = new StringBuilder();
        for (Map.Entry<String, Object> field : extraFields.entrySet()) {
            if (!values.isEmpty()) {
                values.append("\n");
            }
            values.append(formatFieldName(field.getKey()))
                    .append(": ")
                    .append(field.getValue());
        }
        return values.toString();
    }

    private String formatFieldName(String key) {
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < key.length(); index++) {
            char value = key.charAt(index);
            if (index > 0 && Character.isUpperCase(value)) {
                text.append(' ');
            }
            text.append(index == 0 ? Character.toUpperCase(value) : value);
        }
        return text.toString();
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value != null && !value.isBlank() ? value : defaultValue;
    }

    private void replaceContent(Parent content) {
        Parent root = nameLabel.getScene().getRoot();
        if (root instanceof HBox dashboard) {
            VBox mainCard = (VBox) dashboard.getChildren().get(1);
            StackPane contentArea = (StackPane) mainCard.getChildren().get(1);
            contentArea.getChildren().setAll(content);
        }
    }
}
