package com.auctionuet.client.view;

import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.network.AuctionClient;
import com.auctionuet.client.network.ItemClient;
import com.auctionuet.protocol.dto.response.auction.AuctionDTO;
import com.auctionuet.protocol.dto.response.item.ItemDTO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CreateAuctionController {

    @FXML private ComboBox<ItemDTO> itemComboBox;
    @FXML private Label durationLabel, statusLabel;
    @FXML private TextField titleField, antiSnipingWindowField, antiSnipingExtensionField;
    @FXML private TextArea descArea;
    @FXML private DatePicker startDatePicker, endDatePicker;
    @FXML private ComboBox<String> startHourCombo, startMinuteCombo, endHourCombo, endMinuteCombo;
    @FXML private VBox previewContentBox;

    private final ItemClient itemClient = new ItemClient();
    private final AuctionClient auctionClient = new AuctionClient();
    private String initialItemId;
    private String lastAutoTitle;

    @FXML
    public void initialize() {
        renderEmptyItemPreview();
        loadMyItems();

        itemComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                renderItemPreview(newVal);
                syncTitleWithSelectedItem(newVal);
            } else {
                renderEmptyItemPreview();
            }
        });

        ObservableList<String> hours = FXCollections.observableArrayList();
        for (int i = 0; i < 24; i++) hours.add(String.format("%02d", i));

        ObservableList<String> minutes = FXCollections.observableArrayList();
        for (int i = 0; i < 60; i += 5) minutes.add(String.format("%02d", i));

        startHourCombo.setItems(hours); endHourCombo.setItems(hours);
        startMinuteCombo.setItems(minutes); endMinuteCombo.setItems(minutes);

        startDatePicker.valueProperty().addListener((o, old, now) -> calculateDuration());
        endDatePicker.valueProperty().addListener((o, old, now) -> calculateDuration());
        startHourCombo.valueProperty().addListener((o, old, now) -> calculateDuration());
        startMinuteCombo.valueProperty().addListener((o, old, now) -> calculateDuration());
        endHourCombo.valueProperty().addListener((o, old, now) -> calculateDuration());
        endMinuteCombo.valueProperty().addListener((o, old, now) -> calculateDuration());
    }

    private void calculateDuration() {
        LocalDateTime start = getSelectedDateTime(startDatePicker, startHourCombo, startMinuteCombo);
        LocalDateTime end = getSelectedDateTime(endDatePicker, endHourCombo, endMinuteCombo);

        if (start == null || end == null) {
            durationLabel.setText("Thời lượng: Vui lòng chọn đủ ngày giờ.");
            durationLabel.setStyle("-fx-text-fill: #f39c12;");
            return;
        }

        if (!end.isAfter(start)) {
            durationLabel.setText("Lỗi: Thời gian kết thúc phải sau thời gian bắt đầu.");
            durationLabel.setStyle("-fx-text-fill: #e94560;");
            return;
        }

        Duration duration = Duration.between(start, end);
        durationLabel.setText(String.format(
                "Thời lượng dự kiến: %d ngày, %d giờ, %d phút",
                duration.toDays(),
                duration.toHoursPart(),
                duration.toMinutesPart()));
        durationLabel.setStyle("-fx-text-fill: #2ecc71;");
    }

    private LocalDateTime getSelectedDateTime(DatePicker date, ComboBox<String> h, ComboBox<String> m) {
        if (date.getValue() == null || h.getValue() == null || m.getValue() == null) return null;
        return LocalDateTime.of(date.getValue(),
                LocalTime.of(Integer.parseInt(h.getValue()), Integer.parseInt(m.getValue())));
    }

    @FXML
    public void handleCreateAuction() {
        ItemDTO selectedItem = itemComboBox.getValue();
        String title = titleField.getText();
        LocalDateTime start = getSelectedDateTime(startDatePicker, startHourCombo, startMinuteCombo);
        LocalDateTime end = getSelectedDateTime(endDatePicker, endHourCombo, endMinuteCombo);

        if (selectedItem == null || title.isBlank() || start == null || end == null) {
            showError("Vui lòng điền đầy đủ thông tin bắt buộc.");
            return;
        }
        if (!end.isAfter(start)) {
            showError("Thời gian kết thúc không hợp lệ.");
            return;
        }

        int windowSec = 60;
        int extensionSec = 120;
        try {
            if (!antiSnipingWindowField.getText().trim().isEmpty()) {
                windowSec = Integer.parseInt(antiSnipingWindowField.getText().trim());
            }
            if (!antiSnipingExtensionField.getText().trim().isEmpty()) {
                extensionSec = Integer.parseInt(antiSnipingExtensionField.getText().trim());
            }
        } catch (NumberFormatException ex) {
            showError("Anti-Sniping phải là số nguyên.");
            return;
        }

        String token = ClientSession.getInstance().getToken();
        if (token == null || token.isEmpty()) {
            showError("Vui lòng đăng nhập lại.");
            return;
        }

        statusLabel.setText("Đang xử lý...");
        statusLabel.setStyle("-fx-text-fill: #f39c12;");

        final int finalWindowSec = windowSec;
        final int finalExtSec = extensionSec;

        new Thread(() -> {
            try {
                auctionClient.createAuction(
                        token,
                        selectedItem.getId(),
                        start,
                        end,
                        title,
                        descArea.getText(),
                        finalWindowSec,
                        finalExtSec);

                Platform.runLater(() -> {
                    statusLabel.setText("Tạo phiên đấu giá thành công.");
                    statusLabel.setStyle("-fx-text-fill: #2ecc71;");
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Lỗi từ server: " + e.getMessage()));
            }
        }).start();
    }

    private void showError(String msg) {
        statusLabel.setText("Lỗi: " + msg);
        statusLabel.setStyle("-fx-text-fill: #e94560;");
    }

    private void renderEmptyItemPreview() {
        previewContentBox.getChildren().clear();
        addPreviewFullWidth("Trạng thái", "Chưa chọn sản phẩm.");
    }

    private void renderItemPreview(ItemDTO item) {
        previewContentBox.getChildren().clear();
        List<VBox> fields = new ArrayList<>();
        fields.add(createPreviewField("Tên", item.getName(), true));
        fields.add(createPreviewMoneyField("Giá khởi điểm", item.getStartingPrice()));
        fields.add(createPreviewField("Loại", valueOrEmpty(item.getType()), false));
        fields.add(createPreviewField("Tình trạng", valueOrEmpty(item.getCondition()), false));
        fields.addAll(createTypeSpecificPreviewFields(item));
        addPreviewRows(fields);

        String description = item.getDescription();
        if (description != null && !description.isBlank()) {
            addPreviewFullWidth("Chi tiết", description.trim());
        }
    }

    private List<VBox> createTypeSpecificPreviewFields(ItemDTO item) {
        List<VBox> result = new ArrayList<>();
        Map<String, Object> fields = item.getExtraFields();
        if (fields == null || item.getType() == null) {
            return result;
        }

        switch (item.getType()) {
            case ELECTRONICS -> {
                result.add(createPreviewField("Brand", fieldValue(fields, "brand"), false));
                result.add(createPreviewField("Warranty", withUnit(fieldValue(fields, "warrantyMonths"), "tháng"), false));
            }
            case ART -> {
                result.add(createPreviewField("Artist", fieldValue(fields, "artist"), false));
                result.add(createPreviewField("Year", fieldValue(fields, "year"), false));
                result.add(createPreviewField("Medium", fieldValue(fields, "medium"), false));
            }
            case VEHICLE -> {
                result.add(createPreviewField("Make", fieldValue(fields, "make"), false));
                result.add(createPreviewField("Model", fieldValue(fields, "model"), false));
                result.add(createPreviewField("Mileage", withUnit(fieldValue(fields, "mileage"), "km"), false));
                result.add(createPreviewField("Vehicle year", fieldValue(fields, "vehicleYear"), false));
            }
        }
        return result;
    }

    private void addPreviewRows(List<VBox> fields) {
        for (int i = 0; i < fields.size(); i += 2) {
            HBox row = new HBox(24);
            row.setMaxWidth(Double.MAX_VALUE);
            row.getChildren().add(fields.get(i));
            HBox.setHgrow(fields.get(i), Priority.ALWAYS);

            if (i + 1 < fields.size()) {
                row.getChildren().add(fields.get(i + 1));
                HBox.setHgrow(fields.get(i + 1), Priority.ALWAYS);
            }
            previewContentBox.getChildren().add(row);
        }
    }

    private VBox createPreviewField(String name, String value, boolean highlight) {
        VBox fieldBox = new VBox();
        fieldBox.setMaxWidth(Double.MAX_VALUE);
        fieldBox.setPrefWidth(1);

        Label fieldLabel = new Label(name + ": " + value);
        fieldLabel.getStyleClass().add(highlight ? "text-primary" : "text-detail");
        fieldLabel.setWrapText(true);
        fieldLabel.setMaxWidth(Double.MAX_VALUE);
        fieldLabel.setStyle(highlight
                ? "-fx-font-size: 14px; -fx-font-weight: bold;"
                : "-fx-font-size: 13px;");

        fieldBox.getChildren().add(fieldLabel);
        return fieldBox;
    }

    private VBox createPreviewMoneyField(String name, double amount) {
        VBox fieldBox = createPreviewField(name, CurrencyFormatter.format(amount), true);
        if (!fieldBox.getChildren().isEmpty() && fieldBox.getChildren().get(0) instanceof Label label) {
            CurrencyFormatter.installTooltip(label, name + ": " + CurrencyFormatter.formatFull(amount));
        }
        return fieldBox;
    }

    private void addPreviewFullWidth(String name, String value) {
        VBox fieldBox = createPreviewField(name, value, false);
        previewContentBox.getChildren().add(fieldBox);
    }

    private String fieldValue(Map<String, Object> fields, String key) {
        return formatFieldValue(fields.get(key));
    }

    private String withUnit(String value, String unit) {
        return value == null || value.isBlank() ? "" : value + " " + unit;
    }

    private String formatFieldValue(Object value) {
        if (value instanceof Number number) {
            double doubleValue = number.doubleValue();
            if (doubleValue == Math.rint(doubleValue)) {
                return String.format("%.0f", doubleValue);
            }
        }
        return value != null ? value.toString() : "";
    }

    private String valueOrEmpty(Object value) {
        return value != null ? value.toString() : "";
    }

    private void syncTitleWithSelectedItem(ItemDTO item) {
        String currentTitle = titleField.getText() != null ? titleField.getText().trim() : "";
        String nextAutoTitle = "Đấu giá: " + item.getName();
        if (currentTitle.isEmpty() || currentTitle.equals(lastAutoTitle)) {
            titleField.setText(nextAutoTitle);
            lastAutoTitle = nextAutoTitle;
        }
    }

    public void setInitialItemId(String itemId) {
        this.initialItemId = itemId;
        selectInitialItemIfLoaded();
    }

    private void loadMyItems() {
        String token = ClientSession.getInstance().getToken();
        if (token == null || token.isEmpty()) {
            System.err.println("[CreateAuction] Chưa đăng nhập, không thể tải item.");
            return;
        }

        new Thread(() -> {
            try {
                List<ItemDTO> items = itemClient.getMyItems(token);
                List<ItemDTO> auctionableItems = filterAuctionableItems(token, items);
                Platform.runLater(() -> {
                    itemComboBox.setItems(FXCollections.observableArrayList(auctionableItems));
                    boolean selectedInitialItem = selectInitialItemIfLoaded();
                    if (items.isEmpty()) {
                        statusLabel.setText("Bạn chưa có sản phẩm nào. Hãy tạo sản phẩm trước.");
                        statusLabel.setStyle("-fx-text-fill: #f39c12;");
                    } else if (auctionableItems.isEmpty()) {
                        statusLabel.setText("Tất cả vật phẩm đang có phiên đấu giá hoạt động.");
                        statusLabel.setStyle("-fx-text-fill: #f39c12;");
                    } else if (initialItemId != null && !selectedInitialItem) {
                        statusLabel.setText("Vật phẩm này đang có phiên đấu giá hoạt động.");
                        statusLabel.setStyle("-fx-text-fill: #f39c12;");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Không thể tải danh sách sản phẩm: " + e.getMessage()));
                System.err.println("[CreateAuction] Lỗi tải item: " + e.getMessage());
            }
        }).start();
    }

    private List<ItemDTO> filterAuctionableItems(String token, List<ItemDTO> items) {
        List<ItemDTO> result = new ArrayList<>();
        if (items == null) {
            return result;
        }

        for (ItemDTO item : items) {
            if (!hasBlockingAuction(token, item)) {
                result.add(item);
            }
        }
        return result;
    }

    private boolean hasBlockingAuction(String token, ItemDTO item) {
        try {
            List<AuctionDTO> auctions = itemClient.getItemAuctionHistory(token, item.getId());
            return auctions != null && auctions.stream()
                    .anyMatch(auction -> AuctionViewFilter.isBlockingStatus(auction.getStatus()));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean selectInitialItemIfLoaded() {
        if (initialItemId == null || itemComboBox.getItems() == null) {
            return false;
        }
        for (ItemDTO item : itemComboBox.getItems()) {
            if (initialItemId.equals(item.getId())) {
                itemComboBox.getSelectionModel().select(item);
                return true;
            }
        }
        return false;
    }
}
