package com.example.backend.auth.verification;

import com.example.backend.common.exception.BusinessException;
import com.example.backend.common.exception.ErrorCode;

/**
 * 验证码使用场景。
 *
 * <p>同一个邮箱或手机号，在注册、登录、找回密码三个场景下使用不同 Redis key，互不串用。</p>
 */
public enum VerificationScene {
    REGISTER,
    LOGIN,
    RESET_PASSWORD;

    public static VerificationScene from(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Verification scene is required");
        }
        try {
            return VerificationScene.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Unsupported verification scene");
        }
    }
}