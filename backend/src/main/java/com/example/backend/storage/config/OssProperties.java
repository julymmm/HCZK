package com.example.backend.storage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oss")
public class OssProperties {
    private String endpoint;
    private String bucket;
    private String accessKeyId;
    private String accessKeySecret;
    private String publicDomain;
    private String folder = "hczk";
    private boolean enabled = false;

    public boolean configured() {
        return enabled
                && hasText(endpoint)
                && hasText(bucket)
                && hasText(accessKeyId)
                && hasText(accessKeySecret);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}