package com.auctionuet.client.view;

import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.network.ItemClient;
import com.auctionuet.protocol.dto.request.item.CreateItemRequestDTO;
import com.auctionuet.protocol.dto.response.item.ItemDTO;
import com.auctionuet.protocol.enums.ItemCondition;
import com.auctionuet.protocol.enums.ItemType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;

public class CreateItemController {

    @FXML private ComboBox<String> typeComboBox;
    @FXML private ComboBox<ItemCondition> conditionComboBox;
    @FXML private TextField nameField, priceField, imageUrlField;
    @FXML private TextArea descArea;
    @FXML private Label statusLabel;
    @FXML private Button submitBtn;

    @FXML private VBox electronicsBox, artBox, vehicleBox;

    @FXML private TextField brandField, warrantyField;
    @FXML private TextField artistField, artYearField, mediumField;
    @FXML private TextField makeField, modelField, mileageField, vehicleYearField;

    @FXML
    public void initialize() {
        for (ItemType type : ItemType.values()) {
            typeComboBox.getItems().add(type.name());
        }
        conditionComboBox.getItems().addAll(ItemCondition.values());

        typeComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            switchDynamicForm(newValue);
        });
    }

    private void switchDynamicForm(String type) {
        electronicsBox.setVisible(false); electronicsBox.setManaged(false);
        artBox.setVisible(false); artBox.setManaged(false);
        vehicleBox.setVisible(false); vehicleBox.setManaged(false);

        if (type == null) return;

        switch (type) {
            case "ELECTRONICS" -> { electronicsBox.setVisible(true); electronicsBox.setManaged(true); }
            case "ART" -> { artBox.setVisible(true); artBox.setManaged(true); }
            case "VEHICLE" -> { vehicleBox.setVisible(true); vehicleBox.setManaged(true); }
            default -> {}
        }
    }

    @FXML
    public void handleSubmit() {
        String typeValue = typeComboBox.getValue();
        String name = nameField.getText();
        String priceStr = priceField.getText();

        if (typeValue == null || name.isBlank() || priceStr.isBlank()) {
            showError("Vui lòng nhập tên, giá và chọn loại sản phẩm.");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
            if (price <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError("Giá tiền phải là số lớn hơn 0.");
            return;
        }

        ItemType type;
        try {
            type = ItemType.valueOf(typeValue);
        } catch (IllegalArgumentException e) {
            showError("Loại sản phẩm không hợp lệ.");
            return;
        }

        CreateItemRequestDTO request = new CreateItemRequestDTO();
        request.setType(type);
        request.setName(name);
        request.setDescription(descArea.getText());
        request.setStartingPrice(price);
        request.setCondition(conditionComboBox.getValue());
        request.setImageUrl(imageUrlField.getText());
        request.setExtraFields(buildExtraFields(type));

        String currentToken = ClientSession.getInstance().getToken();
        if (currentToken == null || currentToken.isEmpty()) {
            showError("Bạn chưa đăng nhập.");
            return;
        }

        submitBtn.setDisable(true);
        statusLabel.setText("Đang gửi dữ liệu lên Server...");
        statusLabel.setStyle("-fx-text-fill: #f39c12;");

        new Thread(() -> {
            try {
                ItemClient client = new ItemClient();
                ItemDTO result = client.createItem(currentToken, request);

                Platform.runLater(() -> {
                    statusLabel.setText("Đăng sản phẩm thành công! ID: " + result.getId());
                    statusLabel.setStyle("-fx-text-fill: #2ecc71;");

                    nameField.clear();
                    priceField.clear();
                    descArea.clear();
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError(e.getMessage()));
            } finally {
                Platform.runLater(() -> submitBtn.setDisable(false));
            }
        }).start();
    }

    private Map<String, Object> buildExtraFields(ItemType type) {
        Map<String, Object> extraFields = new HashMap<>();
        switch (type) {
            case ELECTRONICS -> {
                extraFields.put("brand", brandField.getText());
                extraFields.put("warrantyMonths", warrantyField.getText());
            }
            case ART -> {
                extraFields.put("artist", artistField.getText());
                extraFields.put("year", artYearField.getText());
                extraFields.put("medium", mediumField.getText());
            }
            case VEHICLE -> {
                extraFields.put("make", makeField.getText());
                extraFields.put("model", modelField.getText());
                extraFields.put("mileage", mileageField.getText());
                extraFields.put("vehicleYear", vehicleYearField.getText());
            }
        }
        return extraFields;
    }

    private void showError(String msg) {
        statusLabel.setText("Lỗi: " + msg);
        statusLabel.setStyle("-fx-text-fill: #e94560;");
    }
}
