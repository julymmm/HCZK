package com.example.backend.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "app.redis")
public class AppRedisProperties {
    private boolean enabled = false;
    private String keyPrefix = "hczk";
    private Duration cacheTtl = Duration.ofMinutes(30);
    private Duration detailTtl = Duration.ofMinutes(30);
    private Duration nullTtl = Duration.ofSeconds(60);
    private Duration ttlJitter = Duration.ofMinutes(5);
}