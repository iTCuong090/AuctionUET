package com.auctionuet.protocol.dto.response.auth;

import com.auctionuet.protocol.dto.ValidatableDTO;
import com.auctionuet.protocol.dto.response.user.UserDTO;

public class LoginResponseDTO implements ValidatableDTO {
    private final String token;
    private final UserDTO user;
    private final boolean mustChangePassword;

    public LoginResponseDTO(String token, UserDTO user) {
        this(token, user, false);
    }

    public LoginResponseDTO(String token, UserDTO user, boolean mustChangePassword) {
        this.token = token;
        this.user = user;
        this.mustChangePassword = mustChangePassword;
        validate();
    }

    @Override
    public void validate() {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token must not be blank");
        }
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        user.validate();
    }

    public String getToken() { return token; }
    public UserDTO getUser() { return user; }
    public boolean isMustChangePassword() { return mustChangePassword; }
}
