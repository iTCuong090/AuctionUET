package com.auctionuet.client.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp tiện ích chuyển đổi số tiền thành chữ viết tiếng Việt.
 * Hỗ trợ các mệnh giá lớn lên tới hàng triệu tỷ đồng.
 */
public class VietnameseNumberReader {

    private static final String[] DIGITS = {
        "không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"
    };

    private static final String[] UNITS = {
        "", "nghìn", "triệu", "tỷ"
    };

    private VietnameseNumberReader() {}

    /**
     * Chuyển đổi một số nguyên dương thành tên tiếng Việt của số tiền đó.
     * Ví dụ: 1500000 -> "Một triệu năm trăm nghìn đồng"
     *
     * @param number Số tiền cần đọc
     * @return Tên tiếng Việt dạng chữ
     */
    public static String toVietnameseWords(long number) {
        if (number == 0) {
            return "Không đồng";
        }

        if (number < 0) {
            return "Số tiền không hợp lệ";
        }

        long temp = number;
        List<Integer> groups = new ArrayList<>();

        // Tách số thành từng nhóm 3 chữ số từ phải qua trái (đơn vị, nghìn, triệu, tỷ...)
        while (temp > 0) {
            groups.add((int) (temp % 1000));
            temp /= 1000;
        }

        StringBuilder sb = new StringBuilder();

        // Duyệt và đọc từng nhóm từ trái qua phải (từ lớn tới bé)
        for (int i = groups.size() - 1; i >= 0; i--) {
            int groupValue = groups.get(i);

            // Nếu nhóm bằng 0 nhưng ở vị trí hàng tỷ bội (i % 3 == 0 và i > 0) thì vẫn cần thêm chữ "tỷ"
            if (groupValue == 0) {
                if (i > 0 && i % 3 == 0) {
                    sb.append(" tỷ");
                }
                continue;
            }

            // Chỉ đọc "không trăm" nếu không phải nhóm lớn nhất ngoài cùng bên trái
            boolean showZeroHundred = (i < groups.size() - 1);

            String groupText = readThreeDigits(groupValue, showZeroHundred);
            if (!groupText.isEmpty()) {
                sb.append(" ").append(groupText);
                
                // Thêm đơn vị (nghìn, triệu, tỷ...) dựa vào chỉ số nhóm
                int unitIndex = i % 3;
                if (unitIndex > 0) {
                    sb.append(" ").append(UNITS[unitIndex]);
                }
                
                // Xử lý các cấp độ tỷ cao hơn (ví dụ: nghìn tỷ, triệu tỷ...)
                int billionLevel = i / 3;
                for (int j = 0; j < billionLevel; j++) {
                    sb.append(" tỷ");
                }
            }
        }

        String result = sb.toString().trim();
        if (!result.isEmpty()) {
            // Viết hoa chữ cái đầu tiên và thêm đơn vị "đồng" ở cuối
            result = Character.toUpperCase(result.charAt(0)) + result.substring(1) + " đồng";
        }

        // Loại bỏ khoảng trắng thừa
        return result.replaceAll("\\s+", " ");
    }

    /**
     * Đọc một nhóm tối đa 3 chữ số (ví dụ: 105 -> "một trăm linh năm").
     */
    private static String readThreeDigits(int number, boolean showZeroHundred) {
        int hundred = number / 100;
        int ten = (number % 100) / 10;
        int unit = number % 10;

        StringBuilder sb = new StringBuilder();

        // Đọc hàng trăm
        if (hundred > 0 || showZeroHundred) {
            sb.append(DIGITS[hundred]).append(" trăm ");
        }

        // Đọc hàng chục
        if (ten > 0) {
            if (ten == 1) {
                sb.append("mười ");
            } else {
                sb.append(DIGITS[ten]).append(" mươi ");
            }
        } else if (hundred > 0 || showZeroHundred) {
            if (unit > 0) {
                sb.append("linh "); // Hoặc lẻ tùy vùng miền, ở đây dùng linh cho thống nhất
            }
        }

        // Đọc hàng đơn vị
        if (unit > 0) {
            if (unit == 1 && ten > 1) {
                sb.append("mốt");
            } else if (unit == 5 && ten > 0) {
                sb.append("lăm");
            } else if (unit == 4 && ten > 1) {
                sb.append("tư"); // Trong văn phong tiền tệ dùng "tư" tự nhiên hơn "bốn"
            } else {
                sb.append(DIGITS[unit]);
            }
        }

        return sb.toString().trim();
    }
}
