package com.example.backend.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 认证模块配置项，对应 application.properties 中的 app.auth.*。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {
    private final Verification verification = new Verification();
    private final Password password = new Password();

    @Data
    public static class Verification {
        /** 验证码位数，默认 6 位数字。 */
        private int codeLength = 6;
        /** 验证码有效期，默认 5 分钟。 */
        private Duration ttl = Duration.ofMinutes(5);
        /** 单个验证码最多允许尝试次数。 */
        private int maxAttempts = 5;
        /** 同一场景、同一账号重复发送验证码的最小间隔。 */
        private Duration sendInterval = Duration.ofSeconds(60);
        /** 同一场景、同一账号每日最多发送次数。 */
        private int dailyLimit = 10;
        /** 本地调试时是否把验证码返回给前端；生产环境必须关闭。 */
        private boolean exposeCodeInResponse = true;
    }

    @Data
    public static class Password {
        /** 密码最小长度。 */
        private int minLength = 8;
    }
}