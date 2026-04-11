package com.auctionuet.server.mapper;

import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.domain.model.Bidder;
import com.auctionuet.server.persistence.schema.UserSchema;
import com.auctionuet.server.domain.enums.UserRole;
import java.time.LocalDateTime;

public class UserMapper {
    private UserMapper() {}

    public static User toDomain(UserSchema schema) {
        if (schema == null) return null;
        return new Bidder(schema.getId(), schema.getUsername());
    }

    public static UserSchema toNewSchema(String username, String hashedPassword, String salt, String email, UserRole role) {
        LocalDateTime now = LocalDateTime.now();
        com.auctionuet.server.persistence.schema.UserRole schemaRole = 
            com.auctionuet.server.persistence.schema.UserRole.valueOf(role.name());
        return new UserSchema(java.util.UUID.randomUUID().toString(), now, now, username, hashedPassword, salt, email, schemaRole);
    }
}
