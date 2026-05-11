package com.auctionuet.protocol.dto.response;

public class LoginResponseDTO {
    private final String token;
    private final UserDTO user;

    public LoginResponseDTO(String token, UserDTO user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() { return token; }
    public UserDTO getUser() { return user; }
}
