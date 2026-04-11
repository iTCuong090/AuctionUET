package com.auctionuet.server.persistence.json;

import com.auctionuet.server.util.json.GsonFactory;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class JsonFileHelper {
    public static <T> List<T> readList(String filePath, Class<T> clazz) {
        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }

        try {
            String content = Files.readString(path);
            Gson gson = GsonFactory.create();
            Type listType = TypeToken.getParameterized(List.class, clazz).getType();
            List<T> result = gson.fromJson(content, listType);
            if (result == null) {
                return new ArrayList<>();
            }
            return result;
        } catch (IOException e) {
            // Can be ignored or logged, but as per instructions if not found or empty we should handle it
            // Assuming we just return empty list in case of IO problem for reading gracefully
            return new ArrayList<>();
        }
    }

    public static <T> void writeList(String filePath, List<T> list) {
        Path path = Path.of(filePath);
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Gson gson = GsonFactory.create();
            String jsonString = gson.toJson(list);
            Files.writeString(path, jsonString);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to JSON file: " + filePath, e);
        }
    }
}
