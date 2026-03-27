package com.auctionuet.server;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServerAppTest {
    
    @Test
    void testGetWelcomeMessage() {
        String message = ServerApp.getWelcomeMessage();
        assertNotNull(message);
        assertTrue(message.contains("Broken Server"));
        assertTrue(message.contains("1.0-SNAPSHOT"));
    }
    
    @Test
    void testAppNameNotNull() {
        assertNotNull(ServerApp.APP_NAME);
        assertFalse(ServerApp.APP_NAME.isEmpty());
    }
}
