package com.example.backend.auth.verification;

/** 验证码校验结果。 */
public enum VerificationCodeStatus {
    SUCCESS,
    NOT_FOUND,
    MISMATCH,
    TOO_MANY_ATTEMPTS
}