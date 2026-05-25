package com.auctionuet.server.domain.service;

import com.auctionuet.protocol.dto.response.admin.AdminUserDTO;
import com.auctionuet.protocol.enums.AccountStatus;
import com.auctionuet.protocol.enums.UserRole;
import com.auctionuet.server.domain.manager.SessionManager;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.persistence.dao.UserDAO;
import com.auctionuet.server.persistence.schema.UserSchema;
import com.auctionuet.server.util.IdGenerator;
import com.auctionuet.server.util.PasswordUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

public class AdminService {
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin@123";
    private static final String DEFAULT_ADMIN_EMAIL = "admin@auctionuet.com";

    private final UserDAO userDAO;
    private final SessionManager sessionManager;

    public AdminService(UserDAO userDAO) {
        this.userDAO = userDAO;
        this.sessionManager = SessionManager.getInstance();
    }

    public synchronized void ensureDefaultAdminExists() {
        boolean adminExists = userDAO.findAll().stream()
                .anyMatch(user -> user.getRole() == UserRole.ADMIN);
        if (adminExists) {
            return;
        }

        String salt = PasswordUtils.generateSalt();
        LocalDateTime now = LocalDateTime.now();
        UserSchema admin = new UserSchema(
                IdGenerator.generate(),
                now,
                now,
                DEFAULT_ADMIN_USERNAME,
                PasswordUtils.hash(DEFAULT_ADMIN_PASSWORD, salt),
                salt,
                DEFAULT_ADMIN_EMAIL,
                UserRole.ADMIN);
        admin.setStatus(AccountStatus.ACTIVE);
        admin.setMustChangePassword(true);
        userDAO.save(admin);
    }

    public List<AdminUserDTO> getUsers(String usernameQuery, UserRole role, AccountStatus status) {
        String normalizedQuery = usernameQuery == null
                ? ""
                : usernameQuery.trim().toLowerCase(Locale.ROOT);
        return userDAO.findAll().stream()
                .filter(user -> normalizedQuery.isEmpty()
                        || user.getUsername().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .filter(user -> role == null || user.getRole() == role)
                .filter(user -> status == null || user.getStatus() == status)
                .map(this::toDTO)
                .toList();
    }

    public AdminUserDTO updateUserStatus(User admin, String targetUserId, AccountStatus status) {
        if (admin.getId().equals(targetUserId)) {
            throw new IllegalArgumentException("Admin không thể khóa tài khoản của chính mình");
        }

        UserSchema target = userDAO.findById(targetUserId);
        if (target == null) {
            throw new IllegalArgumentException("Không tìm thấy tài khoản");
        }
        if (target.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("Không thể khóa hoặc mở khóa tài khoản Admin");
        }

        target.setStatus(status);
        userDAO.update(target);
        if (status == AccountStatus.BLOCKED) {
            sessionManager.invalidateByUserId(targetUserId);
        }
        return toDTO(target);
    }

    private AdminUserDTO toDTO(UserSchema schema) {
        return new AdminUserDTO(
                schema.getId(),
                schema.getUsername(),
                schema.getRole(),
                schema.getStatus());
    }
}
