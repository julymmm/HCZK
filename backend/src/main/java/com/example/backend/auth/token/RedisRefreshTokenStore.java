package com.example.backend.auth.token;

import com.example.backend.infrastructure.cache.RedisKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

/**
 * 基于 Redis 的 refresh token 白名单实现，结构参考知光项目。
 *
 * <p>Redis 中只保存 refresh token 的令牌 ID，不保存完整 JWT：</p>
 *
 * <pre>
 * hczk:auth:rt:{userId}:{tokenId} -> "1"
 * TTL = refresh token 剩余有效期
 * </pre>
 *
 * <p>这样可以在 JWT 自身未过期时，通过删除 Redis 白名单记录实现即时撤销刷新能力。
 * access token 不进入 Redis，仍然依靠短有效期和 RS256 签名校验保证性能。</p>
 */
@Component
public class RedisRefreshTokenStore implements RefreshTokenStore {
    private final StringRedisTemplate redisTemplate;
    private final RedisKeys redisKeys;

    public RedisRefreshTokenStore(StringRedisTemplate redisTemplate,
                                  RedisKeys redisKeys) {
        this.redisTemplate = redisTemplate;
        this.redisKeys = redisKeys;
    }

    @Override
    public void storeToken(Long userId, String tokenId, Duration ttl) {
        redisTemplate.opsForValue().set(redisKeys.refreshToken(userId, tokenId), "1", ttl);
    }

    @Override
    public boolean isTokenValid(Long userId, String tokenId) {
        String value = redisTemplate.opsForValue().get(redisKeys.refreshToken(userId, tokenId));
        return "1".equals(value);
    }

    @Override
    public void revokeToken(Long userId, String tokenId) {
        redisTemplate.delete(redisKeys.refreshToken(userId, tokenId));
    }

    @Override
    public void revokeAll(Long userId) {
        Set<String> keys = redisTemplate.keys(redisKeys.refreshTokenPattern(userId));
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}