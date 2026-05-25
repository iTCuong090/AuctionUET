package com.auctionuet.server.domain.service;

import com.auctionuet.protocol.enums.UserRole;
import com.auctionuet.protocol.enums.AccountStatus;
import com.auctionuet.protocol.dto.response.auth.LoginResponseDTO;
import com.auctionuet.protocol.dto.response.user.UserDTO;
import com.auctionuet.server.domain.manager.SessionManager;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.exception.AuthenticationException;
import com.auctionuet.server.exception.DuplicateUserException;
import com.auctionuet.server.exception.UserNotFoundException;
import com.auctionuet.server.persistence.dao.UserDAO;
import com.auctionuet.server.persistence.schema.UserSchema;
import com.auctionuet.server.util.PasswordUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceTest {
    private static final String TEST_DB_PATH = "data/test_auth_users.json";
    private UserDAO userDAO;
    private AuthService authService;
    private SessionManager sessionManager;

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(Path.of("data"));
        Files.deleteIfExists(Path.of(TEST_DB_PATH));
        userDAO = new UserDAO(TEST_DB_PATH);
        UserService userService = new UserService(userDAO);
        authService = new AuthService(userDAO, userService);
        sessionManager = SessionManager.getInstance();
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(Path.of(TEST_DB_PATH));
    }

    @Test
    void testRegisterAndLogin() {
        authService.register("newuser", "mypassword", "test@uet.vn", UserRole.BIDDER);
        LoginResponseDTO result = authService.login("newuser", "mypassword");

        assertNotNull(result.getToken());
        assertNotNull(result.getUser());
        assertEquals("newuser", result.getUser().getUsername());
        assertEquals(UserRole.BIDDER, result.getUser().getRole());
    }

    @Test
    void testLoginWrongPassword() {
        authService.register("user1", "correctpass", "t1@uet.vn", UserRole.BIDDER);
        assertThrows(AuthenticationException.class, () -> authService.login("user1", "wrongpass"));
    }

    @Test
    void testLoginUserNotExist() {
        assertThrows(UserNotFoundException.class, () -> authService.login("ghost", "pass"));
    }

    @Test
    void testRegisterDuplicate() {
        authService.register("dupuser", "password123", "t@uet.vn", UserRole.BIDDER);
        assertThrows(DuplicateUserException.class, () -> 
            authService.register("dupuser", "password456", "t2@uet.vn", UserRole.SELLER));
    }

    @Test
    void testValidateToken() {
        authService.register("sessionuser", "password123", "t@uet.vn", UserRole.BIDDER);
        LoginResponseDTO result = authService.login("sessionuser", "password123");
        
        User user = sessionManager.validateToken(result.getToken());
        assertNotNull(user);
        assertEquals("sessionuser", user.getUsername());
    }

    @Test
    void testLogout() {
        authService.register("logoutuser", "password123", "t@uet.vn", UserRole.BIDDER);
        LoginResponseDTO result = authService.login("logoutuser", "password123");
        
        sessionManager.removeSession(result.getToken());
        assertThrows(AuthenticationException.class, () -> sessionManager.validateToken(result.getToken()));
    }

    @Test
    void testSingletonSessionManager() {
        SessionManager sm1 = SessionManager.getInstance();
        SessionManager sm2 = SessionManager.getInstance();
        assertSame(sm1, sm2);
    }

    @Test
    void testBlockedUserCannotLogin() {
        authService.register("blockeduser", "password123", "blocked@uet.vn", UserRole.BIDDER);
        UserSchema user = userDAO.findByUsername("blockeduser");
        user.setStatus(AccountStatus.BLOCKED);
        userDAO.update(user);

        assertThrows(AuthenticationException.class,
                () -> authService.login("blockeduser", "password123"));
    }

    @Test
    void testLoginReturnsRequiredPasswordChangeFlag() {
        authService.register("adminuser", "password123", "admin@uet.vn", UserRole.BIDDER);
        UserSchema user = userDAO.findByUsername("adminuser");
        user.setMustChangePassword(true);
        userDAO.update(user);

        LoginResponseDTO response = authService.login("adminuser", "password123");

        assertTrue(response.isMustChangePassword());
    }

    @Test
    void testLegacyUserWithoutStatusRemainsActive() throws IOException {
        String salt = PasswordUtils.generateSalt();
        String hash = PasswordUtils.hash("password123", salt);
        Files.writeString(Path.of(TEST_DB_PATH), """
                [{
                  "id": "legacy-user",
                  "username": "legacyuser",
                  "hashedPassword": "%s",
                  "passwordSalt": "%s",
                  "email": "legacy@uet.vn",
                  "role": "BIDDER",
                  "balance": 0.0,
                  "frozenBalance": 0.0
                }]
                """.formatted(hash, salt));

        LoginResponseDTO response = authService.login("legacyuser", "password123");

        assertEquals("legacyuser", response.getUser().getUsername());
    }
}
