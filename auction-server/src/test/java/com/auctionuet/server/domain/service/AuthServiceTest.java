package com.auctionuet.server.domain.service;

import com.auctionuet.server.domain.enums.UserRole;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.exception.AuthenticationException;
import com.auctionuet.server.exception.DuplicateUserException;
import com.auctionuet.server.exception.UserNotFoundException;
import com.auctionuet.server.persistence.dao.UserDAO;
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
        authService = new AuthService(userDAO);
        sessionManager = SessionManager.getInstance();
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(Path.of(TEST_DB_PATH));
    }

    @Test
    void testRegisterAndLogin() {
        authService.register("newuser", "mypassword", "test@uet.vn", UserRole.BIDDER);
        LoginResult result = authService.login("newuser", "mypassword");

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
        LoginResult result = authService.login("sessionuser", "password123");
        
        User user = sessionManager.validateToken(result.getToken());
        assertNotNull(user);
        assertEquals("sessionuser", user.getUsername());
    }

    @Test
    void testLogout() {
        authService.register("logoutuser", "password123", "t@uet.vn", UserRole.BIDDER);
        LoginResult result = authService.login("logoutuser", "password123");
        
        sessionManager.removeSession(result.getToken());
        assertThrows(AuthenticationException.class, () -> sessionManager.validateToken(result.getToken()));
    }

    @Test
    void testSingletonSessionManager() {
        SessionManager sm1 = SessionManager.getInstance();
        SessionManager sm2 = SessionManager.getInstance();
        assertSame(sm1, sm2);
    }
}
