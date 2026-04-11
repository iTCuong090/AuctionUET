package com.auctionuet.server.util.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.LocalDateTime;

public class GsonFactory {
    public static Gson create() {
        // Tại đây, cắm các Adapter cần thiết theo ý muốn của ta để Gson làm việc được
        // với
        // Class phức tạp.
        return new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                // Cắm thêm Adapter bằng .registerTypeAdapter().
                .setPrettyPrinting()
                .create();
    }
}
