package com.auctionuet.client.util;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextField;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Lớp tiện ích hỗ trợ tự động định dạng dấu phẩy hàng nghìn khi nhập tiền tệ.
 */
public class CurrencyInputHelper {

    private CurrencyInputHelper() {}

    /**
     * Gắn tính năng tự động tách 3 chữ số bằng dấu phẩy.
     *
     * @param textField Ô nhập liệu cần gắn tính năng
     */
    public static void setupCurrencyInput(TextField textField) {
        if (textField == null) return;

        // Lắng nghe thay đổi văn bản để định dạng
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
                    return;
                }

                try {
                    // Giới hạn tối đa 15 chữ số tránh tràn số long
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

                } catch (NumberFormatException e) {
                    updating = true;
                    textField.setText(oldValue);
                    updating = false;
                }
            }
        });
    }
}
