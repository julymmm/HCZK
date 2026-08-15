package com.example.backend.auth.verification;

/**
 * 验证码校验详情，包含结果状态和已尝试次数。
 */
public record VerificationCheckResult(VerificationCodeStatus status, int attempts, int maxAttempts) {
    public boolean success() {
        return status == VerificationCodeStatus.SUCCESS;
    }
}