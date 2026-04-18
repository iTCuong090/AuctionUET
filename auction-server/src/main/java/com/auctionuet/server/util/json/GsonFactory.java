package com.auctionuet.server.util.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.LocalDateTime;

public class GsonFactory {
    public static Gson create() {
        // Đăng ký RuntimeTypeAdapterFactory để xử lý bài toán Đa hình (Polymorphism) cho ItemSchema.
        // Vấn đề: ItemSchema là abstract class. Khi lưu xuống JSON, thông tin về kiểu thực tế của đối tượng bị mất.
        // Giải pháp: 
        // - Khi ghi (Serialize): Dựa theo loại subclass, Gson sẽ tự động chèn thêm trường dữ liệu "itemType" 
        //   (với giá trị là "ELECTRONICS", "ART", hoặc "VEHICLE") vào chuỗi JSON.
        // - Khi đọc (Deserialize): Gson sẽ quét tìm trường "itemType" này trước tiên. Từ đó nó biết chính xác 
        //   cần khởi tạo subclass tương ứng nào (ElectronicsSchema, ArtSchema, VehicleSchema) và map dữ liệu 
        //   một cách trọn vẹn mà không bị lỗi.
        //
        // Công nghệ sử dụng: 
        // - RuntimeTypeAdapterFactory là công cụ nâng cao nằm trong gói `gson-extras` do chính Google phát triển.
        // - Thay vì cài nguyên thư viện gson-extras khổng lồ, source code của file này đã được chép trực tiếp vào 
        //   dự án (`util/json`) theo "best practice" nhằm giữ cấu trúc sạch (zero-bloat) trong khi vẫn tận dụng
        //   được giải pháp xử lý đa hình mạnh mẽ, an toàn (bảo vệ khỏi injection attacks) đã được kiểm chứng.
        RuntimeTypeAdapterFactory<com.auctionuet.server.persistence.schema.ItemSchema> itemFactory =
            RuntimeTypeAdapterFactory.of(com.auctionuet.server.persistence.schema.ItemSchema.class, "itemType")
                .recognizeSubtypes()
                .registerSubtype(com.auctionuet.server.persistence.schema.ElectronicsSchema.class, "ELECTRONICS")
                .registerSubtype(com.auctionuet.server.persistence.schema.ArtSchema.class, "ART")
                .registerSubtype(com.auctionuet.server.persistence.schema.VehicleSchema.class, "VEHICLE");

        return new GsonBuilder()
                .registerTypeAdapterFactory(itemFactory)
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .setPrettyPrinting()
                .create();
    }
    public static Gson createForNetwork() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                // KHÔNG gọi .setPrettyPrinting() ở đây
                .create();
    }
}
