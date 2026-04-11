package com.auctionuet.server.persistence.dao;

import com.auctionuet.server.domain.enums.UserRole;
import com.auctionuet.server.persistence.schema.UserSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserDAOTest {

    private static final String TEST_FILE = "data/test_users.json";
    private UserDAO userDAO;

    @BeforeEach
    void setUp() {
        userDAO = new UserDAO(TEST_FILE);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(Path.of(TEST_FILE));
    }

    @Test
    void testSaveAndFindById() {
        UserSchema user = new UserSchema("1", LocalDateTime.now(), LocalDateTime.now(),
                "cuong123", "hash", "salt", "cuong@uet.vn", UserRole.BIDDER);
        userDAO.save(user);

        UserSchema found = userDAO.findById("1");
        assertNotNull(found);
        assertEquals("cuong123", found.getUsername());
    }

    @Test
    void testFindByUsername() {
        UserSchema user = new UserSchema("2", LocalDateTime.now(), LocalDateTime.now(),
                "testuser", "hash", "salt", "test@uet.vn", UserRole.SELLER);
        userDAO.save(user);

        UserSchema found = userDAO.findByUsername("testuser");
        assertNotNull(found);
        assertEquals("testuser", found.getUsername());
    }

    @Test
    void testFindAll() {
        for (int i = 1; i <= 3; i++) {
            UserSchema user = new UserSchema(String.valueOf(i), LocalDateTime.now(), LocalDateTime.now(),
                    "user" + i, "hash", "salt", "email" + i + "@uet.vn", UserRole.BIDDER);
            userDAO.save(user);
        }

        List<UserSchema> users = userDAO.findAll();
        assertEquals(3, users.size());
    }

    @Test
    void testUpdate() {
        UserSchema user = new UserSchema("1", LocalDateTime.now(), LocalDateTime.now(),
                "cuong123", "hash", "salt", "old@uet.vn", UserRole.BIDDER);
        userDAO.save(user);

        user.setEmail("new@uet.vn");
        userDAO.update(user);

        UserSchema found = userDAO.findById("1");
        assertNotNull(found);
        assertEquals("new@uet.vn", found.getEmail());
    }

    @Test
    void testDelete() {
        UserSchema user = new UserSchema("1", LocalDateTime.now(), LocalDateTime.now(),
                "cuong123", "hash", "salt", "cuong@uet.vn", UserRole.BIDDER);
        userDAO.save(user);

        userDAO.delete("1");

        UserSchema found = userDAO.findById("1");
        assertNull(found);
    }

    @Test
    void testFileNotExist() {
        List<UserSchema> users = userDAO.findAll();
        assertTrue(users.isEmpty());
    }

    @Test
    void testGsonLocalDateTime() {
        LocalDateTime now = LocalDateTime.now();
        // Remove nanoseconds because of JSON ISO parsing diff or standard formatting precision
        now = now.withNano(0); 

        UserSchema user = new UserSchema("1", now, now,
                "cuong123", "hash", "salt", "cuong@uet.vn", UserRole.BIDDER);
        userDAO.save(user);

        UserSchema found = userDAO.findById("1");
        assertNotNull(found);
        assertEquals(now, found.getCreatedAt());
        assertEquals(now, found.getUpdatedAt());
    }
}
