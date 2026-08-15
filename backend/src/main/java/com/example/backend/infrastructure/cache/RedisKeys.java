package com.example.backend.infrastructure.cache;

import com.example.backend.infrastructure.config.AppRedisProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Redis key 统一生成器。
 *
 * <p>所有 key 都带 app.redis.key-prefix 前缀，默认是 hczk，避免多个项目共用同一个 Redis 时互相污染。</p>
 */
@Component
@RequiredArgsConstructor
public class RedisKeys {
    private final AppRedisProperties properties;

    public String authCode(String scene, String identifier) {
        return key("auth", "code", scene, identifier);
    }

    public String authCodeSent(String scene, String identifier) {
        return key("auth", "code", "sent", scene, identifier);
    }

    public String authCodeDaily(String scene, String identifier) {
        return key("auth", "code", "daily", scene, identifier);
    }

    public String refreshToken(Long userId, String tokenId) {
        return key("auth", "rt", userId, tokenId);
    }

    public String refreshTokenPattern(Long userId) {
        return key("auth", "rt", userId, "*");
    }

    public String aiShareSummary(Long shareId) {
        return key("ai", "share", "summary", shareId);
    }

    public String shareDetail(Long shareId) {
        return key("share", "detail", shareId);
    }

    public String resourceDetail(Long resourceId) {
        return key("resource", "detail", resourceId);
    }

    public String projectDetail(Long projectId) {
        return key("project", "detail", projectId);
    }

    public String articleDetail(Long articleId) {
        return key("article", "detail", articleId);
    }

    public String toolDetail(Long toolId) {
        return key("tool", "detail", toolId);
    }

    public String announcementDetail(Long announcementId) {
        return key("announcement", "detail", announcementId);
    }

    private String key(Object... parts) {
        StringBuilder builder = new StringBuilder(properties.getKeyPrefix());
        for (Object part : parts) {
            if (part == null) continue;
            String value = String.valueOf(part).trim();
            if (value.isEmpty()) continue;
            builder.append(':').append(value.replaceAll("\\s+", "_"));
        }
        return builder.toString();
    }
}