package com.auctionuet.client.util;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Popup;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Lớp tiện ích hỗ trợ tự động định dạng dấu phẩy hàng nghìn khi nhập tiền tệ
 * và hiển thị popup đọc số tiền bằng chữ Tiếng Việt siêu đẹp ngay dưới ô nhập liệu.
 */
public class CurrencyInputHelper {

    private static Popup wordPopup;
    private static Label wordLabel;

    private CurrencyInputHelper() {}

    /**
     * Gắn tính năng tự động tách 3 chữ số bằng dấu phẩy và hiển thị popup đọc số tiền bằng tiếng Việt.
     *
     * @param textField Ô nhập liệu cần gắn tính năng
     */
    public static void setupCurrencyInput(TextField textField) {
        if (textField == null) return;

        // Lắng nghe thay đổi văn bản để định dạng và cập nhật popup
        textField.textProperty().addListener(new ChangeListener<String>() {
            private boolean updating = false;

            @Override
            public void changed(ObservableValue<? extends String> obs, String oldValue, String newValue) {
                if (updating) return;

                // 1. Chỉ giữ lại các chữ số
                String clean = newValue.replaceAll("[^\\d]", "");

                if (clean.isEmpty()) {
                    updating = true;
                    textField.setText("");
                    updating = false;
                    hidePopup();
                    return;
                }

                try {
                    // Giới hạn tối đa 15 chữ số tránh tràn số long (đủ cho hàng trăm nghìn tỷ)
                    if (clean.length() > 15) {
                        clean = clean.substring(0, 15);
                    }

                    long value = Long.parseLong(clean);

                    // 2. Định dạng số bằng dấu phẩy hàng nghìn kiểu US (1,234,567)
                    DecimalFormat df = new DecimalFormat("#,##0");
                    df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.US));
                    String formatted = df.format(value);

                    // 3. Tính toán và bảo toàn vị trí con trỏ (caret) tránh bị nhảy về cuối
                    int caretPos = textField.getCaretPosition();
                    int digitsBeforeCaret = 0;
                    String oldText = textField.getText();
                    
                    // Đếm số chữ số nằm trước con trỏ trong chuỗi hiện tại
                    for (int i = 0; i < Math.min(caretPos, oldText.length()); i++) {
                        if (Character.isDigit(oldText.charAt(i))) {
                            digitsBeforeCaret++;
                        }
                    }

                    updating = true;
                    textField.setText(formatted);

                    // Xác định vị trí con trỏ mới trong chuỗi đã định dạng
                    int newCaretPos = 0;
                    int digitCount = 0;
                    for (int i = 0; i < formatted.length(); i++) {
                        if (Character.isDigit(formatted.charAt(i))) {
                            digitCount++;
                        }
                        if (digitCount == digitsBeforeCaret) {
                            newCaretPos = i + 1;
                            break;
                        }
                    }
                    
                    // Nếu không khớp (ví dụ con trỏ ở cuối), mặc định đưa về cuối chuỗi mới
                    if (digitCount < digitsBeforeCaret) {
                        newCaretPos = formatted.length();
                    }

                    textField.selectRange(newCaretPos, newCaretPos);
                    updating = false;

                    // 4. Hiển thị popup đọc số tiền bằng chữ Tiếng Việt
                    String words = VietnameseNumberReader.toVietnameseWords(value);
                    showPopup(textField, words);

                } catch (NumberFormatException e) {
                    updating = true;
                    textField.setText(oldValue);
                    updating = false;
                }
            }
        });

        // Ẩn/Hiện popup tương ứng khi ô nhập liệu được Focus hoặc Mất Focus
        textField.focusedProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue) {
                hidePopup();
            } else {
                String clean = textField.getText().replaceAll("[^\\d]", "");
                if (!clean.isEmpty()) {
                    try {
                        long value = Long.parseLong(clean);
                        showPopup(textField, VietnameseNumberReader.toVietnameseWords(value));
                    } catch (NumberFormatException ignored) {}
                }
            }
        });
    }

    /**
     * Hiển thị popup chứa chữ đọc số tiền dưới ô nhập liệu đang tương tác.
     */
    private static void showPopup(TextField textField, String text) {
        if (wordPopup == null) {
            wordPopup = new Popup();
            wordLabel = new Label();
            
            // Thiết kế giao diện Glassmorphism / Dark Mode cao cấp thích hợp với mọi tông nền
            wordLabel.setStyle(
                "-fx-background-color: rgba(13, 17, 23, 0.95);" + // Màu nền tối sâu thẳm sang trọng
                "-fx-text-fill: #38bdf8;" +                        // Màu chữ xanh neon dịu mát
                "-fx-padding: 8 14 8 14;" +                        // Spacing thoáng đãng
                "-fx-background-radius: 8;" +                      // Bo góc hiện đại
                "-fx-border-color: rgba(56, 189, 248, 0.4);" +     // Viền mờ đồng điệu tinh tế
                "-fx-border-radius: 8;" +
                "-fx-border-width: 1;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 4);" // Đổ bóng tạo chiều sâu
            );
            
            wordPopup.getContent().add(wordLabel);
            wordPopup.setAutoHide(true); // Tự ẩn khi người dùng click ra ngoài
        }

        wordLabel.setText("Bằng chữ: " + text);

        // Tính toán vị trí hiển thị chuẩn xác ngay bên dưới ô nhập liệu
        Bounds bounds = textField.localToScreen(textField.getBoundsInLocal());
        if (bounds != null && textField.getScene() != null && textField.getScene().getWindow() != null) {
            // Hiển thị cách cạnh dưới ô nhập liệu 5px
            wordPopup.show(textField, bounds.getMinX(), bounds.getMaxY() + 5);
        }
    }

    /**
     * Ẩn popup đọc chữ.
     */
    private static void hidePopup() {
        if (wordPopup != null && wordPopup.isShowing()) {
            wordPopup.hide();
        }
    }
}
