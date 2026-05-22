package com.auctionuet.client.view;

import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.network.AuctionClient;
import com.auctionuet.client.network.ItemClient;
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class CreateAuctionController {

    @FXML private ComboBox<ItemDTO> itemComboBox;
    @FXML private Label previewName, previewPrice, previewType, durationLabel, statusLabel;
    @FXML private TextField titleField, antiSnipingWindowField, antiSnipingExtensionField;
    @FXML private TextArea descArea;
    @FXML private DatePicker startDatePicker, endDatePicker;
    @FXML private ComboBox<String> startHourCombo, startMinuteCombo, endHourCombo, endMinuteCombo;

    private final ItemClient itemClient = new ItemClient();
    private final AuctionClient auctionClient = new AuctionClient();
    private String initialItemId;

    @FXML
    public void initialize() {
        loadMyItems();

        itemComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                previewName.setText("Tên: " + newVal.getName());
                previewPrice.setText("Giá khởi điểm: " + CurrencyFormatter.format(newVal.getStartingPrice()));
                previewType.setText("Loại: " + newVal.getType());

                if (titleField.getText().isEmpty()) {
                    titleField.setText("Đấu giá: " + newVal.getName());
                }
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
                Platform.runLater(() -> {
                    itemComboBox.setItems(FXCollections.observableArrayList(items));
                    selectInitialItemIfLoaded();
                    if (items.isEmpty()) {
                        statusLabel.setText("Bạn chưa có sản phẩm nào. Hãy tạo sản phẩm trước.");
                        statusLabel.setStyle("-fx-text-fill: #f39c12;");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Không thể tải danh sách sản phẩm: " + e.getMessage()));
                System.err.println("[CreateAuction] Lỗi tải item: " + e.getMessage());
            }
        }).start();
    }

    private void selectInitialItemIfLoaded() {
        if (initialItemId == null || itemComboBox.getItems() == null) {
            return;
        }
        for (ItemDTO item : itemComboBox.getItems()) {
            if (initialItemId.equals(item.getId())) {
                itemComboBox.getSelectionModel().select(item);
                return;
            }
        }
    }
}
