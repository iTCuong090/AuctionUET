package com.auctionuet.protocol.dto.request.user;

import com.auctionuet.protocol.dto.ValidatableDTO;

public class UpdateProfileRequestDTO implements ValidatableDTO {
    private String username;
    private String currentPassword;
    private String newPassword;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    @Override
    public void validate() {
        boolean hasUsername = username != null && !username.isBlank();
        boolean hasCurrentPassword = currentPassword != null && !currentPassword.isBlank();
        boolean hasNewPassword = newPassword != null && !newPassword.isBlank();

        if (!hasUsername && !hasCurrentPassword && !hasNewPassword) {
            throw new IllegalArgumentException("Không có thông tin profile cần cập nhật");
        }
        if (hasCurrentPassword != hasNewPassword) {
            throw new IllegalArgumentException("Cần nhập đủ mật khẩu hiện tại và mật khẩu mới");
        }
    }
}
