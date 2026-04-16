package com.auctionuet.client.view;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.HashMap;
import java.util.Map;

public class CreateItemController {

    // --- Khai báo móc nối FXML ---
    @FXML private ComboBox<String> typeComboBox;
    @FXML private ComboBox<String> conditionComboBox;
    @FXML private TextField nameField, priceField, imageUrlField;
    @FXML private TextArea descArea;
    @FXML private Label statusLabel;
    @FXML private Button submitBtn;

    // Các VBox chứa form động
    @FXML private VBox electronicsBox, artBox, vehicleBox;

    // Các trường dữ liệu động
    @FXML private TextField brandField, warrantyField;
    @FXML private TextField artistField, artYearField, mediumField;
    @FXML private TextField makeField, modelField, mileageField, vehicleYearField;

    @FXML
    public void initialize() {
        // 1. Nạp đạn cho các ComboBox
        typeComboBox.getItems().addAll("ELECTRONICS", "ART", "VEHICLE");
        conditionComboBox.getItems().addAll("NEW", "LIKE_NEW", "GOOD", "FAIR", "POOR");

        // 2. Lắng nghe sự kiện: Khách chọn Loại hàng nào thì đổi Form đó
        typeComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            switchDynamicForm(newValue);
        });
    }

    // TUYỆT CHIÊU BIẾN HÌNH FORM
    private void switchDynamicForm(String type) {
        // Lột sạch (Ẩn hết đi)
        hideBox(electronicsBox);
        hideBox(artBox);
        hideBox(vehicleBox);

        if (type == null) return;

        // Mặc áo mới (Hiện cái form tương ứng lên)
        switch (type) {
            case "ELECTRONICS": showBox(electronicsBox); break;
            case "ART":         showBox(artBox);         break;
            case "VEHICLE":     showBox(vehicleBox);     break;
        }
    }

    // Hàm phụ trợ Ẩn/Hiện đỉnh cao
    private void hideBox(VBox box) {
        box.setVisible(false);
        box.setManaged(false); // Gỡ nó ra khỏi luồng Layout để khỏi chiếm chỗ
    }

    private void showBox(VBox box) {
        box.setVisible(true);
        box.setManaged(true);  // Nhét nó lại vào luồng Layout
    }

    @FXML
    public void handleSubmit() {
        // 1. Validate (Kiểm tra lởm)
        String type = typeComboBox.getValue();
        String name = nameField.getText();
        String priceStr = priceField.getText();

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

        // 2. Đóng gói hàng hóa vào 1 cái Map (Chuẩn bị gửi cho Server)
        Map<String, Object> itemData = new HashMap<>();
        itemData.put("type", type);
        itemData.put("name", name);
        itemData.put("description", descArea.getText());
        itemData.put("startingPrice", price);
        itemData.put("condition", conditionComboBox.getValue());
        itemData.put("imageUrl", imageUrlField.getText());

        // Quét thêm các trường động tùy theo Loại hàng
        switch (type) {
            case "ELECTRONICS":
                itemData.put("brand", brandField.getText());
                itemData.put("warrantyMonths", warrantyField.getText()); // Tạm để String, sau Server tự parse
                break;
            case "ART":
                itemData.put("artist", artistField.getText());
                itemData.put("year", artYearField.getText());
                itemData.put("medium", mediumField.getText());
                break;
            case "VEHICLE":
                itemData.put("make", makeField.getText());
                itemData.put("model", modelField.getText());
                itemData.put("mileage", mileageField.getText());
                itemData.put("year", vehicleYearField.getText());
                break;
        }

        // 3. Nửa đầu tuần: In ra Console test thử trước
        System.out.println("==== DỮ LIỆU CHUẨN BỊ GỬI LÊN SERVER ====");
        itemData.forEach((key, value) -> System.out.println(key + " : " + value));

        statusLabel.setText("✅ Thu thập dữ liệu thành công! (Xem Console)");
        statusLabel.setStyle("-fx-text-fill: #2ecc71;"); // Đổi màu xanh lá

        // TODO (Nửa sau tuần): Gọi ItemClient.createItem() ở đây
    }

    private void showError(String msg) {
        statusLabel.setText("❌ " + msg);
        statusLabel.setStyle("-fx-text-fill: #e94560;"); // Đổi màu đỏ
    }
}