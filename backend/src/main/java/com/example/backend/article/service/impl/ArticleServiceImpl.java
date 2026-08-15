package com.example.backend.article.service.impl;

import com.example.backend.infrastructure.cache.RedisCacheService;
import com.example.backend.infrastructure.cache.RedisKeys;
import com.example.backend.common.response.PageResponse;
import com.example.backend.article.model.Article;
import com.example.backend.article.mapper.ArticleMapper;
import com.example.backend.article.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {
    private static final Logger log = LoggerFactory.getLogger(ArticleServiceImpl.class);

    private final ArticleMapper articleMapper;
    private final RedisCacheService redisCacheService;
    private final RedisKeys redisKeys;

    @Override
    public PageResponse<Article> list(int page, int size, String search, String source) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        search = normalizeParam(search);
        source = normalizeParam(source);
        int offset = (page - 1) * size;
        if (search != null && !search.isEmpty()) {
            log.debug("Article search param: [{}], page={}, size={}, source={}", search, page, size, source);
        }
        List<Article> data = articleMapper.list(offset, size, search, source);
        long total = articleMapper.count(search, source);
        return new PageResponse<>(data, total, page, size);
    }

    @Override
    public Article getById(Long id, boolean increaseViewCount) {
        if (id == null) return null;
        String key = redisKeys.articleDetail(id);
        if (increaseViewCount) {
            articleMapper.incrementViewCount(id);
            redisCacheService.evict(key);
        } else {
            if (redisCacheService.hasNull(key)) return null;
            Article cached = redisCacheService.get(key, Article.class).orElse(null);
            if (cached != null) return cached;
        }
        Article article = articleMapper.findById(id);
        if (article == null) redisCacheService.setNull(key);
        else redisCacheService.set(key, article, redisCacheService.detailTtl());
        return article;
    }

    private static String normalizeParam(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        if (normalized.length() >= 2 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        return normalized.isEmpty() ? null : normalized;
    }
}