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

    @FXML
    public void initialize() {
        loadMyItems();

        itemComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                previewName.setText("Ten: " + newVal.getName());
                previewPrice.setText(String.format("Gia khoi diem: %,.0f VND", newVal.getStartingPrice()));
                previewType.setText("Loai: " + newVal.getType());

                if (titleField.getText().isEmpty()) {
                    titleField.setText("Dau gia: " + newVal.getName());
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
            durationLabel.setText("Thoi luong: Vui long chon du ngay gio.");
            durationLabel.setStyle("-fx-text-fill: #f39c12;");
            return;
        }

        if (!end.isAfter(start)) {
            durationLabel.setText("Loi: Thoi gian ket thuc phai sau thoi gian bat dau.");
            durationLabel.setStyle("-fx-text-fill: #e94560;");
            return;
        }

        Duration duration = Duration.between(start, end);
        durationLabel.setText(String.format(
                "Thoi luong du kien: %d ngay, %d gio, %d phut",
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
            showError("Vui long dien day du thong tin bat buoc.");
            return;
        }
        if (!end.isAfter(start)) {
            showError("Thoi gian ket thuc khong hop le.");
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
            showError("Anti-Sniping phai la so nguyen.");
            return;
        }

        String token = ClientSession.getInstance().getToken();
        if (token == null || token.isEmpty()) {
            showError("Vui long dang nhap lai.");
            return;
        }

        statusLabel.setText("Dang xu ly...");
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
                    statusLabel.setText("Tao phien dau gia thanh cong.");
                    statusLabel.setStyle("-fx-text-fill: #2ecc71;");
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Loi tu server: " + e.getMessage()));
            }
        }).start();
    }

    private void showError(String msg) {
        statusLabel.setText("Loi: " + msg);
        statusLabel.setStyle("-fx-text-fill: #e94560;");
    }

    private void loadMyItems() {
        String token = ClientSession.getInstance().getToken();
        if (token == null || token.isEmpty()) {
            System.err.println("[CreateAuction] Chua dang nhap, khong the tai item.");
            return;
        }

        new Thread(() -> {
            try {
                List<ItemDTO> items = itemClient.getMyItems(token);
                Platform.runLater(() -> {
                    itemComboBox.setItems(FXCollections.observableArrayList(items));
                    if (items.isEmpty()) {
                        statusLabel.setText("Ban chua co san pham nao. Hay tao san pham truoc.");
                        statusLabel.setStyle("-fx-text-fill: #f39c12;");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Khong the tai danh sach san pham: " + e.getMessage()));
                System.err.println("[CreateAuction] Loi tai item: " + e.getMessage());
            }
        }).start();
    }
}
