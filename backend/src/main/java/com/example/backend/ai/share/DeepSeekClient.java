package com.example.backend.ai.share;

import com.example.backend.ai.config.AiProperties;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class DeepSeekClient {
    private final AiProperties properties;
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

    public String chat(String system, String user, double temperature, int maxTokens) {
        if (!properties.isEnabled()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI service is not enabled");
        }
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Spring AI ChatClient is not configured");
        }
        try {
            String content = builder.build()
                    .prompt()
                    .system(system)
                    .user(user)
                    .options(OpenAiChatOptions.builder()
                            .temperature(temperature)
                            .maxTokens(maxTokens)
                            .build())
                    .call()
                    .content();
            if (!StringUtils.hasText(content)) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI response is empty");
            }
            return content;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Spring AI service call failed: " + e.getMessage());
        }
    }
}