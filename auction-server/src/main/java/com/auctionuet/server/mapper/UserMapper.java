package com.auctionuet.server.mapper;

import com.auctionuet.server.domain.enums.UserRole;
import com.auctionuet.server.domain.model.*;
import com.auctionuet.server.network.dto.UserDTO;
import com.auctionuet.server.persistence.schema.UserSchema;
import com.auctionuet.server.util.IdGenerator;

import java.time.LocalDateTime;

public class UserMapper {
    private UserMapper() {}

    public static User toDomain(UserSchema schema) {
        String id = schema.getId();
        String username = schema.getUsername();

        // Kiểm tra loại user để mapping từ schema sang User trong domain
        return switch (schema.getRole()) {
            case BIDDER -> new Bidder(id, username);
            case SELLER -> new Seller(id, username);
            case ADMIN  -> new Admin(id, username);
        };
    }

    // Mapping từ User sang userDTO
    public static UserDTO toDTO(User user) {
        return new UserDTO(user.getId(), user.getUsername(), user.getRole());
    }

    // Tạo new userSchema
    public static UserSchema toNewSchema(String username, String hashedPassword,
                                         String salt, String email, UserRole role) {
        String id = IdGenerator.generate();
        LocalDateTime now = LocalDateTime.now();
        return new UserSchema(id, now, now, username, hashedPassword, salt, email, role);
    }
}

