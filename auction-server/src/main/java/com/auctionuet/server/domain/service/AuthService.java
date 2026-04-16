package com.auctionuet.server.domain.service;

import com.auctionuet.server.domain.enums.UserRole;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.exception.AuthenticationException;
import com.auctionuet.server.exception.DuplicateUserException;
import com.auctionuet.server.exception.UserNotFoundException;
import com.auctionuet.server.mapper.UserMapper;
import com.auctionuet.server.persistence.schema.dao.UserDAO;
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
    //Tại sao constructor lại phải nhận 1 đối tượng loại UserDAO? Vì các đối tượng userDao có thể custom
    //đường dẫn file database phục vụ test. Hơn nữa, sau này nếu ta có thay đổi phía DAO thì truyền vào
    //một đối tượng kiểu xxxDAO kế thừa userdao thì cũng ko cần phải thay đổi code ở authservice.

    public LoginResult login(String username, String password) {
        //Thực hiện truy vấn database cái username.
        UserSchema schema = userDAO.findByUsername(username);

        //Không tìm thấy username thì nhả usernotfound.
        if (schema == null) {
            throw new UserNotFoundException(username);
        }

        //Nếu tìm thấy user thì kiểm tra password.
        boolean isValid = PasswordUtils.verify(password, schema.getPasswordSalt(), schema.getHashedPassword());
        if (!isValid) {
            throw new AuthenticationException("Sai mật khẩu");
        }

        //nếu password lẫn username pass thì tạo đối tượng user mới trong domain và tạo sesion mới.
        User user = UserMapper.toDomain(schema);
        String token = sessionManager.createSession(user);
        return new LoginResult(token, user);
    }

    public void register(String username, String password, String email, UserRole role) {
        //Kiểm tra validation input phía server
        ValidationUtils.validateUsername(username);
        ValidationUtils.validatePassword(password);
        if (email != null) {
            ValidationUtils.validateEmail(email);
        }


        //Kiểm tra xem username đã tồn tại chưa.
        UserSchema existingSchema = userDAO.findByUsername(username);
        if (existingSchema != null) {
            throw new DuplicateUserException(username);
        }


        //Tạo user mới
        String salt = PasswordUtils.generateSalt();
        String hashedPassword = PasswordUtils.hash(password, salt);

        // Sử dụng UserMapper.toNewSchema vì userschema yêu cầu constructor phức tạp, cần thêm cả thời gian tạo, vv.
        UserSchema newSchema = UserMapper.toNewSchema(username, hashedPassword, salt, email, role);

        userDAO.save(newSchema);
    }
}
