package com.auctionuet.server.domain.service;

import com.auctionuet.protocol.ActionType;
import com.auctionuet.protocol.contract.Dto;
import com.auctionuet.protocol.enums.UserRole;
import com.auctionuet.server.domain.manager.SessionManager;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.exception.AuthenticationException;
import com.auctionuet.server.exception.DuplicateUserException;
import com.auctionuet.server.exception.UserNotFoundException;
import com.auctionuet.server.domain.model.Admin;
import com.auctionuet.server.domain.model.Bidder;
import com.auctionuet.server.domain.model.Seller;
import com.auctionuet.server.util.IdGenerator;
import java.time.LocalDateTime;
import com.auctionuet.server.persistence.dao.UserDAO;
import com.auctionuet.server.persistence.schema.UserSchema;
import com.auctionuet.server.util.PasswordUtils;

import java.util.HashMap;
import java.util.Map;

public class AuthService {

    private final UserDAO userDAO;
    private final SessionManager sessionManager;

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
        this.sessionManager = SessionManager.getInstance();
    }

    public Dto<?> login(String username, String password) {
        UserSchema schema = userDAO.findByUsername(username);

        if (schema == null) {
            throw new UserNotFoundException(username);
        }

        boolean isValid = PasswordUtils.verify(password, schema.getPasswordSalt(), schema.getHashedPassword());
        if (!isValid) {
            throw new AuthenticationException("Sai mật khẩu");
        }

        User user = switch (schema.getRole()) {
            case BIDDER -> new Bidder(schema.getId(), schema.getUsername());
            case SELLER -> new Seller(schema.getId(), schema.getUsername());
            case ADMIN -> new Admin(schema.getId(), schema.getUsername());
        };
        String token = sessionManager.createSession(user);

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("username", user.getUsername());
        userMap.put("role", user.getRole().name());

        return ActionType.LOGIN.createResponseDto()
                .set("token", token)
                .set("user", userMap);
    }

    public void register(String username, String password, String email, UserRole role) {
        if (role == UserRole.ADMIN) {
            throw new IllegalArgumentException("Không được phép đăng ký tài khoản ADMIN");
        }

        // Kiểm tra xem username đã tồn tại chưa.
        UserSchema existingSchema = userDAO.findByUsername(username);
        if (existingSchema != null) {
            throw new DuplicateUserException(username);
        }

        String salt = PasswordUtils.generateSalt();
        String hashedPassword = PasswordUtils.hash(password, salt);

        String id = IdGenerator.generate();
        LocalDateTime now = LocalDateTime.now();
        UserSchema newSchema = new UserSchema(id, now, now, username, hashedPassword, salt, email, role);
        userDAO.save(newSchema);
    }
}
