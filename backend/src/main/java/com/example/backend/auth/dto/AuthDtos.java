package com.example.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthDtos {
    @Data
    public static class RegisterReq {
        private String identifierType = "EMAIL";

        @Size(max = 100, message = "Identifier is too long")
        private String identifier;

        @NotBlank(message = "Verification code is required")
        @Size(min = 4, max = 10, message = "Invalid code length")
        private String code;

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 20, message = "Username length must be 3-20")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers and underscore")
        private String username;

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid phone number")
        private String phone;

        @NotBlank(message = "Email is required")
        @Size(max = 100, message = "Email is too long")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 128, message = "Password length must be 6-128")
        private String password;

        @Size(max = 50, message = "Nickname is too long")
        private String nickname;

        @NotBlank(message = "College is required")
        private String college;

        @NotBlank(message = "Student id is required")
        @Size(min = 11, max = 11, message = "Student id length must be 11")
        private String studentId;
    }

    @Data
    public static class LoginReq {
        @NotBlank(message = "Identifier type is required")
        private String identifierType = "EMAIL";

        @NotBlank(message = "Identifier is required")
        @Size(max = 100, message = "Identifier is too long")
        private String identifier;

        @Size(max = 128, message = "Password is too long")
        private String password;

        @Size(min = 4, max = 10, message = "Invalid code length")
        private String code;
    }

    @Data
    public static class SendCodeReq {
        private String scene;

        @NotBlank(message = "Identifier type is required")
        private String identifierType = "EMAIL";

        @NotBlank(message = "Identifier is required")
        @Size(max = 100, message = "Identifier is too long")
        private String identifier;
    }

    @Data
    public static class ResetPasswordReq {
        @NotBlank(message = "Identifier type is required")
        private String identifierType = "EMAIL";

        @NotBlank(message = "Identifier is required")
        @Size(max = 100, message = "Identifier is too long")
        private String identifier;

        @NotBlank(message = "Code is required")
        @Size(min = 4, max = 10, message = "Invalid code length")
        private String code;

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 128, message = "New password length must be 8-128")
        private String newPassword;
    }

    @Data
    public static class TokenRefreshReq {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }

    @Data
    public static class LogoutReq {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }

    @Data
    public static class HicVerifyReq {
        @NotBlank(message = "Auth key is required")
        @Size(min = 1, max = 256, message = "Invalid auth key length")
        private String key;
    }
}