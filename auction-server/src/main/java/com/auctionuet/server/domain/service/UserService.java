package com.auctionuet.server.domain.service;

import com.auctionuet.protocol.dto.response.user.UserDTO;
import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.exception.UserNotFoundException;
import com.auctionuet.server.persistence.dao.UserDAO;
import com.auctionuet.server.persistence.schema.UserSchema;

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
}

