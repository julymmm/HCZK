package com.example.backend.auth.verification;

import com.example.backend.infrastructure.cache.RedisKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Redis 验证码存储实现。
 *
 * <p>每个验证码使用一个 Hash 保存：</p>
 * <pre>
 * hczk:auth:code:{scene}:{identifier} -> {
 *   code: 验证码,
 *   maxAttempts: 最大尝试次数,
 *   attempts: 已失败次数
 * }
 * </pre>
 *
 * <p>验证码校验成功后立即删除；失败次数达到上限后保留一段时间，避免暴力尝试。</p>
 */
@Component
@RequiredArgsConstructor
public class RedisVerificationCodeStore implements VerificationCodeStore {
    private static final String FIELD_CODE = "code";
    private static final String FIELD_MAX_ATTEMPTS = "maxAttempts";
    private static final String FIELD_ATTEMPTS = "attempts";

    private final StringRedisTemplate redisTemplate;
    private final RedisKeys redisKeys;

    @Override
    public void saveCode(String scene, String identifier, String code, Duration ttl, int maxAttempts) {
        String key = redisKeys.authCode(scene, identifier);
        HashOperations<String, String, String> ops = redisTemplate.opsForHash();
        try {
            ops.put(key, FIELD_CODE, code);
            ops.put(key, FIELD_MAX_ATTEMPTS, String.valueOf(maxAttempts));
            ops.put(key, FIELD_ATTEMPTS, "0");
            redisTemplate.expire(key, ttl);
        } catch (DataAccessException ex) {
            throw new RedisSystemException("Failed to save verification code", ex);
        }
    }

    @Override
    public VerificationCheckResult verify(String scene, String identifier, String code) {
        String key = redisKeys.authCode(scene, identifier);
        HashOperations<String, String, String> ops = redisTemplate.opsForHash();
        Map<String, String> data = ops.entries(key);
        if (data.isEmpty()) return new VerificationCheckResult(VerificationCodeStatus.NOT_FOUND, 0, 0);

        String storedCode = data.get(FIELD_CODE);
        int maxAttempts = parseInt(data.get(FIELD_MAX_ATTEMPTS), 5);
        int attempts = parseInt(data.get(FIELD_ATTEMPTS), 0);
        if (attempts >= maxAttempts) return new VerificationCheckResult(VerificationCodeStatus.TOO_MANY_ATTEMPTS, attempts, maxAttempts);
        if (Objects.equals(storedCode, code)) {
            redisTemplate.delete(key);
            return new VerificationCheckResult(VerificationCodeStatus.SUCCESS, attempts, maxAttempts);
        }

        Long updated = ops.increment(key, FIELD_ATTEMPTS, 1L);
        int updatedAttempts = updated == null ? attempts + 1 : updated.intValue();
        if (updatedAttempts >= maxAttempts) {
            redisTemplate.expire(key, Duration.ofMinutes(30));
            return new VerificationCheckResult(VerificationCodeStatus.TOO_MANY_ATTEMPTS, updatedAttempts, maxAttempts);
        }
        return new VerificationCheckResult(VerificationCodeStatus.MISMATCH, updatedAttempts, maxAttempts);
    }

    @Override
    public void invalidate(String scene, String identifier) {
        redisTemplate.delete(redisKeys.authCode(scene, identifier));
    }

    private static int parseInt(String value, int defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}