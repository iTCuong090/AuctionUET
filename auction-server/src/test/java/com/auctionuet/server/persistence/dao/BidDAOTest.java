package com.auctionuet.server.persistence.dao;

import com.auctionuet.protocol.enums.BidType;
import com.auctionuet.server.persistence.schema.BidSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class BidDAOTest {
    private BidDAO bidDAO;
    private final String testFile = "data/test_bids.json";

    @BeforeEach
    public void setup() {
        bidDAO = new BidDAO(testFile);
        new File(testFile).delete();
    }

    @AfterEach
    public void teardown() {
        new File(testFile).delete();
    }

    @Test
    public void testBidSaveAndFindByAuction() {
        BidSchema b1 = new BidSchema(UUID.randomUUID().toString(), LocalDateTime.now(), LocalDateTime.now(),
                "A", "bidder1", 100.0, LocalDateTime.now(), BidType.MANUAL);
        BidSchema b2 = new BidSchema(UUID.randomUUID().toString(), LocalDateTime.now(), LocalDateTime.now(),
                "A", "bidder2", 200.0, LocalDateTime.now(), BidType.MANUAL);
        BidSchema b3 = new BidSchema(UUID.randomUUID().toString(), LocalDateTime.now(), LocalDateTime.now(),
                "B", "bidder1", 150.0, LocalDateTime.now(), BidType.MANUAL);

        bidDAO.save(b1);
        bidDAO.save(b2);
        bidDAO.save(b3);

        List<BidSchema> auctionABids = bidDAO.findByAuctionId("A");
        assertEquals(2, auctionABids.size());
    }
}
