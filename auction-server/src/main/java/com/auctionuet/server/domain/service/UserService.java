package com.auctionuet.server.domain.service;

import com.auctionuet.protocol.dto.response.user.UserDTO;
import com.auctionuet.server.domain.model.Admin;
import com.auctionuet.server.domain.model.Bidder;
import com.auctionuet.server.domain.model.Seller;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.exception.AuthenticationException;
import com.auctionuet.server.exception.DuplicateUserException;
import com.auctionuet.server.exception.UserNotFoundException;
import com.auctionuet.server.persistence.dao.UserDAO;
import com.auctionuet.server.persistence.schema.UserSchema;
import com.auctionuet.server.util.PasswordUtils;
import com.auctionuet.server.util.ValidationUtils;

public class UserService {
    private final UserDAO userDAO;

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public UserDTO toDTO(User user) {
        if (user == null) {
            return null;
        }
        return new UserDTO(user.getId(), user.getUsername(), user.getRole());
    }

    public UserDTO toDTO(UserSchema schema) {
        if (schema == null) {
            return null;
        }
        return new UserDTO(schema.getId(), schema.getUsername(), schema.getRole());
    }

    public UserDTO getUserDTOById(String id) {
        UserSchema schema = userDAO.findById(id);
        if (schema == null) {
            throw new UserNotFoundException(id);
        }
        return toDTO(schema);
    }

    public UserSchema getUserSchemaById(String id) {
        UserSchema schema = userDAO.findById(id);
        if (schema == null) {
            throw new UserNotFoundException(id);
        }
        return schema;
    }

    public User toDomainUser(UserSchema schema) {
        if (schema == null) {
            return null;
        }
        return switch (schema.getRole()) {
            case BIDDER -> new Bidder(schema.getId(), schema.getUsername());
            case SELLER -> new Seller(schema.getId(), schema.getUsername());
            case ADMIN -> new Admin(schema.getId(), schema.getUsername());
        };
    }

    public UserDTO updateProfile(
            String userId,
            String newUsername,
            String currentPassword,
            String newPassword) {
        UserSchema schema = getUserSchemaById(userId);

        boolean hasUsername = newUsername != null && !newUsername.isBlank();
        boolean hasPasswordChange = newPassword != null && !newPassword.isBlank();

        if (!hasUsername && !hasPasswordChange) {
            throw new IllegalArgumentException("Không có thông tin profile cần cập nhật");
        }

        if (hasUsername) {
            String normalizedUsername = newUsername.trim();
            ValidationUtils.validateUsername(normalizedUsername);
            UserSchema existing = userDAO.findByUsername(normalizedUsername);
            if (existing != null && !existing.getId().equals(schema.getId())) {
                throw new DuplicateUserException(normalizedUsername);
            }
            schema.setUsername(normalizedUsername);
        }

        if (hasPasswordChange) {
            ValidationUtils.validatePassword(newPassword);
            boolean currentPasswordValid = PasswordUtils.verify(
                    currentPassword,
                    schema.getPasswordSalt(),
                    schema.getHashedPassword());
            if (!currentPasswordValid) {
                throw new AuthenticationException("Mật khẩu hiện tại không đúng");
            }
            if (PasswordUtils.verify(newPassword, schema.getPasswordSalt(), schema.getHashedPassword())) {
                throw new IllegalArgumentException("Mật khẩu mới không được trùng mật khẩu hiện tại");
            }
            String salt = PasswordUtils.generateSalt();
            schema.setPasswordSalt(salt);
            schema.setHashedPassword(PasswordUtils.hash(newPassword, salt));
        }

        userDAO.update(schema);
        return toDTO(schema);
    }
}

