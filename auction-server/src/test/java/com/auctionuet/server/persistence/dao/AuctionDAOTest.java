package com.auctionuet.server.persistence.dao;

import com.auctionuet.server.domain.enums.AuctionStatus;
import com.auctionuet.server.persistence.schema.AuctionSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionDAOTest {
    private AuctionDAO auctionDAO;
    private final String testFile = "data/test_auctions.json";

    @BeforeEach
    public void setup() {
        auctionDAO = new AuctionDAO(testFile);
        new File(testFile).delete();
    }

    @AfterEach
    public void teardown() {
        new File(testFile).delete();
    }

    @Test
    public void testAuctionSaveAndFind() {
        AuctionSchema a = new AuctionSchema(UUID.randomUUID().toString(), LocalDateTime.now(), LocalDateTime.now(),
                "item1", "seller1", "Title", "Desc", LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                AuctionStatus.OPEN, 0.0, null);
        auctionDAO.save(a);

        AuctionSchema found = auctionDAO.findById(a.getId());
        assertNotNull(found);
        assertEquals(AuctionStatus.OPEN, found.getStatus());
        assertEquals("item1", found.getItemId());
    }

    @Test
    public void testFindByStatus() {
        AuctionSchema a1 = new AuctionSchema(UUID.randomUUID().toString(), LocalDateTime.now(), LocalDateTime.now(),
                "item1", "seller1", "Title", "Desc", LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                AuctionStatus.OPEN, 0.0, null);
        AuctionSchema a2 = new AuctionSchema(UUID.randomUUID().toString(), LocalDateTime.now(), LocalDateTime.now(),
                "item2", "seller1", "Title", "Desc", LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                AuctionStatus.OPEN, 0.0, null);
        AuctionSchema a3 = new AuctionSchema(UUID.randomUUID().toString(), LocalDateTime.now(), LocalDateTime.now(),
                "item3", "seller1", "Title", "Desc", LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                AuctionStatus.RUNNING, 0.0, null);

        auctionDAO.save(a1);
        auctionDAO.save(a2);
        auctionDAO.save(a3);

        List<AuctionSchema> openAuctions = auctionDAO.findByStatus(AuctionStatus.OPEN);
        assertEquals(2, openAuctions.size());
    }
}
