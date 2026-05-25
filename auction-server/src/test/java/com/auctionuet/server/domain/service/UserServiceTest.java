package com.auctionuet.server.domain.service;

import com.auctionuet.protocol.dto.response.auth.LoginResponseDTO;
import com.auctionuet.protocol.dto.response.user.UserDTO;
import com.auctionuet.protocol.enums.UserRole;
import com.auctionuet.server.exception.AuthenticationException;
import com.auctionuet.server.exception.DuplicateUserException;
import com.auctionuet.server.persistence.dao.UserDAO;
import com.auctionuet.server.persistence.schema.UserSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UserServiceTest {
    private static final String TEST_DB_PATH = "data/test_user_service_users.json";

    private UserDAO userDAO;
    private UserService userService;
    private AuthService authService;

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(Path.of("data"));
        Files.deleteIfExists(Path.of(TEST_DB_PATH));
        userDAO = new UserDAO(TEST_DB_PATH);
        userService = new UserService(userDAO);
        authService = new AuthService(userDAO, userService);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(Path.of(TEST_DB_PATH));
    }

    @Test
    void testUpdateUsername() {
        authService.register("olduser", "password123", "old@uet.vn", UserRole.BIDDER);
        LoginResponseDTO login = authService.login("olduser", "password123");

        UserDTO updated = userService.updateProfile(login.getUser().getId(), "newuser", null, null);

        assertEquals("newuser", updated.getUsername());
        assertEquals("newuser", userDAO.findById(login.getUser().getId()).getUsername());
    }

    @Test
    void testUpdateUsernameRejectsDuplicate() {
        authService.register("userone", "password123", "one@uet.vn", UserRole.BIDDER);
        authService.register("usertwo", "password123", "two@uet.vn", UserRole.BIDDER);
        String userId = authService.login("userone", "password123").getUser().getId();

        assertThrows(DuplicateUserException.class,
                () -> userService.updateProfile(userId, "usertwo", null, null));
    }

    @Test
    void testUpdateUsernameUsesExistingValidation() {
        authService.register("validuser", "password123", "valid@uet.vn", UserRole.BIDDER);
        String userId = authService.login("validuser", "password123").getUser().getId();

        assertThrows(IllegalArgumentException.class,
                () -> userService.updateProfile(userId, "ab", null, null));
    }

    @Test
    void testUpdatePassword() {
        authService.register("passuser", "password123", "pass@uet.vn", UserRole.BIDDER);
        String userId = authService.login("passuser", "password123").getUser().getId();

        userService.updateProfile(userId, null, "password123", "newpass123");

        authService.login("passuser", "newpass123");
        assertThrows(AuthenticationException.class, () -> authService.login("passuser", "password123"));
    }

    @Test
    void testUpdatePasswordRejectsWrongCurrentPassword() {
        authService.register("wrongpass", "password123", "wrong@uet.vn", UserRole.BIDDER);
        UserSchema schema = userDAO.findByUsername("wrongpass");

        assertThrows(AuthenticationException.class,
                () -> userService.updateProfile(schema.getId(), null, "badpass123", "newpass123"));
    }

    @Test
    void testUpdatePasswordUsesExistingValidation() {
        authService.register("weakpass", "password123", "weak@uet.vn", UserRole.BIDDER);
        UserSchema schema = userDAO.findByUsername("weakpass");

        assertThrows(IllegalArgumentException.class,
                () -> userService.updateProfile(schema.getId(), null, "password123", "123"));
    }

    @Test
    void testUpdatePasswordClearsRequiredChangeFlag() {
        authService.register("forcedpass", "password123", "forced@uet.vn", UserRole.BIDDER);
        UserSchema schema = userDAO.findByUsername("forcedpass");
        schema.setMustChangePassword(true);
        userDAO.update(schema);

        userService.updateProfile(schema.getId(), null, "password123", "newpass123");

        assertEquals(false, userDAO.findById(schema.getId()).isMustChangePassword());
    }
}
