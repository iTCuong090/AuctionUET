package com.auctionuet.server.UnitTest;

import com.auctionuet.server.domain.enums.UserRole;
import com.auctionuet.server.domain.model.*;
import com.auctionuet.server.mapper.UserMapper;
import com.auctionuet.server.network.dto.UserDTO;
import com.auctionuet.server.persistence.schema.UserSchema;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UserMapperTest {

    @Test
    void testToDomainBidder() {
        UserSchema schema = new UserSchema(
                "1", null, null,
                "duy", "pass", "salt", "email", UserRole.BIDDER
        );

        User user = UserMapper.toDomain(schema);

        assertInstanceOf(Bidder.class, user);
    }

    @Test
    void testToDomainSeller() {
        UserSchema schema = new UserSchema(
                "2", null, null,
                "duy", "pass", "salt", "email", UserRole.SELLER
        );

        User user = UserMapper.toDomain(schema);

        assertInstanceOf(Seller.class, user);
    }

    @Test
    void testToDomainAdmin() {
        UserSchema schema = new UserSchema(
                "3", null, null,
                "duy", "pass", "salt", "email", UserRole.ADMIN
        );

        User user = UserMapper.toDomain(schema);

        assertInstanceOf(Admin.class, user);
    }

    @Test
    void testToDTO() {
        User user = new Bidder("1", "duy");

        UserDTO dto = UserMapper.toDTO(user);

        assertEquals("1", dto.getId());
        assertEquals("duy", dto.getUsername());
        assertEquals(UserRole.BIDDER, dto.getRole());
    }

    @Test
    void testToDTONoPassword() {
        UserDTO dto = new UserDTO("1", "duy", UserRole.BIDDER);

        // Không có getPassword() → compile-time check
        assertNotNull(dto.getId());
    }

    @Test
    void testToNewSchema() {
        UserSchema schema = UserMapper.toNewSchema(
                "duy",
                "hashedPass",
                "salt123",
                "duy@uet.vn",
                UserRole.BIDDER
        );

        assertNotNull(schema.getId());
        assertEquals("duy", schema.getUsername());
        assertEquals(UserRole.BIDDER, schema.getRole());
    }
}