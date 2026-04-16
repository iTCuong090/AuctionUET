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

public class CreateAuctionController {

    // Liên kết FXML
    @FXML private ComboBox<MockItem> itemComboBox;
    @FXML private Label previewName, previewPrice, previewType, durationLabel, statusLabel;
    @FXML private TextField titleField;
    @FXML private TextArea descArea;

    @FXML private DatePicker startDatePicker, endDatePicker;
    @FXML private ComboBox<String> startHourCombo, startMinuteCombo, endHourCombo, endMinuteCombo;

    @FXML
    public void initialize() {
        // 1. Nạp data ảo cho danh sách Sản phẩm của tui (My Items)
        loadMyItems();

        // 2. Lắng nghe khi chọn Sản phẩm -> Hiện Preview
        itemComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                previewName.setText("Tên: " + newVal.name);
                previewPrice.setText(String.format("Giá khởi điểm: %,d VNĐ", newVal.price));
                previewType.setText("Loại: " + newVal.type);

                // Tự động gán luôn Tiêu đề phiên cho tiện
                if (titleField.getText().isEmpty()) {
                    titleField.setText("Đấu giá: " + newVal.name);
                }
            }
        });

        // 3. Đổ dữ liệu Giờ (00-23) và Phút (00-59) vào các ComboBox
        ObservableList<String> hours = FXCollections.observableArrayList();
        for (int i = 0; i < 24; i++) hours.add(String.format("%02d", i)); // Tạo format 00, 01.. 23

        ObservableList<String> minutes = FXCollections.observableArrayList();
        for (int i = 0; i < 60; i+=5) minutes.add(String.format("%02d", i)); // Bước nhảy 5 phút cho gọn

        startHourCombo.setItems(hours); endHourCombo.setItems(hours);
        startMinuteCombo.setItems(minutes); endMinuteCombo.setItems(minutes);

        // 4. Lắng nghe mọi thay đổi của Lịch/Giờ để tự động tính thời lượng
        startDatePicker.valueProperty().addListener((o, old, now) -> calculateDuration());
        endDatePicker.valueProperty().addListener((o, old, now) -> calculateDuration());
        startHourCombo.valueProperty().addListener((o, old, now) -> calculateDuration());
        startMinuteCombo.valueProperty().addListener((o, old, now) -> calculateDuration());
        endHourCombo.valueProperty().addListener((o, old, now) -> calculateDuration());
        endMinuteCombo.valueProperty().addListener((o, old, now) -> calculateDuration());
    }

    // --- MA THUẬT TÍNH THỜI GIAN ---
    private void calculateDuration() {
        LocalDateTime start = getSelectedDateTime(startDatePicker, startHourCombo, startMinuteCombo);
        LocalDateTime end = getSelectedDateTime(endDatePicker, endHourCombo, endMinuteCombo);

        if (start == null || end == null) {
            durationLabel.setText("⏱️ Thời lượng: Vui lòng chọn đầy đủ ngày giờ!");
            durationLabel.setStyle("-fx-text-fill: #f39c12;"); // Màu cam
            return;
        }

        if (end.isBefore(start) || end.isEqual(start)) {
            durationLabel.setText("❌ Lỗi: Thời gian kết thúc phải lớn hơn thời gian bắt đầu!");
            durationLabel.setStyle("-fx-text-fill: #e94560;"); // Màu đỏ
            return;
        }

        // Tính khoảng cách giữa 2 mốc thời gian
        Duration duration = Duration.between(start, end);
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();

        String timeStr = String.format("⏱️ Thời lượng dự kiến: %d ngày, %d giờ, %d phút", days, hours, minutes);
        durationLabel.setText(timeStr);
        durationLabel.setStyle("-fx-text-fill: #2ecc71;"); // Màu xanh lá
    }

    // Hàm phụ trợ: Rút gọn việc lấy Ngày + Giờ + Phút gộp lại
    private LocalDateTime getSelectedDateTime(DatePicker date, ComboBox<String> h, ComboBox<String> m) {
        if (date.getValue() == null || h.getValue() == null || m.getValue() == null) return null;
        return LocalDateTime.of(date.getValue(),
                LocalTime.of(Integer.parseInt(h.getValue()), Integer.parseInt(m.getValue())));
    }

    // --- KHI BẤM NÚT TẠO PHIÊN ---
    @FXML
    public void handleCreateAuction() {
        MockItem selectedItem = itemComboBox.getValue();
        String title = titleField.getText();
        LocalDateTime start = getSelectedDateTime(startDatePicker, startHourCombo, startMinuteCombo);
        LocalDateTime end = getSelectedDateTime(endDatePicker, endHourCombo, endMinuteCombo);

        // Validate Validation Validation!!!
        if (selectedItem == null) {
            showError("Vui lòng chọn sản phẩm muốn đem đi đấu giá!"); return;
        }
        if (title.isBlank()) {
            showError("Vui lòng nhập tiêu đề phiên đấu giá!"); return;
        }
        if (start == null || end == null) {
            showError("Vui lòng chọn đầy đủ thời gian bắt đầu và kết thúc!"); return;
        }
        if (end.isBefore(start) || end.isEqual(start)) {
            showError("Thời gian kết thúc không hợp lệ!"); return;
        }
        if (start.isBefore(LocalDateTime.now())) {
            showError("Thời gian bắt đầu không được nằm trong quá khứ!"); return;
        }

        // Gói hàng để chuẩn bị gửi qua mạng (Tuần sau gọi AuctionClient)
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("itemId", selectedItem.id);
        requestData.put("title", title);
        requestData.put("description", descArea.getText());
        requestData.put("startTime", start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        requestData.put("endTime", end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        System.out.println("=== CHUẨN BỊ GỬI REQUEST TẠO AUCTION LÊN SERVER ===");
        requestData.forEach((k, v) -> System.out.println(k + ": " + v));

        statusLabel.setText("✅ Tạo phiên đấu giá thành công! Đang chuyển trang...");
        statusLabel.setStyle("-fx-text-fill: #2ecc71;");

        // TODO: SceneManager.switchScene("/fxml/AuctionListView.fxml");
    }

    private void showError(String msg) {
        statusLabel.setText("❌ " + msg);
        statusLabel.setStyle("-fx-text-fill: #e94560;");
    }

    // --- DATA ẢO ---
    private void loadMyItems() {
        ObservableList<MockItem> items = FXCollections.observableArrayList();
        items.add(new MockItem("ITM001", "iPhone 15 Pro Max", 25000000, "ELECTRONICS"));
        items.add(new MockItem("ITM002", "Honda SH 150i ABS", 95000000, "VEHICLE"));
        items.add(new MockItem("ITM003", "Tranh phong cảnh Đồng Quê", 1200000, "ART"));
        itemComboBox.setItems(items);
    }

    // Class chứa dữ liệu nội bộ (override toString để hiển thị tên trên ComboBox)
    private static class MockItem {
        String id, name, type;
        long price;
        public MockItem(String id, String name, long price, String type) {
            this.id = id; this.name = name; this.price = price; this.type = type;
        }
        @Override
        public String toString() { return name; } // Cực kỳ quan trọng: Định nghĩa cách ComboBox hiển thị dòng chữ
    }
}