package com.auctionuet.client.view;

import com.auctionuet.client.model.ClientSession;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller cho cửa sổ popup cấu hình Nhận diện giọng nói Gemini.
 */
public class VoiceConfigController {

    @FXML private ComboBox<String> geminiModelComboBox;
    @FXML private Label geminiModelDescLabel;
    @FXML private TextField geminiApiKeyField;
    @FXML private Label geminiConfigStatusLabel;
    @FXML private Button btnSaveGeminiConfig;

    @FXML
    public void initialize() {
        setupGeminiConfig();
    }

    /**
     * Khởi tạo cấu hình ban đầu bằng cách nạp dữ liệu từ ClientSession.
     */
    private void setupGeminiConfig() {
        if (geminiModelComboBox != null) {
            geminiModelComboBox.getItems().clear();
            geminiModelComboBox.getItems().addAll(
                "Gemini 3.1 Pro",
                "Gemini 3.5 Flash",
                "Gemini 3 Flash",
                "Gemini 3.1 Flash-Lite",
                "Gemini 2.5 Flash",
                "Gemini 2.5 Flash-Lite"
            );

            // Tải giá trị hiện tại từ ClientSession
            String currentModel = ClientSession.getInstance().getGeminiModel();
            String displayModel = mapModelIdToDisplay(currentModel);
            geminiModelComboBox.setValue(displayModel);
            updateModelDescription(displayModel);

            String currentApiKey = ClientSession.getInstance().getGeminiApiKey();
            if (geminiApiKeyField != null) {
                geminiApiKeyField.setText(currentApiKey);
            }
        }
    }

    /**
     * Chuyển đổi mã Model ID sang tên hiển thị trên giao diện.
     */
    private String mapModelIdToDisplay(String modelId) {
        if (modelId == null) return "Gemini 2.5 Flash-Lite";
        return switch (modelId) {
            case "gemini-3.1-pro" -> "Gemini 3.1 Pro";
            case "gemini-3.5-flash" -> "Gemini 3.5 Flash";
            case "gemini-3-flash" -> "Gemini 3 Flash";
            case "gemini-3.1-flash-lite" -> "Gemini 3.1 Flash-Lite";
            case "gemini-2.5-flash" -> "Gemini 2.5 Flash";
            default -> "Gemini 2.5 Flash-Lite";
        };
    }

    /**
     * Chuyển đổi tên hiển thị giao diện sang mã Model ID chuẩn API.
     */
    private String mapDisplayToModelId(String display) {
        if (display == null) return "gemini-2.5-flash-lite";
        return switch (display) {
            case "Gemini 3.1 Pro" -> "gemini-3.1-pro";
            case "Gemini 3.5 Flash" -> "gemini-3.5-flash";
            case "Gemini 3 Flash" -> "gemini-3-flash";
            case "Gemini 3.1 Flash-Lite" -> "gemini-3.1-flash-lite";
            case "Gemini 2.5 Flash" -> "gemini-2.5-flash";
            default -> "gemini-2.5-flash-lite";
        };
    }

    /**
     * Xử lý khi người dùng đổi mô hình trên ComboBox.
     */
    @FXML
    private void handleModelChanged() {
        if (geminiModelComboBox != null) {
            String selected = geminiModelComboBox.getValue();
            updateModelDescription(selected);
        }
    }

    /**
     * Cập nhật nhãn mô tả chi tiết tính năng của từng mô hình Gemini.
     */
    private void updateModelDescription(String displayModel) {
        if (geminiModelDescLabel == null) return;
        if (displayModel == null) {
            geminiModelDescLabel.setText("");
            return;
        }

        String desc = switch (displayModel) {
            case "Gemini 3.1 Pro" -> "Trí tuệ tiên tiến, kỹ năng giải quyết vấn đề phức tạp và khả năng tác nhân mạnh mẽ cùng khả năng lập trình theo cảm hứng. [Xem trước]";
            case "Gemini 3.5 Flash" -> "Mô hình thông minh nhất để duy trì hiệu suất tiên tiến cho các tác vụ tác nhân và lập trình. [Ổn định]";
            case "Gemini 3 Flash" -> "Hiệu suất ở cấp độ tiên tiến, ngang bằng với các mô hình lớn hơn nhưng chỉ tốn một phần chi phí. [Xem trước]";
            case "Gemini 3.1 Flash-Lite" -> "Hiệu suất ở cấp độ tiên tiến, ngang bằng với các mô hình lớn hơn nhưng chỉ tốn một phần chi phí. [Ổn định]";
            case "Gemini 2.5 Flash" -> "Cân bằng xuất sắc giữa tốc độ xử lý nhanh và độ chính xác cao. [Ổn định]";
            case "Gemini 2.5 Flash-Lite" -> "Mô hình cực kỳ nhanh, nhẹ và tiết kiệm chi phí, giảm thiểu tối đa giới hạn cuộc gọi. [Ổn định]";
            default -> "";
        };
        geminiModelDescLabel.setText(desc);
    }

    /**
     * Lưu thông tin cấu hình đã thiết lập vào hệ thống (tự động ghi xuống file lâu dài).
     */
    @FXML
    private void handleSaveGeminiConfig() {
        if (geminiApiKeyField == null || geminiModelComboBox == null) return;
        
        String apiKey = geminiApiKeyField.getText().trim();
        String displayModel = geminiModelComboBox.getValue();
        String modelId = mapDisplayToModelId(displayModel);

        ClientSession.getInstance().setGeminiApiKey(apiKey);
        ClientSession.getInstance().setGeminiModel(modelId);

        if (geminiConfigStatusLabel != null) {
            if (apiKey.isEmpty()) {
                geminiConfigStatusLabel.setText("⚠️ Đã cập nhật mô hình: " + displayModel + ". Chưa cấu hình API Key.");
                geminiConfigStatusLabel.setStyle("-fx-text-fill: #f59e0b;");
            } else {
                geminiConfigStatusLabel.setText("✅ Đã lưu cấu hình trợ lý giọng nói Gemini thành công!");
                geminiConfigStatusLabel.setStyle("-fx-text-fill: #10b981;");
            }
        }
    }

    /**
     * Đóng cửa sổ popup cấu hình.
     */
    @FXML
    private void handleClose() {
        if (geminiModelComboBox != null && geminiModelComboBox.getScene() != null) {
            Stage stage = (Stage) geminiModelComboBox.getScene().getWindow();
            stage.close();
        }
    }
}
