package com.auctionuet.server.util.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.LocalDateTime;

public class GsonFactory {
    public static Gson create() {
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
