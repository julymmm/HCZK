package com.example.backend.auth.model;

import com.example.backend.common.exception.BusinessException;
import com.example.backend.common.exception.ErrorCode;

public enum IdentifierType {
    PHONE,
    EMAIL;

    /**
     * Accepts frontend aliases while keeping auth identifiers limited to phone or email.
     */
    public static IdentifierType from(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Account identifier type is required");
        }
        return switch (value.trim().toLowerCase()) {
            case "phone", "mobile" -> PHONE;
            case "email" -> EMAIL;
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "Only phone or email login is supported");
        };
    }
}