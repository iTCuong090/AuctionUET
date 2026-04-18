package com.auctionuet.client.view;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

// Import DTO và Client mạng
import com.auctionuet.client.model.ItemDTO;
import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.network.ItemClient;
import com.auctionuet.client.network.AuctionClient;

public class CreateAuctionController {

    @FXML private ComboBox<ItemDTO> itemComboBox;
    @FXML private Label previewName, previewPrice, previewType, durationLabel, statusLabel;
    @FXML private TextField titleField;
    @FXML private TextArea descArea;
    @FXML private DatePicker startDatePicker, endDatePicker;
    @FXML private ComboBox<String> startHourCombo, startMinuteCombo, endHourCombo, endMinuteCombo;

    @FXML
    public void initialize() {
        // GỌI API LẤY KHO ĐỒ THẬT KHI VỪA MỞ MÀN HÌNH
        loadMyItems();

        itemComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                previewName.setText("Tên: " + newVal.getName());
                previewPrice.setText(String.format("Giá khởi điểm: %,.0f VNĐ", newVal.getStartingPrice()));

                String typeStr = newVal.getType() != null ? newVal.getType().name() : "Chưa rõ";
                previewType.setText("Loại: " + typeStr);

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

        String token = ClientSession.getInstance().getToken();
        if (token == null) {
            showError("Lỗi: Bạn chưa đăng nhập!"); return;
        }

        // Định dạng thời gian chuẩn ISO để gửi Server
        String startTimeStr = start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String endTimeStr = end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String description = descArea.getText();

        statusLabel.setText("⏳ Đang tạo phiên đấu giá...");
        statusLabel.setStyle("-fx-text-fill: #f39c12;"); // Màu cam

        // ==============================================================
        // GỌI MẠNG TẠO PHIÊN ĐẤU GIÁ (LUỒNG PHỤ)
        // ==============================================================
        new Thread(() -> {
            try {
                AuctionClient client = new AuctionClient();
                // Bắn qua mạng theo chuẩn C.2
                client.createAuction(token, selectedItem.getId(), startTimeStr, endTimeStr, title, description);

                Platform.runLater(() -> {
                    statusLabel.setText("✅ Tạo phiên đấu giá thành công!");
                    statusLabel.setStyle("-fx-text-fill: #2ecc71;"); // Màu xanh lá

                    // Tạo xong đá văng khách về màn hình Danh sách Đấu giá
                    SceneManager.getInstance().switchScene("/fxml/DashboardView.fxml");
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Lỗi từ Server: " + e.getMessage()));
            }
        }).start();
    }

    private void showError(String msg) {
        statusLabel.setText("❌ " + msg);
        statusLabel.setStyle("-fx-text-fill: #e94560;"); // Màu đỏ
    }

    // ==============================================================
    // LẤY DANH SÁCH MÓN HÀNG THẬT TỪ SERVER (THAY CHO MOCK DATA)
    // ==============================================================
    private void loadMyItems() {
        String token = ClientSession.getInstance().getToken();
        if (token == null) return;

        itemComboBox.setPromptText("⏳ Đang tải kho đồ...");
        itemComboBox.setDisable(true);

        new Thread(() -> {
            try {
                ItemClient client = new ItemClient();
                List<ItemDTO> myItems = client.getMyItems(token);

                Platform.runLater(() -> {
                    ObservableList<ItemDTO> items = FXCollections.observableArrayList(myItems);
                    itemComboBox.setItems(items);

                    if (items.isEmpty()) {
                        itemComboBox.setPromptText("❌ Kho đồ trống. Hãy đăng SP!");
                    } else {
                        itemComboBox.setPromptText("✅ Chọn sản phẩm...");
                    }
                    itemComboBox.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    itemComboBox.setPromptText("❌ Lỗi tải dữ liệu!");
                    itemComboBox.setDisable(false);
                });
            }
        }).start();
    }
}