package com.example.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDtos {
    @Data
    public static class ProfileResp {
        private Long id;
        private String username;
        private String nickname;
        private String email;
        private String avatarUrl;
        private String college;
        private String bio;
        private Integer status;
        private String role;
        private Integer hic;
        private LocalDateTime lastLoginTime;
        private LocalDateTime createdAt;
    }

    @Data
    public static class UpdateProfileReq {
        private String nickname;
        private String email;
        private String avatarUrl;
        private String college;
        private String bio;
        private Integer hic;
    }

    @Data
    public static class ChangePasswordReq {
        @NotBlank(message = "Old password is required")
        @Size(min = 1, max = 128, message = "Old password is too long")
        private String oldPassword;

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 128, message = "New password length must be 8-128")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$", message = "Password is too weak")
        private String newPassword;
    }
}
