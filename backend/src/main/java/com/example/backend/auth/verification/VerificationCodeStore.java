package com.example.backend.auth.verification;

import java.time.Duration;

/**
 * 验证码存储接口。
 *
 * <p>这里只描述验证码如何保存、校验和失效；当前项目只有 Redis 实现，不提供内存兜底。</p>
 */
public interface VerificationCodeStore {
    void saveCode(String scene, String identifier, String code, Duration ttl, int maxAttempts);

    VerificationCheckResult verify(String scene, String identifier, String code);

    void invalidate(String scene, String identifier);
}