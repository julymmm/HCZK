package com.example.backend.search.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "search")
public class SearchProperties {
    private boolean enabled = true;
    private final Elasticsearch elasticsearch = new Elasticsearch();
    private final Index index = new Index();

    @Data
    public static class Elasticsearch {
        private String uris = "http://localhost:9200";
        private String username = "";
        private String password = "";
    }

    @Data
    public static class Index {
        private String content = "hczk_content_index";
    }
}
