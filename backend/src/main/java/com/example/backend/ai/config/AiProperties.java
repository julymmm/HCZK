package com.example.backend.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * AI 业务开关和 RAG 参数。
 *
 * <p>模型供应商、API Key、base-url、模型名使用 Spring AI 标准 spring.ai.openai.* 配置。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {
    private boolean enabled = false;
    private final Summary summary = new Summary();
    private final Rag rag = new Rag();

    @Data
    public static class Summary {
        private Duration cacheTtl = Duration.ofDays(7);
    }

    @Data
    public static class Rag {
        private String indexName = "hczk_share_rag_index";
        private int topK = 5;
        private int maxTokens = 1024;
    }
}