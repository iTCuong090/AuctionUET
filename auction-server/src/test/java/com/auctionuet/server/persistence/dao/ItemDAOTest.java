package com.auctionuet.server.persistence.dao;

import com.auctionuet.server.domain.enums.ItemType;
import com.auctionuet.server.persistence.schema.ArtSchema;
import com.auctionuet.server.persistence.schema.ElectronicsSchema;
import com.auctionuet.server.persistence.schema.ItemSchema;
import com.auctionuet.server.persistence.schema.VehicleSchema;
import com.auctionuet.server.util.json.GsonFactory;
import com.google.gson.Gson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

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
    public void testSaveAndFindElectronics() {
        ElectronicsSchema e = new ElectronicsSchema(UUID.randomUUID().toString(), LocalDateTime.now(), LocalDateTime.now(),
                "iPhone", "Apple phone", 1000.0, ItemType.ELECTRONICS, "seller1", "url", "NEW", 0, "Apple", 12);
        itemDAO.save(e);
        ItemSchema found = itemDAO.findById(e.getId());
        assertTrue(found instanceof ElectronicsSchema);
        assertEquals("Apple", ((ElectronicsSchema) found).getBrand());
        assertEquals(12, ((ElectronicsSchema) found).getWarrantyMonths());
    }

    @Test
    public void testSaveAndFindArt() {
        ArtSchema a = new ArtSchema(UUID.randomUUID().toString(), LocalDateTime.now(), LocalDateTime.now(),
                "Painting", "Desc", 500.0, ItemType.ART, "seller1", "url", "GOOD", 0, "Da Vinci", 1500, "Oil");
        itemDAO.save(a);
        ItemSchema found = itemDAO.findById(a.getId());
        assertTrue(found instanceof ArtSchema);
        assertEquals("Da Vinci", ((ArtSchema) found).getArtist());
        assertEquals(1500, ((ArtSchema) found).getYear());
    }

    @Test
    public void testSaveAndFindVehicle() {
        VehicleSchema v = new VehicleSchema(UUID.randomUUID().toString(), LocalDateTime.now(), LocalDateTime.now(),
                "Car", "Desc", 20000.0, ItemType.VEHICLE, "seller1", "url", "USED", 0, "Toyota", "Camry", 10000, 2020);
        itemDAO.save(v);
        ItemSchema found = itemDAO.findById(v.getId());
        assertTrue(found instanceof VehicleSchema);
        assertEquals("Toyota", ((VehicleSchema) found).getMake());
        assertEquals("Camry", ((VehicleSchema) found).getModel());
    }

    @Test
    public void testItemPolymorphism() {
        ElectronicsSchema e = new ElectronicsSchema(UUID.randomUUID().toString(), LocalDateTime.now(), LocalDateTime.now(),
                "iPhone", "Apple phone", 1000.0, ItemType.ELECTRONICS, "seller1", "url", "NEW", 0, "Apple", 12);
        ArtSchema a = new ArtSchema(UUID.randomUUID().toString(), LocalDateTime.now(), LocalDateTime.now(),
                "Painting", "Desc", 500.0, ItemType.ART, "seller1", "url", "GOOD", 0, "Da Vinci", 1500, "Oil");
        itemDAO.save(e);
        itemDAO.save(a);
        List<ItemSchema> all = itemDAO.findAll();
        assertEquals(2, all.size());
        assertTrue(all.stream().anyMatch(i -> i instanceof ElectronicsSchema));
        assertTrue(all.stream().anyMatch(i -> i instanceof ArtSchema));
    }

    @Test
    public void testFindBySellerId() {
        ElectronicsSchema e = new ElectronicsSchema(UUID.randomUUID().toString(), LocalDateTime.now(), LocalDateTime.now(),
                "iPhone", "Apple phone", 1000.0, ItemType.ELECTRONICS, "A", "url", "NEW", 0, "Apple", 12);
        ArtSchema a = new ArtSchema(UUID.randomUUID().toString(), LocalDateTime.now(), LocalDateTime.now(),
                "Painting", "Desc", 500.0, ItemType.ART, "B", "url", "GOOD", 0, "Da Vinci", 1500, "Oil");
        itemDAO.save(e);
        ElectronicsSchema e2 = new ElectronicsSchema(UUID.randomUUID().toString(), LocalDateTime.now(), LocalDateTime.now(),
                "Samsung", "phone", 500.0, ItemType.ELECTRONICS, "A", "url", "NEW", 0, "Samsung", 12);
        itemDAO.save(e2);
        itemDAO.save(a);

        List<ItemSchema> sellerAItems = itemDAO.findBySellerId("A");
        assertEquals(2, sellerAItems.size());
    }

    @Test
    public void testGsonItemSubclass() {
        ElectronicsSchema e = new ElectronicsSchema("123", LocalDateTime.now(), LocalDateTime.now(),
                "iPhone", "Apple phone", 1000.0, ItemType.ELECTRONICS, "seller1", "url", "NEW", 0, "Apple", 12);
        Gson gson = GsonFactory.create();
        String json = gson.toJson(e, ItemSchema.class);
        ItemSchema parsed = gson.fromJson(json, ItemSchema.class);
        assertTrue(parsed instanceof ElectronicsSchema);
        assertEquals("Apple", ((ElectronicsSchema) parsed).getBrand());
    }
}
