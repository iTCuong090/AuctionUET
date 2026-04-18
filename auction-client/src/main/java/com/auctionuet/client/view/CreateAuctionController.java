package com.auctionuet.client.view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import com.auctionuet.client.model.ItemDTO;
import com.auctionuet.client.network.protocol.ItemType;

public class CreateAuctionController {

    // ĐÃ SỬA: Đổi sang nhận ItemDTO thật
    @FXML private ComboBox<ItemDTO> itemComboBox;
    @FXML private Label previewName, previewPrice, previewType, durationLabel, statusLabel;
    @FXML private TextField titleField;
    @FXML private TextArea descArea;
    @FXML private DatePicker startDatePicker, endDatePicker;
    @FXML private ComboBox<String> startHourCombo, startMinuteCombo, endHourCombo, endMinuteCombo;

    @FXML
    public void initialize() {
        loadMyItems();

        itemComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                previewName.setText("Tên: " + newVal.getName());
                previewPrice.setText(String.format("Giá khởi điểm: %,.0f VNĐ", newVal.getStartingPrice()));
                previewType.setText("Loại: " + newVal.getType());

                if (titleField.getText().isEmpty()) {
                    titleField.setText("Đấu giá: " + newVal.getName());
                }
            }
        });

        ObservableList<String> hours = FXCollections.observableArrayList();
        for (int i = 0; i < 24; i++) hours.add(String.format("%02d", i));

        ObservableList<String> minutes = FXCollections.observableArrayList();
        for (int i = 0; i < 60; i+=5) minutes.add(String.format("%02d", i));

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
            durationLabel.setText("⏱️ Thời lượng: Vui lòng chọn đầy đủ ngày giờ!");
            durationLabel.setStyle("-fx-text-fill: #f39c12;");
            return;
        }

        if (end.isBefore(start) || end.isEqual(start)) {
            durationLabel.setText("❌ Lỗi: Thời gian kết thúc phải lớn hơn thời gian bắt đầu!");
            durationLabel.setStyle("-fx-text-fill: #e94560;");
            return;
        }

        Duration duration = Duration.between(start, end);
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();

        durationLabel.setText(String.format("⏱️ Thời lượng dự kiến: %d ngày, %d giờ, %d phút", days, hours, minutes));
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
            showError("Vui lòng điền đầy đủ thông tin bắt buộc!"); return;
        }
        if (end.isBefore(start) || end.isEqual(start)) {
            showError("Thời gian kết thúc không hợp lệ!"); return;
        }

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("itemId", selectedItem.getId());
        requestData.put("title", title);
        requestData.put("description", descArea.getText());
        requestData.put("startTime", start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        requestData.put("endTime", end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        System.out.println("=== CHUẨN BỊ GỬI REQUEST TẠO AUCTION LÊN SERVER ===");
        requestData.forEach((k, v) -> System.out.println(k + ": " + v));

        statusLabel.setText("✅ Tạo phiên đấu giá thành công! Đang chuyển trang...");
        statusLabel.setStyle("-fx-text-fill: #2ecc71;");
    }

    private void showError(String msg) {
        statusLabel.setText("❌ " + msg);
        statusLabel.setStyle("-fx-text-fill: #e94560;");
    }

    private void loadMyItems() {
        ObservableList<ItemDTO> items = FXCollections.observableArrayList();

        // Tạo thử vài cái DTO ảo để test UI
        ItemDTO i1 = new ItemDTO(); i1.setId("I1"); i1.setName("iPhone 15");
        i1.setStartingPrice(25000000);

        items.add(i1);
        itemComboBox.setItems(items);
    }
}