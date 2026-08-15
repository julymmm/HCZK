package com.example.backend.tool.service.impl;

import com.example.backend.infrastructure.cache.RedisCacheService;
import com.example.backend.infrastructure.cache.RedisKeys;
import com.example.backend.common.response.PageResponse;
import com.example.backend.tool.model.Tool;
import com.example.backend.tool.mapper.ToolMapper;
import com.example.backend.tool.service.ToolService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ToolServiceImpl implements ToolService {

    private final ToolMapper toolMapper;
    private final RedisCacheService redisCacheService;
    private final RedisKeys redisKeys;

    @Override
    public PageResponse<Tool> list(int page, int size, String category, String search) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        size = Math.min(size, 100);
        int offset = (page - 1) * size;
        String normalizedCategory = normalizeFilter(category);
        List<Tool> data = toolMapper.list(offset, size, normalizedCategory, search);
        long total = toolMapper.count(normalizedCategory, search);
        return new PageResponse<>(data, total, page, size);
    }

    @Override
    public Tool getById(Long id, boolean increaseEyeCount) {
        String cacheKey = redisKeys.toolDetail(id);
        if (increaseEyeCount) {
            toolMapper.incrementEyeCount(id);
            redisCacheService.evict(cacheKey);
            return toolMapper.findById(id);
        }

        if (redisCacheService.hasNull(cacheKey)) {
            return null;
        }
        Tool cached = redisCacheService.get(cacheKey, Tool.class).orElse(null);
        if (cached != null) {
            return cached;
        }

        Tool tool = toolMapper.findById(id);
        if (tool == null) {
            redisCacheService.setNull(cacheKey);
        } else {
            redisCacheService.set(cacheKey, tool, redisCacheService.detailTtl());
        }
        return tool;
    }

    private String normalizeFilter(String value) {
        if (!StringUtils.hasText(value)
                || "all".equalsIgnoreCase(value)
                || "全部".equals(value)) {
            return null;
        }
        return value;
    }
}
