package com.auctionuet.client.view;

import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.network.ItemClient;
import com.auctionuet.protocol.dto.response.auction.AuctionDTO;
import com.auctionuet.protocol.dto.response.item.ItemDTO;
import com.auctionuet.protocol.enums.AuctionStatus;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MyItemsController {
    private static final String ALL_STATUS = "Tất cả";
    private static final String NO_AUCTION_STATUS = "NOT_LISTED";

    @FXML private FlowPane itemGrid;
    @FXML private Label statusLabel;
    @FXML private Button btnToggleCreate;
    @FXML private Button btnRefresh;
    @FXML private VBox createItemHost;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private TextField searchField;

    private final ItemClient itemClient = new ItemClient();
    private List<ItemCardData> allItems = new ArrayList<>();

    @FXML
    public void initialize() {
        boolean seller = ClientSession.getInstance().isSeller();
        btnToggleCreate.setVisible(seller);
        btnToggleCreate.setManaged(seller);
        btnToggleCreate.setOnAction(e -> toggleCreateItemForm());
        btnRefresh.setOnAction(e -> loadMyItems());
        setupFilters();
        loadMyItems();
    }

    private void setupFilters() {
        filterComboBox.getItems().add(ALL_STATUS);
        filterComboBox.getItems().add(NO_AUCTION_STATUS);
        for (AuctionStatus status : AuctionStatus.values()) {
            filterComboBox.getItems().add(status.name());
        }
        filterComboBox.setValue(ALL_STATUS);
        filterComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        searchField.setOnAction(e -> applyFilters());
    }

    private void toggleCreateItemForm() {
        if (createItemHost.getChildren().isEmpty()) {
            try {
                Parent form = FXMLLoader.load(getClass().getResource("/fxml/CreateItemView.fxml"));
                createItemHost.getChildren().setAll(form);
            } catch (Exception e) {
                statusLabel.setText("Lỗi mở form tạo vật phẩm: " + e.getMessage());
                return;
            }
        }

        boolean visible = !createItemHost.isVisible();
        createItemHost.setVisible(visible);
        createItemHost.setManaged(visible);
        btnToggleCreate.setText(visible ? "Ẩn" : "+  New");
    }

    private void loadMyItems() {
        String token = ClientSession.getInstance().getToken();
        if (token == null) {
            statusLabel.setText("Bạn chưa đăng nhập.");
            return;
        }

        new Thread(() -> {
            try {
                List<ItemDTO> items = itemClient.getMyItems(token);
                List<ItemCardData> loadedItems = loadItemCardData(token, items);
                Platform.runLater(() -> {
                    allItems = loadedItems;
                    applyFilters();
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Lỗi tải vật phẩm: " + e.getMessage()));
            }
        }).start();
    }

    private List<ItemCardData> loadItemCardData(String token, List<ItemDTO> items) {
        List<ItemCardData> result = new ArrayList<>();
        if (items == null) {
            return result;
        }

        for (ItemDTO item : items) {
            result.add(new ItemCardData(item, loadAuctionStatus(token, item)));
        }
        return result;
    }

    private String loadAuctionStatus(String token, ItemDTO item) {
        try {
            List<AuctionDTO> auctions = itemClient.getItemAuctionHistory(token, item.getId());
            if (auctions == null || auctions.isEmpty()) {
                return NO_AUCTION_STATUS;
            }
            return auctions.stream()
                    .max(Comparator.comparing(auction ->
                            auction.getEndTime() != null ? auction.getEndTime() : auction.getStartTime()))
                    .map(auction -> auction.getStatus() != null ? auction.getStatus().name() : NO_AUCTION_STATUS)
                    .orElse(NO_AUCTION_STATUS);
        } catch (Exception e) {
            return NO_AUCTION_STATUS;
        }
    }

    @FXML
    private void handleSearch() {
        applyFilters();
    }

    private void applyFilters() {
        String selectedStatus = filterComboBox.getValue();
        String keyword = searchField.getText() != null
                ? searchField.getText().trim().toLowerCase(Locale.ROOT)
                : "";
        List<ItemCardData> filteredItems = allItems.stream()
                .filter(item -> matchesStatus(item, selectedStatus))
                .filter(item -> matchesKeyword(item, keyword))
                .toList();
        renderItems(filteredItems);
    }

    private boolean matchesStatus(ItemCardData item, String selectedStatus) {
        if (selectedStatus == null || selectedStatus.equals(ALL_STATUS)) {
            return true;
        }
        return selectedStatus.equals(item.auctionStatus());
    }

    private boolean matchesKeyword(ItemCardData item, String keyword) {
        if (keyword.isEmpty()) {
            return true;
        }

        ItemDTO dto = item.item();
        return text(dto.getName()).contains(keyword)
                || text(String.valueOf(dto.getType())).contains(keyword)
                || text(String.valueOf(dto.getCondition())).contains(keyword)
                || text(item.auctionStatus()).contains(keyword)
                || text(extraFieldsText(dto.getExtraFields())).contains(keyword);
    }

    private String text(String value) {
        return value != null ? value.toLowerCase(Locale.ROOT) : "";
    }

    private void renderItems(List<ItemCardData> items) {
        itemGrid.getChildren().clear();
        if (items == null || items.isEmpty()) {
            statusLabel.setText(allItems.isEmpty() ? "Bạn chưa có vật phẩm nào." : "Không có vật phẩm phù hợp.");
            return;
        }

        statusLabel.setText("");
        for (ItemCardData item : items) {
            itemGrid.getChildren().add(createItemCard(item));
        }
    }

    private VBox createItemCard(ItemCardData data) {
        ItemDTO item = data.item();
        VBox card = new VBox(11);
        card.getStyleClass().add("auction-card");
        card.setPrefWidth(300);

        Label title = new Label(item.getName());
        title.getStyleClass().add("text-primary");
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold;");
        title.setWrapText(true);

        Label price = new Label(String.format("%,.0f VND", item.getStartingPrice()));
        price.getStyleClass().add("text-accent");
        price.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Label type = new Label("Loại: " + item.getType());
        type.getStyleClass().add("text-secondary");
        type.setWrapText(true);
        type.setStyle("-fx-font-size: 13px;");

        Label condition = new Label("Tình trạng: " + item.getCondition());
        condition.getStyleClass().add("text-secondary");
        condition.setWrapText(true);
        condition.setStyle("-fx-font-size: 13px;");

        Label auctionStatusBadge = new Label(formatAuctionStatusBadge(data.auctionStatus()));
        applyAuctionStatusStyle(auctionStatusBadge, data.auctionStatus());

        VBox details = new VBox(6);
        details.getChildren().addAll(type, condition);
        details.getChildren().addAll(createExtraFieldLabels(item.getExtraFields()));

        VBox actions = new VBox(8);
        Button historyButton = new Button("Lịch sử");
        historyButton.getStyleClass().add("button");
        historyButton.setMaxWidth(Double.MAX_VALUE);
        historyButton.setStyle("-fx-font-size: 13px;");
        historyButton.setOnAction(e -> openItemHistory(historyButton, item));
        actions.getChildren().add(historyButton);

        if (ClientSession.getInstance().isSeller()) {
            Button createAuctionButton = new Button("Tạo phiên");
            createAuctionButton.getStyleClass().add("button");
            createAuctionButton.setMaxWidth(Double.MAX_VALUE);
            createAuctionButton.setStyle("-fx-font-size: 13px;");
            createAuctionButton.setOnAction(e -> openCreateAuction(item));

            Button deleteButton = new Button("Gỡ");
            deleteButton.getStyleClass().add("button");
            deleteButton.setMaxWidth(Double.MAX_VALUE);
            deleteButton.setStyle("-fx-background-color: #e94560; -fx-font-size: 13px;");
            deleteButton.setOnAction(e -> archiveItem(item));

            actions.getChildren().addAll(createAuctionButton, deleteButton);
        }

        card.getChildren().addAll(title, price, auctionStatusBadge, details, actions);
        return card;
    }

    private String formatAuctionStatusBadge(String auctionStatus) {
        return auctionStatus;
    }

    private void applyAuctionStatusStyle(Label badge, String auctionStatus) {
        if ("RUNNING".equals(auctionStatus)) {
            badge.getStyleClass().add("status-badge-running");
        } else if ("FINISHED".equals(auctionStatus) || "PAID".equals(auctionStatus)) {
            badge.getStyleClass().add("status-badge-finished");
        } else {
            badge.getStyleClass().add("status-badge-open");
        }
    }

    private List<Label> createExtraFieldLabels(Map<String, Object> extraFields) {
        List<Label> labels = new ArrayList<>();
        if (extraFields == null || extraFields.isEmpty()) {
            return labels;
        }

        for (Map.Entry<String, Object> entry : extraFields.entrySet()) {
            Label label = new Label(formatExtraFieldName(entry.getKey()) + ": " + formatExtraFieldValue(entry.getValue()));
            label.getStyleClass().add("text-detail");
            label.setWrapText(true);
            label.setStyle("-fx-font-size: 12px;");
            labels.add(label);
        }
        return labels;
    }

    private String formatExtraFieldName(String key) {
        if ("brand".equals(key)) {
            return "Brand";
        }
        if ("warrantyMonths".equals(key)) {
            return "Warranty";
        }
        if ("vehicleYear".equals(key)) {
            return "Vehicle year";
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < key.length(); i++) {
            char ch = key.charAt(i);
            if (i > 0 && Character.isUpperCase(ch)) {
                result.append(' ');
            }
            result.append(i == 0 ? Character.toUpperCase(ch) : ch);
        }
        return result.toString();
    }

    private String formatExtraFieldValue(Object value) {
        if (value instanceof Number number) {
            double doubleValue = number.doubleValue();
            if (doubleValue == Math.rint(doubleValue)) {
                return String.format("%.0f", doubleValue);
            }
        }
        return value != null ? value.toString() : "";
    }

    private String extraFieldsText(Map<String, Object> extraFields) {
        if (extraFields == null || extraFields.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Object> entry : extraFields.entrySet()) {
            builder.append(entry.getKey()).append(' ').append(entry.getValue()).append(' ');
        }
        return builder.toString();
    }

    private void archiveItem(ItemDTO item) {
        String token = ClientSession.getInstance().getToken();
        new Thread(() -> {
            try {
                itemClient.deleteItem(token, item.getId());
                Platform.runLater(() -> {
                    statusLabel.setText("Đã gỡ vật phẩm: " + item.getName());
                    loadMyItems();
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Lỗi gỡ vật phẩm: " + e.getMessage()));
            }
        }).start();
    }

    private void openItemHistory(Button source, ItemDTO item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ItemHistoryView.fxml"));
            Parent root = loader.load();
            ItemHistoryController controller = loader.getController();
            controller.setItem(item);

            Parent currentRoot = source.getScene().getRoot();
            if (currentRoot instanceof HBox) {
                VBox mainCard = (VBox) ((HBox) currentRoot).getChildren().get(1);
                javafx.scene.layout.StackPane contentArea =
                        (javafx.scene.layout.StackPane) mainCard.getChildren().get(1);
                contentArea.getChildren().setAll(root);
            }
        } catch (Exception e) {
            statusLabel.setText("Lỗi mở lịch sử vật phẩm: " + e.getMessage());
        }
    }

    private void openCreateAuction(ItemDTO item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CreateAuctionView.fxml"));
            Parent root = loader.load();
            CreateAuctionController controller = loader.getController();
            controller.setInitialItemId(item.getId());

            Parent currentRoot = itemGrid.getScene().getRoot();
            if (currentRoot instanceof HBox) {
                VBox mainCard = (VBox) ((HBox) currentRoot).getChildren().get(1);
                javafx.scene.layout.StackPane contentArea =
                        (javafx.scene.layout.StackPane) mainCard.getChildren().get(1);
                contentArea.getChildren().setAll(root);
            }
        } catch (Exception e) {
            statusLabel.setText("Lỗi mở màn tạo phiên: " + e.getMessage());
        }
    }

    private record ItemCardData(ItemDTO item, String auctionStatus) {
    }
}
