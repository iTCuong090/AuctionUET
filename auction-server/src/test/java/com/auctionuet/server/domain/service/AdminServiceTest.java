package com.auctionuet.server.domain.service;

import com.auctionuet.protocol.dto.response.admin.AdminUserDTO;
import com.auctionuet.protocol.dto.response.auth.LoginResponseDTO;
import com.auctionuet.protocol.enums.AccountStatus;
import com.auctionuet.protocol.enums.UserRole;
import com.auctionuet.server.domain.manager.SessionManager;
import com.auctionuet.server.domain.model.Admin;
import com.auctionuet.server.exception.AuthenticationException;
import com.auctionuet.server.persistence.dao.UserDAO;
import com.auctionuet.server.persistence.schema.UserSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AdminServiceTest {
    private static final String TEST_DB_PATH = "data/test_admin_users.json";

    private UserDAO userDAO;
    private UserService userService;
    private AuthService authService;
    private AdminService adminService;

    @BeforeEach
    void setUp() throws Exception {
        Files.createDirectories(Path.of("data"));
        Files.deleteIfExists(Path.of(TEST_DB_PATH));
        userDAO = new UserDAO(TEST_DB_PATH);
        userService = new UserService(userDAO);
        authService = new AuthService(userDAO, userService);
        adminService = new AdminService(userDAO);
    }

    @AfterEach
    void tearDown() throws Exception {
        Files.deleteIfExists(Path.of(TEST_DB_PATH));
    }

    @Test
    void testEnsureDefaultAdminExistsCreatesOneRequiredPasswordAccount() {
        adminService.ensureDefaultAdminExists();
        adminService.ensureDefaultAdminExists();

        List<UserSchema> users = userDAO.findAll();
        assertEquals(1, users.size());
        UserSchema admin = users.get(0);
        assertEquals("admin", admin.getUsername());
        assertEquals(UserRole.ADMIN, admin.getRole());
        assertEquals(AccountStatus.ACTIVE, admin.getStatus());
        assertTrue(admin.isMustChangePassword());

        LoginResponseDTO login = authService.login("admin", "admin@123");
        assertTrue(login.isMustChangePassword());
    }

    @Test
    void testGetUsersFiltersUsernameRoleAndStatus() {
        authService.register("seller_target", "password123", "seller@uet.vn", UserRole.SELLER);
        authService.register("bidder_target", "password123", "bidder@uet.vn", UserRole.BIDDER);
        UserSchema seller = userDAO.findByUsername("seller_target");
        seller.setStatus(AccountStatus.BLOCKED);
        userDAO.update(seller);

        List<AdminUserDTO> result = adminService.getUsers("SELLER", UserRole.SELLER, AccountStatus.BLOCKED);

        assertEquals(1, result.size());
        assertEquals("seller_target", result.get(0).getUsername());
        assertEquals(AccountStatus.BLOCKED, result.get(0).getStatus());
    }

    @Test
    void testBlockUserInvalidatesSessionAndUnlockAllowsLoginAgain() {
        authService.register("targetuser", "password123", "target@uet.vn", UserRole.BIDDER);
        LoginResponseDTO targetLogin = authService.login("targetuser", "password123");
        Admin admin = new Admin("admin-operator", "operator");

        adminService.updateUserStatus(admin, targetLogin.getUser().getId(), AccountStatus.BLOCKED);

        assertThrows(AuthenticationException.class,
                () -> SessionManager.getInstance().validateToken(targetLogin.getToken()));
        assertThrows(AuthenticationException.class,
                () -> authService.login("targetuser", "password123"));

        adminService.updateUserStatus(admin, targetLogin.getUser().getId(), AccountStatus.ACTIVE);
        authService.login("targetuser", "password123");
    }

    @Test
    void testAdminCannotBlockAdminAccount() {
        adminService.ensureDefaultAdminExists();
        UserSchema target = userDAO.findByUsername("admin");

        assertThrows(IllegalArgumentException.class,
                () -> adminService.updateUserStatus(
                        new Admin("admin-operator", "operator"),
                        target.getId(),
                        AccountStatus.BLOCKED));
    }
}
