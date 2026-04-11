package com.auctionuet.server;

import com.auctionuet.server.util.ValidationUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ValidationUtilsTest {

    @Test
    void testValidEmail() {
        assertDoesNotThrow(() ->
                ValidationUtils.validateEmail("user@uet.vn")
        );
    }

    @Test
    void testInvalidEmail() {
        assertThrows(IllegalArgumentException.class, () ->
                ValidationUtils.validateEmail("not-email")
        );
    }

    @Test
    void testWeakPassword() {
        assertThrows(IllegalArgumentException.class, () ->
                ValidationUtils.validatePassword("123")
        );
    }

    @Test
    void testValidPassword() {
        assertDoesNotThrow(() ->
                ValidationUtils.validatePassword("abc12345")
        );
    }

    @Test
    void testShortUsername() {
        assertThrows(IllegalArgumentException.class, () ->
                ValidationUtils.validateUsername("ab")
        );
    }
}