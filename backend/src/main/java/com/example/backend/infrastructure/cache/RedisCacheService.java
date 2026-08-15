package com.example.backend.infrastructure.cache;

import com.example.backend.infrastructure.config.AppRedisProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class RedisCacheService {
    private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);
    private static final String NULL_VALUE = "__NULL__";

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectMapper objectMapper;
    private final AppRedisProperties properties;

    public <T> Optional<T> get(String key, Class<T> type) {
        StringRedisTemplate redis = redis();
        if (redis == null) return Optional.empty();
        try {
            String value = redis.opsForValue().get(key);
            if (value == null || NULL_VALUE.equals(value)) return Optional.empty();
            return Optional.ofNullable(objectMapper.readValue(value, type));
        } catch (Exception e) {
            log.warn("Redis cache read failed for {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    public boolean hasNull(String key) {
        StringRedisTemplate redis = redis();
        if (redis == null) return false;
        try {
            return NULL_VALUE.equals(redis.opsForValue().get(key));
        } catch (Exception e) {
            log.warn("Redis null cache read failed for {}: {}", key, e.getMessage());
            return false;
        }
    }

    public void set(String key, Object value, Duration ttl) {
        StringRedisTemplate redis = redis();
        if (redis == null || value == null) return;
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), withJitter(ttl));
        } catch (Exception e) {
            log.warn("Redis cache write failed for {}: {}", key, e.getMessage());
        }
    }

    public void setNull(String key) {
        StringRedisTemplate redis = redis();
        if (redis == null) return;
        try {
            redis.opsForValue().set(key, NULL_VALUE, properties.getNullTtl());
        } catch (Exception e) {
            log.warn("Redis null cache write failed for {}: {}", key, e.getMessage());
        }
    }

    public void evict(String... keys) {
        StringRedisTemplate redis = redis();
        if (redis == null || keys == null) return;
        for (String key : keys) {
            if (key == null || key.isBlank()) continue;
            try {
                redis.delete(key);
            } catch (Exception e) {
                log.warn("Redis cache evict failed for {}: {}", key, e.getMessage());
            }
        }
    }

    public Duration detailTtl() {
        return properties.getDetailTtl();
    }

    private Duration withJitter(Duration ttl) {
        Duration base = ttl == null || ttl.isNegative() || ttl.isZero() ? properties.getCacheTtl() : ttl;
        Duration jitter = properties.getTtlJitter();
        if (jitter == null || jitter.isZero() || jitter.isNegative()) return base;
        long jitterSeconds = ThreadLocalRandom.current().nextLong(jitter.toSeconds() + 1);
        return base.plusSeconds(jitterSeconds);
    }

    private StringRedisTemplate redis() {
        if (!properties.isEnabled()) return null;
        return redisTemplateProvider.getIfAvailable();
    }
}