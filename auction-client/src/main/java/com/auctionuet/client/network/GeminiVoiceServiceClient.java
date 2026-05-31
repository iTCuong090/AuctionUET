package com.auctionuet.client.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.Base64;

/**
 * Lớp Client thực hiện kết nối HTTPS đến Google Gemini API (gemini-2.5-flash-lite)
 * sử dụng HttpClient tích hợp sẵn trong Java 21.
 * Gửi file âm thanh dạng inline Base64 kèm Prompt định hướng kết quả để nhận diện số tiền đấu giá.
 */
public class GeminiVoiceServiceClient {
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent";
    private final HttpClient httpClient;

    public GeminiVoiceServiceClient() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Gửi file âm thanh ghi âm đến Google Gemini API để lắng nghe và chuyển đổi thành số tiền đặt giá.
     * 
     * @param audioFile File âm thanh định dạng WAV tạm thời
     * @param apiKey API Key của Google Gemini được lưu trong RAM
     * @return Số tiền đấu giá nhận diện được dưới dạng số nguyên (long), trả về 0 nếu không phát hiện số tiền hợp lệ.
     */
    public long processVoiceBid(File audioFile, String apiKey) throws IOException, InterruptedException {
        return processVoiceBid(audioFile, apiKey, "gemini-2.5-flash-lite");
    }

    public long processVoiceBid(File audioFile, String apiKey, String modelName) throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Vui lòng cấu hình Google Gemini API Key trước khi sử dụng.");
        }

        String actualModel = (modelName == null || modelName.isBlank()) ? "gemini-2.5-flash-lite" : modelName.trim();
        String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + actualModel + ":generateContent";

        // 1. Đọc và mã hóa file âm thanh sang Base64
        byte[] fileBytes = Files.readAllBytes(audioFile.toPath());
        String base64Data = Base64.getEncoder().encodeToString(fileBytes);

        // 2. Định nghĩa Prompt để Gemini vừa đóng vai trò STT vừa đóng vai trò phân tích ngữ nghĩa
        String prompt = "Hãy nghe kỹ file âm thanh ghi âm tiếng Việt này và trích xuất số tiền đấu giá mà người dùng muốn đặt. " +
                "Chỉ trả về DUY NHẤT một số nguyên đại diện cho số tiền đấu giá đó. " +
                "Ví dụ: \n" +
                "- Người dùng nói 'năm triệu' -> trả về '5000000'\n" +
                "- Người dùng nói 'năm triệu rưỡi' -> trả về '5500000'\n" +
                "- Người dùng nói 'cho tôi đặt ba trăm nghìn' -> trả về '300000'\n" +
                "- Người dùng nói 'bid năm trăm k' -> trả về '500000'\n" +
                "- Người dùng nói '1 tỷ 200 triệu' -> trả về '1200000000'\n" +
                "Yêu cầu nghiêm ngặt: KHÔNG GIẢI THÍCH, không kèm theo bất kỳ chữ cái, dấu câu hoặc ký tự nào khác. " +
                "Nếu không nghe rõ số tiền hoặc không có âm thanh hợp lệ, trả về số '0'.";

        // 3. Tạo JSON payload sử dụng Gson để đảm bảo an toàn cú pháp
        JsonObject requestBody = new JsonObject();
        
        JsonObject inlineData = new JsonObject();
        inlineData.addProperty("mimeType", "audio/wav");
        inlineData.addProperty("data", base64Data);

        JsonObject partAudio = new JsonObject();
        partAudio.add("inlineData", inlineData);

        JsonObject partText = new JsonObject();
        partText.addProperty("text", prompt);

        com.google.gson.JsonArray parts = new com.google.gson.JsonArray();
        parts.add(partAudio);
        parts.add(partText);

        JsonObject contentObj = new JsonObject();
        contentObj.add("parts", parts);

        com.google.gson.JsonArray contents = new com.google.gson.JsonArray();
        contents.add(contentObj);

        requestBody.add("contents", contents);

        String jsonPayload = requestBody.toString();

        // 4. Tạo HTTP Request và gửi đi
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("X-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Lỗi kết nối Gemini API (HTTP " + response.statusCode() + "): " + response.body());
        }

        // 5. Phân tích cú pháp phản hồi JSON để lấy nội dung text
        return extractAmountFromResponse(response.body());
    }

    private long extractAmountFromResponse(String jsonResponse) {
        try {
            JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();
            String textResult = root.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString()
                    .replaceAll("[^0-9]", "") // Loại bỏ mọi ký tự không phải số đề phòng mô hình trả về chữ lẻ tẻ
                    .trim();

            if (textResult.isEmpty()) {
                return 0;
            }
            return Long.parseLong(textResult);
        } catch (Exception e) {
            return 0;
        }
    }
}
