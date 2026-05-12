package com.auctionuet.server.domain.service;

import com.auctionuet.protocol.dto.response.auth.LoginResponseDTO;
import com.auctionuet.protocol.dto.response.user.UserDTO;
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
import com.auctionuet.server.util.ValidationUtils;

public class AuthService {

    private final UserDAO userDAO;
    private final SessionManager sessionManager;

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
        this.sessionManager = SessionManager.getInstance();
    }
    // Tại sao constructor lại phải nhận 1 đối tượng loại UserDAO? Vì các đối tượng userDao có thể custom
    // đường dẫn file database phục vụ test. Hơn nữa, sau này nếu ta có thay đổi phía DAO thì truyền vào
    // một đối tượng kiểu xxxDAO kế thừa userdao thì cũng ko cần phải thay đổi code ở authservice.

    public LoginResponseDTO login(String username, String password) {
        // Thực hiện truy vấn database cái username.
        UserSchema schema = userDAO.findByUsername(username);

        // Không tìm thấy username thì nhả usernotfound.
        if (schema == null) {
            throw new UserNotFoundException(username);
        }

        // Nếu tìm thấy user thì kiểm tra password.
        boolean isValid = PasswordUtils.verify(password, schema.getPasswordSalt(), schema.getHashedPassword());
        if (!isValid) {
            throw new AuthenticationException("Sai mật khẩu");
        }

        // Nếu password lẫn username pass thì tạo đối tượng user mới trong domain và tạo session mới.
        User user = switch (schema.getRole()) {
            case BIDDER -> new Bidder(schema.getId(), schema.getUsername());
            case SELLER -> new Seller(schema.getId(), schema.getUsername());
            case ADMIN  -> new Admin(schema.getId(), schema.getUsername());
        };
        String token = sessionManager.createSession(user);

        UserDTO dto = new UserDTO(user.getId(), user.getUsername(), user.getRole());
        LoginResponseDTO response = new LoginResponseDTO(token, dto);

        return response;
    }

    public void register(String username, String password, String email, UserRole role) {
        if (role == UserRole.ADMIN) {
            throw new IllegalArgumentException("Không được phép đăng ký tài khoản ADMIN");
        }

        // Kiểm tra validation input phía server
        ValidationUtils.validateUsername(username);
        ValidationUtils.validatePassword(password);
        if (email != null) {
            ValidationUtils.validateEmail(email);
        }

        // Kiểm tra xem username đã tồn tại chưa.
        UserSchema existingSchema = userDAO.findByUsername(username);
        if (existingSchema != null) {
            throw new DuplicateUserException(username);
        }

        // Tạo user mới
        String salt = PasswordUtils.generateSalt();
        String hashedPassword = PasswordUtils.hash(password, salt);

        // Khởi tạo Schema với các thông tin mặc định (id, thời gian tạo)
        String id = IdGenerator.generate();
        LocalDateTime now = LocalDateTime.now();
        UserSchema newSchema = new UserSchema(id, now, now, username, hashedPassword, salt, email, role);
        userDAO.save(newSchema);
    }
}
