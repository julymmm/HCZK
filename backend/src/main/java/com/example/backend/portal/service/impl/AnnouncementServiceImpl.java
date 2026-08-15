package com.example.backend.portal.service.impl;

import com.example.backend.infrastructure.cache.RedisCacheService;
import com.example.backend.infrastructure.cache.RedisKeys;
import com.example.backend.portal.model.Announcement;
import com.example.backend.portal.mapper.AnnouncementMapper;
import com.example.backend.portal.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementMapper announcementMapper;
    private final RedisCacheService redisCacheService;
    private final RedisKeys redisKeys;

    @Override
    public List<Announcement> listAll(int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        size = Math.min(size, 100);
        int offset = (page - 1) * size;
        return announcementMapper.list(offset, size);
    }

    @Override
    public List<Announcement> latest(int limit) {
        if (limit <= 0) limit = 2;
        return announcementMapper.latest(limit);
    }

    @Override
    public Announcement getById(Long id, boolean increaseView) {
        String cacheKey = redisKeys.announcementDetail(id);
        if (increaseView) {
            announcementMapper.incrementViewCount(id);
            redisCacheService.evict(cacheKey);
            return announcementMapper.findById(id);
        }

        if (redisCacheService.hasNull(cacheKey)) {
            return null;
        }
        Announcement cached = redisCacheService.get(cacheKey, Announcement.class).orElse(null);
        if (cached != null) {
            return cached;
        }

        Announcement announcement = announcementMapper.findById(id);
        if (announcement == null) {
            redisCacheService.setNull(cacheKey);
        } else {
            redisCacheService.set(cacheKey, announcement, redisCacheService.detailTtl());
        }
        return announcement;
    }
}
