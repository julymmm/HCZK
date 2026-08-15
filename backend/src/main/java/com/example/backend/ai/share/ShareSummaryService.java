package com.example.backend.ai.share;

import com.example.backend.ai.config.AiProperties;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.common.exception.ErrorCode;
import com.example.backend.share.model.Share;
import com.example.backend.infrastructure.cache.RedisKeys;
import com.example.backend.share.mapper.ShareMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShareSummaryService {
    private final ShareMapper shareMapper;
    private final ShareContentReader contentReader;
    private final DeepSeekClient deepSeekClient;
    private final AiProperties properties;
    private final ObjectProvider<StringRedisTemplate> redisProvider;
    private final RedisKeys redisKeys;

    public Map<String, Object> suggest(String content) {
        return Map.of("summary", generate(content));
    }

    public Map<String, Object> summaryForShare(Long id) {
        Share share = shareMapper.getById(id);
        if (share == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Share not found");
        }
        if (StringUtils.hasText(share.getAiSummary())) {
            return Map.of("summary", share.getAiSummary(), "cached", true);
        }

        String redisKey = redisKeys.aiShareSummary(id);
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis != null) {
            String cached = redis.opsForValue().get(redisKey);
            if (StringUtils.hasText(cached)) {
                return Map.of("summary", cached, "cached", true);
            }
        }

        String summary = generate(contentReader.readText(share));
        shareMapper.updateAiSummary(id, summary);
        if (redis != null) {
            redis.opsForValue().set(redisKey, summary, properties.getSummary().getCacheTtl());
        }
        return Map.of("summary", summary, "cached", false);
    }

    private String generate(String content) {
        String text = content == null ? "" : content.trim();
        if (!StringUtils.hasText(text)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Content is required");
        }
        if (!properties.isEnabled()) {
            return postProcess(extractiveSummary(text));
        }

        String system = "You generate concise Chinese summaries. Return only the summary, no explanation, within 50 Chinese characters.";
        String user = "Article content:\n" + text;
        return postProcess(deepSeekClient.chat(system, user, 0.5, 160));
    }

    private String extractiveSummary(String text) {
        String clean = text.replaceAll("[#>*`_\\-]", " ").replaceAll("\\s+", " ").trim();
        return clean.length() <= 50 ? clean : clean.substring(0, 50);
    }

    private String postProcess(String text) {
        String normalized = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFKC)
                .replaceAll("\\r\\n|\\r|\\n", " ")
                .replaceAll("\\s+", " ")
                .replaceAll("^[\\\"']+|[\\\"']+$", "")
                .trim();
        if (normalized.codePointCount(0, normalized.length()) <= 50) {
            return normalized;
        }
        int end = normalized.offsetByCodePoints(0, 50);
        return normalized.substring(0, end);
    }
}