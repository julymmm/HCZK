package com.example.backend.infrastructure.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * API 限流配置。
 */
@Configuration
public class RateLimitConfig implements WebMvcConfigurer {

    @Bean
    public RateLimitInterceptor rateLimitInterceptor() {
        return new RateLimitInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login", "/api/auth/register");
    }

    public static class RateLimitInterceptor implements HandlerInterceptor {
        private final ConcurrentHashMap<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

        private static final int GENERAL_LIMIT = 100;
        private static final Duration GENERAL_WINDOW = Duration.ofMinutes(1);
        private static final int AUTH_LIMIT = 5;
        private static final Duration AUTH_WINDOW = Duration.ofMinutes(1);

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            String clientIp = getClientIpAddress(request);
            String uri = request.getRequestURI();

            int limit = GENERAL_LIMIT;
            Duration window = GENERAL_WINDOW;
            if (uri.startsWith("/api/auth/")) {
                limit = AUTH_LIMIT;
                window = AUTH_WINDOW;
            }

            if (!isAllowed(clientIp + ":" + getCategory(uri), limit, window)) {
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
                return false;
            }
            return true;
        }

        private boolean isAllowed(String key, int limit, Duration window) {
            long now = System.currentTimeMillis();
            RequestCounter counter = requestCounts.computeIfAbsent(key, k -> new RequestCounter());
            synchronized (counter) {
                if (now - counter.windowStart > window.toMillis()) {
                    counter.count.set(0);
                    counter.windowStart = now;
                }
                if (counter.count.get() >= limit) {
                    return false;
                }
                counter.count.incrementAndGet();
                return true;
            }
        }

        private String getCategory(String uri) {
            return uri.startsWith("/api/auth/") ? "auth" : "general";
        }

        private String getClientIpAddress(HttpServletRequest request) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
                return xForwardedFor.split(",")[0].trim();
            }
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
                return xRealIp;
            }
            return request.getRemoteAddr();
        }

        private static class RequestCounter {
            private final AtomicInteger count = new AtomicInteger(0);
            private volatile long windowStart = System.currentTimeMillis();
        }
    }
}