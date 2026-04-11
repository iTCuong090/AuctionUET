package com.auctionuet.server.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class PasswordUtils {
    // Chuyển từ mảng byte sang HexString
    private static String convertToHex(byte[] arr) {
        StringBuilder hexString = new StringBuilder(); // Là đối tượng string có thể thay đổi được

        for (byte b : arr) {
            hexString.append(String.format("%02x", b));
            /*
            %: Dấu hiệu báo cho Java biết: "Sắp có một yêu cầu định dạng ở đây nè!".
            0: Nếu số đó quá ngắn, hãy thêm số 0 ở phía trước cho đủ chỗ.
            2: Quy định độ dài luôn luôn là 2 ký tự. (Đây là lý do tại sao 16 byte luôn tạo ra đúng 32 ký tự).
            x: Viết tắt của Hexadecimal (hệ thập lục phân). Nó sẽ biến số thành các ký tự từ 0-9 và a-f.
            */
        }
        return hexString.toString();
    }

    // Tạo salt để trộn lẫn với password tăng tính bảo mật
    public static String generateSalt() {
        // Tạo một salt ngẫu nhiên
        SecureRandom sr = new SecureRandom();

        // Tạo một mảng 16 byte cho salt
        byte[] salt = new byte[16];

        // Đổ dữ liệu ngẫu nhiên vào mảng byte trên
        sr.nextBytes(salt);

        // Chuyển mảng byte sang Hex String 32 kí tự
        return convertToHex(salt);
    }

    public static String hash(String password, String salt) {
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA-256");

            // phân rã string thành 1 mảng các bytes
            hash.update(password.getBytes(StandardCharsets.UTF_8));
            hash.update(salt.getBytes(StandardCharsets.UTF_8));
            /*
            Method getBytes() biến String thành mảng các bytes và lưu vào 1 mảng
            StandardCharsets.UTF_8 biến mọi loại kí tự về dạng bảng mã UTF_8 để dễ dàng chuyển đổi sang bytes.
            */

            // Biến thành mảng bytes mới bởi thuật toán SHA-256 được truyền vào
            byte[] hashPassword = hash.digest();

            return convertToHex(hashPassword);

        } catch (NoSuchAlgorithmException err) {return "Error";}
    }

    public static boolean verify(String password, String salt, String hashedPassword) {
        String computedHash = hash(password,salt);
        return computedHash.equals(hashedPassword);
    }
}
