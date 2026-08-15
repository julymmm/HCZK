package com.example.backend.resource.service.impl;

import com.example.backend.infrastructure.cache.RedisCacheService;
import com.example.backend.infrastructure.cache.RedisKeys;
import com.example.backend.resource.dto.ResourceDtos;
import com.example.backend.resource.model.Resource;
import com.example.backend.resource.mapper.ResourceMapper;
import com.example.backend.resource.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {
    private final ResourceMapper resourceMapper;
    private final RedisCacheService redisCacheService;
    private final RedisKeys redisKeys;

    @Override
    public List<Resource> listAll(int page, int size, String category, String type, String search, List<String> tags, String source) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        size = Math.min(size, 100);
        int offset = (page - 1) * size;
        return resourceMapper.list(offset, size, normalizeAll(category), normalizeAll(type), search, tags, normalizeAll(source));
    }

    @Override
    public long count(String category, String type, String search, List<String> tags, String source) {
        return resourceMapper.count(normalizeAll(category), normalizeAll(type), search, tags, normalizeAll(source));
    }

    @Override
    public Resource getById(Long id, boolean increaseView) {
        if (id == null) return null;
        String key = redisKeys.resourceDetail(id);
        if (increaseView) {
            resourceMapper.incrementEyeCount(id);
            redisCacheService.evict(key);
        } else {
            if (redisCacheService.hasNull(key)) return null;
            Resource cached = redisCacheService.get(key, Resource.class).orElse(null);
            if (cached != null) return cached;
        }
        Resource resource = resourceMapper.findById(id);
        if (resource == null) redisCacheService.setNull(key);
        else redisCacheService.set(key, resource, redisCacheService.detailTtl());
        return resource;
    }

    @Override
    public List<ResourceDtos.ResourceListResp> listAllAsDto(int page, int size, String category, String type, String search, List<String> tags, String source) {
        List<Resource> list = listAll(page, size, category, type, search, tags, source);
        List<ResourceDtos.ResourceListResp> result = new ArrayList<>(list.size());
        for (Resource resource : list) {
            result.add(toListResp(resource));
        }
        return result;
    }

    @Override
    public ResourceDtos.ResourceDetailResp getDetailDto(Long id, boolean increaseView) {
        return toDetailResp(getById(id, increaseView));
    }

    @Override
    public ResourceDtos.ResourceDetailResp toDetailResp(Resource resource) {
        return resource == null ? null : copyToDetailResp(resource);
    }

    @Override
    public Integer testDynamicCountFixed(String category) {
        return resourceMapper.testDynamicCountFixed(category);
    }

    @Override
    public List<Map<String, Object>> getDbData() {
        return resourceMapper.getDbData();
    }

    private static ResourceDtos.ResourceListResp toListResp(Resource r) {
        ResourceDtos.ResourceListResp dto = new ResourceDtos.ResourceListResp();
        dto.setId(r.getId());
        dto.setTitle(r.getTitle());
        dto.setDescription(r.getDescription());
        dto.setCategory(r.getCategory());
        dto.setType(r.getType());
        dto.setResourceUrl(r.getResourceUrl());
        dto.setEyeCount(r.getEyeCount());
        dto.setHic(r.getHic());
        dto.setSource(r.getSource());
        dto.setCreatedAt(r.getCreatedAt());
        dto.setTags(tagsFromString(r.getTags()));
        return dto;
    }

    private static ResourceDtos.ResourceDetailResp copyToDetailResp(Resource r) {
        ResourceDtos.ResourceDetailResp dto = new ResourceDtos.ResourceDetailResp();
        dto.setId(r.getId());
        dto.setTitle(r.getTitle());
        dto.setDescription(r.getDescription());
        dto.setCategory(r.getCategory());
        dto.setType(r.getType());
        dto.setResourceUrl(r.getResourceUrl());
        dto.setEyeCount(r.getEyeCount());
        dto.setHic(r.getHic());
        dto.setSource(r.getSource());
        dto.setCreatedAt(r.getCreatedAt());
        dto.setTags(tagsFromString(r.getTags()));
        dto.setContentUrl(r.getContentUrl());
        return dto;
    }

    private static List<String> tagsFromString(String tagsStr) {
        if (tagsStr == null || tagsStr.trim().isEmpty()) return Collections.emptyList();
        return Arrays.stream(tagsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private static String normalizeAll(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "all".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return trimmed;
    }
}
