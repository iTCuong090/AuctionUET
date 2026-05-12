package com.auctionuet.client.view;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;

// Import các class mạng và dữ liệu
import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.model.ItemDTO;
import com.auctionuet.client.network.ItemClient;

public class CreateItemController {

    // --- Khai báo FXML ---
    @FXML private ComboBox<String> typeComboBox;
    @FXML private ComboBox<String> conditionComboBox;
    @FXML private TextField nameField, priceField, imageUrlField;
    @FXML private TextArea descArea;
    @FXML private Label statusLabel;
    @FXML private Button submitBtn;

    // Các Box chứa Form động
    @FXML private VBox electronicsBox, artBox, vehicleBox;

    // Các trường dữ liệu động
    @FXML private TextField brandField, warrantyField;
    @FXML private TextField artistField, artYearField, mediumField;
    @FXML private TextField makeField, modelField, mileageField, vehicleYearField;

    @FXML
    public void initialize() {
        // Nạp dữ liệu cho ComboBox
        typeComboBox.getItems().addAll("ELECTRONICS", "ART", "VEHICLE");
        conditionComboBox.getItems().addAll("NEW", "LIKE_NEW", "GOOD", "FAIR", "POOR");

        // Lắng nghe đổi Form động
        typeComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            switchDynamicForm(newValue);
        });
    }

    private void switchDynamicForm(String type) {
        // Tắt hết
        electronicsBox.setVisible(false); electronicsBox.setManaged(false);
        artBox.setVisible(false); artBox.setManaged(false);
        vehicleBox.setVisible(false); vehicleBox.setManaged(false);

        if (type == null) return;

        // Bật cái tương ứng
        switch (type) {
            case "ELECTRONICS": electronicsBox.setVisible(true); electronicsBox.setManaged(true); break;
            case "ART":         artBox.setVisible(true); artBox.setManaged(true); break;
            case "VEHICLE":     vehicleBox.setVisible(true); vehicleBox.setManaged(true); break;
        }
    }

    // Gắn hàm này vào sự kiện onAction của cái Nút bấm trên Scene Builder
    @FXML
    public void handleSubmit() {
        String type = typeComboBox.getValue();
        String name = nameField.getText();
        String priceStr = priceField.getText();

        // 1. Kiểm tra lởm (Validate)
        if (type == null || name.isBlank() || priceStr.isBlank()) {
            showError("Lỗi: Vui lòng nhập Tên, Giá và Chọn loại sản phẩm!");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
            if (price <= 0) throw new Exception();
        } catch (Exception e) {
            showError("Lỗi: Giá tiền phải là số lớn hơn 0!");
            return;
        }

        // 2. Gói dữ liệu theo chuẩn ItemDTO của Server
        Map<String, Object> itemData = new HashMap<>();
        itemData.put("type", type);
        itemData.put("name", name);
        itemData.put("description", descArea.getText());
        itemData.put("startingPrice", price);
        itemData.put("condition", conditionComboBox.getValue());
        itemData.put("imageUrl", imageUrlField.getText());

        Map<String, Object> extraFields = new HashMap<>();
        switch (type) {
            case "ELECTRONICS":
                extraFields.put("brand", brandField.getText());
                extraFields.put("warrantyMonths", warrantyField.getText());
                break;
            case "ART":
                extraFields.put("artist", artistField.getText());
                extraFields.put("year", artYearField.getText());
                extraFields.put("medium", mediumField.getText());
                break;
            case "VEHICLE":
                extraFields.put("make", makeField.getText());
                extraFields.put("model", modelField.getText());
                extraFields.put("mileage", mileageField.getText());
                extraFields.put("vehicleYear", vehicleYearField.getText());
                break;
        }
        itemData.put("extraFields", extraFields);

        // 3. GỌI NETWORK THEO YÊU CẦU C.3
        String currentToken = ClientSession.getInstance().getToken();

        if (currentToken == null || currentToken.isEmpty()) {
            showError("Lỗi: Bạn chưa đăng nhập!");
            return;
        }

        // Disable nút bấm và hiện dòng trạng thái
        submitBtn.setDisable(true);
        statusLabel.setText("⏳ Đang gửi dữ liệu lên Server...");
        statusLabel.setStyle("-fx-text-fill: #f39c12;"); // Màu cam

        // Ném việc gọi mạng vào luồng phụ
        new Thread(() -> {
            try {
                // CHÍNH LÀ CHỖ NÀY ĐÂY: Controller gọi hàm createItem của ItemClient
                ItemClient client = new ItemClient();
                ItemDTO result = client.createItem(currentToken, itemData);

                // Gửi thành công thì nhờ luồng chính cập nhật giao diện
                Platform.runLater(() -> {
                    statusLabel.setText("✅ Đăng sản phẩm thành công! ID: " + result.getId());
                    statusLabel.setStyle("-fx-text-fill: #2ecc71;"); // Màu xanh lá

                    // Reset form nếu muốn
                    nameField.clear();
                    priceField.clear();
                    descArea.clear();
                });

            } catch (Exception e) {
                // Có lỗi (Mất mạng, Server chửi...) thì báo đỏ
                Platform.runLater(() -> {
                    showError(e.getMessage());
                });
            } finally {
                // Dù thành công hay thất bại cũng phải nhả cái nút bấm ra
                Platform.runLater(() -> {
                    submitBtn.setDisable(false);
                });
            }
        }).start();
    }

    private void showError(String msg) {
        statusLabel.setText("❌ " + msg);
        statusLabel.setStyle("-fx-text-fill: #e94560;"); // Màu đỏ
    }
}
