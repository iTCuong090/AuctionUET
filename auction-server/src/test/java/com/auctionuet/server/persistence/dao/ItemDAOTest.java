package com.auctionuet.server.persistence.dao;

import com.auctionuet.protocol.enums.ItemCondition;
import com.auctionuet.protocol.enums.ItemType;
import com.auctionuet.server.persistence.schema.ItemSchema;
import com.auctionuet.server.util.json.GsonFactory;
import com.google.gson.Gson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ItemDAOTest {
    private ItemDAO itemDAO;
    private final String testFile = "data/test_items.json";

    @BeforeEach
    public void setup() {
        itemDAO = new ItemDAO(testFile);
        new File(testFile).delete();
    }

    @AfterEach
    public void teardown() {
        new File(testFile).delete();
    }

    @Test
    public void testSaveAndFindByIdWithExtraFields() {
        ItemSchema item = new ItemSchema(
                UUID.randomUUID().toString(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                "iPhone",
                "Apple phone",
                1000.0,
                ItemType.ELECTRONICS,
                "seller1",
                "url",
                ItemCondition.NEW,
                0,
                new LinkedHashMap<>(Map.of("brand", "Apple", "warrantyMonths", 12))
        );

        itemDAO.save(item);

        ItemSchema found = itemDAO.findById(item.getId());
        assertNotNull(found);
        assertEquals(ItemType.ELECTRONICS, found.getType());
        assertEquals("Apple", found.getExtraFields().get("brand"));
        assertEquals(12.0, found.getExtraFields().get("warrantyMonths"));
    }

    @Test
    public void testFindBySellerId() {
        ItemSchema item1 = new ItemSchema(
                UUID.randomUUID().toString(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                "iPhone",
                "Apple phone",
                1000.0,
                ItemType.ELECTRONICS,
                "A",
                "url",
                ItemCondition.NEW,
                0,
                new LinkedHashMap<>(Map.of("brand", "Apple", "warrantyMonths", 12))
        );
        ItemSchema item2 = new ItemSchema(
                UUID.randomUUID().toString(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                "Painting",
                "Desc",
                500.0,
                ItemType.ART,
                "B",
                "url",
                ItemCondition.GOOD,
                0,
                new LinkedHashMap<>(Map.of("artist", "Da Vinci", "year", 1500, "medium", "Oil"))
        );
        ItemSchema item3 = new ItemSchema(
                UUID.randomUUID().toString(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                "Samsung",
                "phone",
                500.0,
                ItemType.ELECTRONICS,
                "A",
                "url",
                ItemCondition.NEW,
                0,
                new LinkedHashMap<>(Map.of("brand", "Samsung", "warrantyMonths", 24))
        );

        itemDAO.save(item1);
        itemDAO.save(item2);
        itemDAO.save(item3);

        List<ItemSchema> sellerAItems = itemDAO.findBySellerId("A");
        assertEquals(2, sellerAItems.size());
    }

    @Test
    public void testGsonItemSchemaRoundTrip() {
        ItemSchema item = new ItemSchema(
                "123",
                LocalDateTime.now(),
                LocalDateTime.now(),
                "Camry",
                "Sedan",
                20000.0,
                ItemType.VEHICLE,
                "seller1",
                "url",
                ItemCondition.FAIR,
                0,
                new LinkedHashMap<>(Map.of("make", "Toyota", "model", "Camry", "mileage", 10000, "vehicleYear", 2020))
        );

        Gson gson = GsonFactory.create();
        String json = gson.toJson(item, ItemSchema.class);
        ItemSchema parsed = gson.fromJson(json, ItemSchema.class);

        assertNotNull(parsed);
        assertEquals(ItemType.VEHICLE, parsed.getType());
        assertEquals("Toyota", parsed.getExtraFields().get("make"));
    }
}
