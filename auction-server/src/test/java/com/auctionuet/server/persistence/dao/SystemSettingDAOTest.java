package com.auctionuet.server.persistence.dao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SystemSettingDAOTest {
    private static final String TEST_FILE = "data/test_system_settings.json";

    @AfterEach
    void tearDown() throws Exception {
        Files.deleteIfExists(Path.of(TEST_FILE));
    }

    @Test
    void testMissingSettingDefaultsToApprovalEnabledAndPersistsChange() {
        SystemSettingDAO dao = new SystemSettingDAO(TEST_FILE);
        assertTrue(dao.isItemApprovalEnabled());

        dao.setItemApprovalEnabled(false);

        assertFalse(new SystemSettingDAO(TEST_FILE).isItemApprovalEnabled());
    }
}
