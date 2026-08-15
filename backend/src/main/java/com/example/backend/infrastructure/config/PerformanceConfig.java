package com.example.backend.infrastructure.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 接口性能监控配置。
 */
@Configuration
@Slf4j
public class PerformanceConfig implements WebMvcConfigurer {

    @Bean
    public PerformanceInterceptor performanceInterceptor() {
        return new PerformanceInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(performanceInterceptor())
                .addPathPatterns("/api/**");
    }

    public static class PerformanceInterceptor implements HandlerInterceptor {
        private static final String START_TIME = "startTime";

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            request.setAttribute(START_TIME, System.currentTimeMillis());
            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
            Long startTime = (Long) request.getAttribute(START_TIME);
            if (startTime != null) {
                long duration = System.currentTimeMillis() - startTime;
                String uri = request.getRequestURI();
                String method = request.getMethod();
                if (duration > 1000) {
                    log.warn("Slow request: {} {} took {}ms", method, uri, duration);
                } else if (duration > 500) {
                    log.info("Request latency: {} {} took {}ms", method, uri, duration);
                }
                response.setHeader("X-Response-Time", duration + "ms");
            }
        }
    }
}