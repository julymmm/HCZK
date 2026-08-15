package com.example.backend.ai.share;

import com.example.backend.share.model.Share;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class ShareContentReader {
    private final RestTemplate restTemplate;

    public String readText(Share share) {
        if (share == null) return "";
        String textUrl = share.getTextUrl();
        if (StringUtils.hasText(textUrl) && isHttpUrl(textUrl)) {
            try {
                String content = restTemplate.getForObject(textUrl, String.class);
                if (StringUtils.hasText(content)) return content;
            } catch (Exception ignored) {
                // OSS 内容读取失败时退回数据库摘要内容，避免 AI 流程阻断主业务。
            }
        }
        return share.getContent() == null ? "" : share.getContent();
    }

    private static boolean isHttpUrl(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }
}
