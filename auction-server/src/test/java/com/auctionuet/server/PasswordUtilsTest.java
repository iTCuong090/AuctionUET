package com.auctionuet.server;

import com.auctionuet.server.util.PasswordUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PasswordUtilsTest {

    @Test
    void testHashAndVerify() {
        String password = "abc123";
        String salt = PasswordUtils.generateSalt();

        String hash = PasswordUtils.hash(password, salt);

        // ⚠️ verify(password, salt, hashedPassword)
        assertTrue(PasswordUtils.verify(password, salt, hash));
    }

    @Test
    void testWrongPassword() {
        String password = "abc";
        String salt = PasswordUtils.generateSalt();

        String hash = PasswordUtils.hash(password, salt);

        // dùng password sai
        assertFalse(PasswordUtils.verify("xyz", salt, hash));
    }

    @Test
    void testDifferentSalt() {
        String password = "abc123";

        String salt1 = PasswordUtils.generateSalt();
        String salt2 = PasswordUtils.generateSalt();

        String hash1 = PasswordUtils.hash(password, salt1);
        String hash2 = PasswordUtils.hash(password, salt2);

        assertNotEquals(hash1, hash2);
    }

    @Test
    void testSaltGeneration() {
        String salt1 = PasswordUtils.generateSalt();
        String salt2 = PasswordUtils.generateSalt();

        assertNotEquals(salt1, salt2);
    }
}