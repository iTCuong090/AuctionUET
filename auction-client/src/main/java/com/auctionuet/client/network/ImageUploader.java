package com.auctionuet.client.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;

/**
 * Lớp trợ giúp tải hình ảnh sản phẩm lên Cloudflare Worker R2 Bucket công khai.
 * Sử dụng kết nối HTTP Multipart Form-Data tiêu chuẩn của Java.
 */
public class ImageUploader {
    private static final String UPLOAD_URL = "https://auctionuet-image-upload.dragonsvip090.workers.dev/upload";
    private static final String BOUNDARY = "===AuctionUETImageUploadBoundary===";
    private static final String LINE_FEED = "\r\n";

    /**
     * Tải một file hình ảnh cục bộ lên server và nhận về đường dẫn URL công khai.
     *
     * @param file Tệp tin hình ảnh cần tải lên
     * @return Chuỗi liên kết URL trực tiếp của hình ảnh
     * @throws Exception khi kết nối lỗi hoặc tải lên thất bại
     */
    public static String upload(File file) throws Exception {
        URL url = new URL(UPLOAD_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setUseCaches(false);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);

        // Thiết lập timeout hợp lý (30 giây) để tránh treo vô hạn nếu mạng yếu
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);

        try (OutputStream outputStream = conn.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true)) {

            // Tạo phần header của file part trong multipart/form-data
            writer.append("--").append(BOUNDARY).append(LINE_FEED);
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(file.getName()).append("\"").append(LINE_FEED);
            
            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            writer.append("Content-Type: ").append(contentType).append(LINE_FEED);
            writer.append(LINE_FEED);
            writer.flush();

            // Ghi luồng byte của file ảnh vào luồng ra HTTP
            try (FileInputStream inputStream = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            outputStream.flush();

            // Ghi phần kết thúc của cấu trúc multipart
            writer.append(LINE_FEED);
            writer.append("--").append(BOUNDARY).append("--").append(LINE_FEED);
            writer.flush();
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                
                // Giải mã JSON kết quả trả về từ Cloudflare Worker
                JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                if (json.has("success") && json.get("success").getAsBoolean()) {
                    return json.get("url").getAsString();
                } else {
                    String errorMsg = json.has("message") ? json.get("message").getAsString() : "Lỗi tải ảnh lên R2";
                    throw new Exception(errorMsg);
                }
            }
        } else {
            throw new Exception("Lỗi kết nối HTTP: Mã phản hồi " + responseCode);
        }
    }
}
