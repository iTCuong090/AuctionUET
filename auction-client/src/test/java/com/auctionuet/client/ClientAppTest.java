package com.auctionuet.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClientAppTest {
    
    @Test
    void testAppNameNotNull() {
        assertNotNull(ClientApp.APP_NAME);
        assertEquals("AuctionUET Client", ClientApp.APP_NAME);
    }
}
