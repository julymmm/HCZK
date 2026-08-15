package com.example.backend.auth.verification;

import com.example.backend.infrastructure.cache.RedisKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis 验证码频控实现。
 *
 * <p>发送间隔用 setIfAbsent 实现，日发送次数用 Redis 自增计数实现。</p>
 */
@Component
@RequiredArgsConstructor
public class RedisVerificationThrottleStore implements VerificationThrottleStore {
    private final StringRedisTemplate redisTemplate;
    private final RedisKeys redisKeys;

    @Override
    public boolean markSentIfAllowed(String scene, String identifier, Duration interval) {
        if (interval == null || interval.isZero() || interval.isNegative()) return true;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(redisKeys.authCodeSent(scene, identifier), "1", interval);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public long increaseDailyCount(String scene, String identifier, Duration ttl) {
        String key = redisKeys.authCodeDaily(scene, identifier);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) redisTemplate.expire(key, ttl);
        return count == null ? 0L : count;
    }
}