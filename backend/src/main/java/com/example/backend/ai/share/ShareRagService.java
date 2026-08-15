package com.example.backend.ai.share;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.example.backend.ai.config.AiProperties;
import com.example.backend.share.model.Share;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.common.exception.ErrorCode;
import com.example.backend.share.mapper.ShareMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShareRagService {
    private static final Logger log = LoggerFactory.getLogger(ShareRagService.class);

    private final ObjectProvider<ElasticsearchClient> elasticsearchClientProvider;
    private final AiProperties properties;
    private final ShareMapper shareMapper;
    private final ShareContentReader contentReader;
    private final DeepSeekClient deepSeekClient;

    public int reindex(Long shareId) {
        Share share = shareMapper.getById(shareId);
        if (share == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Share not found");
        }
        String text = contentReader.readText(share);
        if (!StringUtils.hasText(text)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Share content is required");
        }

        List<String> chunks = chunkMarkdown(text);
        ElasticsearchClient es = elasticsearchClientProvider.getIfAvailable();
        if (es == null) return chunks.size();

        try {
            boolean exists = es.indices().exists(e -> e.index(properties.getRag().getIndexName())).value();
            if (!exists) es.indices().create(c -> c.index(properties.getRag().getIndexName()));
            es.deleteByQuery(d -> d.index(properties.getRag().getIndexName())
                    .query(q -> q.term(t -> t.field("shareId").value(String.valueOf(shareId)))));

            String contentSha = sha256Hex(text);
            for (int i = 0; i < chunks.size(); i++) {
                Map<String, Object> doc = new HashMap<>();
                doc.put("shareId", String.valueOf(shareId));
                doc.put("chunkId", shareId + "#" + i);
                doc.put("title", share.getTitle());
                doc.put("authorId", share.getAuthorId());
                doc.put("textUrl", share.getTextUrl());
                doc.put("contentSha256", contentSha);
                doc.put("position", i);
                doc.put("text", chunks.get(i));
                String docId = shareId + ":" + i;
                es.index(idx -> idx.index(properties.getRag().getIndexName()).id(docId).document(doc).refresh(Refresh.WaitFor));
            }
        } catch (Exception e) {
            log.warn("Share RAG index failed for {}: {}", shareId, e.getMessage());
        }
        return chunks.size();
    }

    public String answer(Long shareId, String question, Integer topK, Integer maxTokens) {
        if (!StringUtils.hasText(question)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Question is required");
        }
        Share share = shareMapper.getById(shareId);
        if (share == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Share not found");
        }
        if (!properties.isEnabled()) return "AI is disabled. Set ai.enabled=true and configure DEEPSEEK_API_KEY.";

        int limit = Math.max(1, topK == null ? properties.getRag().getTopK() : topK);
        reindex(shareId);
        List<String> contexts = searchContexts(shareId, question, limit);
        if (contexts.isEmpty()) {
            String text = contentReader.readText(share);
            contexts = chunkMarkdown(text).stream().limit(limit).toList();
        }

        String system = "Answer only based on the provided article context. If unsure, say you cannot determine from the article.";
        String user = "Question:\\n" + question
                + "\\n\\nArticle context:\\n" + String.join("\\n\\n---\\n\\n", contexts)
                + "\\n\\nPlease answer in Chinese. Do not use information outside the article context.";
        return deepSeekClient.chat(system, user, 0.2, maxTokens == null ? properties.getRag().getMaxTokens() : maxTokens);
    }

    @SuppressWarnings("unchecked")
    private List<String> searchContexts(Long shareId, String question, int topK) {
        ElasticsearchClient es = elasticsearchClientProvider.getIfAvailable();
        if (es == null) return Collections.emptyList();
        try {
            SearchResponse<Map<String, Object>> resp = es.search(s -> s
                            .index(properties.getRag().getIndexName())
                            .size(Math.max(1, Math.min(topK, 10)))
                            .query(q -> q.bool(b -> b
                                    .must(m -> m.multiMatch(mm -> mm.query(question).fields("title^2", "text")))
                                    .filter(f -> f.term(t -> t.field("shareId").value(String.valueOf(shareId))))
                            )),
                    (Class<Map<String, Object>>) (Class<?>) Map.class);

            List<String> out = new ArrayList<>();
            for (Hit<Map<String, Object>> hit : resp.hits().hits()) {
                Object text = hit.source() == null ? null : hit.source().get("text");
                if (text != null) out.add(String.valueOf(text));
            }
            return out;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String sha256Hex(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "SHA-256 digest failed");
        }
    }

    private List<String> chunkMarkdown(String text) {
        List<String> sections = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        for (String line : text.split("\\R")) {
            if (line.startsWith("#") && !buffer.isEmpty()) {
                sections.add(buffer.toString());
                buffer.setLength(0);
            }
            buffer.append(line).append('\n');
        }
        if (!buffer.isEmpty()) sections.add(buffer.toString());

        List<String> chunks = new ArrayList<>();
        for (String section : sections) {
            String value = section.trim();
            if (value.isEmpty()) continue;
            if (value.length() <= 800) {
                chunks.add(value);
            } else {
                int start = 0;
                while (start < value.length()) {
                    int end = Math.min(start + 800, value.length());
                    chunks.add(value.substring(start, end));
                    if (end >= value.length()) break;
                    start = Math.max(start + 1, end - 100);
                }
            }
        }
        return chunks;
    }
}


