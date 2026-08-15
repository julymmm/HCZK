package com.example.backend.search.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "search.enabled", havingValue = "true", matchIfMissing = true)
public class ElasticsearchConfig {
    private final SearchProperties properties;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        String uri = properties.getElasticsearch().getUris();
        String firstUri = uri == null || uri.isBlank() ? "http://localhost:9200" : uri.split(",")[0].trim();
        BasicCredentialsProvider credentials = new BasicCredentialsProvider();
        if (StringUtils.hasText(properties.getElasticsearch().getUsername())) {
            credentials.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(
                    properties.getElasticsearch().getUsername(), properties.getElasticsearch().getPassword()));
        }
        RestClientBuilder builder = RestClient.builder(HttpHost.create(firstUri));
        if (StringUtils.hasText(properties.getElasticsearch().getUsername())) {
            builder.setHttpClientConfigCallback(http -> http.setDefaultCredentialsProvider(credentials));
        }
        return new ElasticsearchClient(new RestClientTransport(builder.build(), new JacksonJsonpMapper()));
    }
}
