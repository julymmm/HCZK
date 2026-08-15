package com.example.backend.project.service.impl;

import com.example.backend.infrastructure.cache.RedisCacheService;
import com.example.backend.infrastructure.cache.RedisKeys;
import com.example.backend.common.response.PageResponse;
import com.example.backend.project.model.Project;
import com.example.backend.project.mapper.ProjectMapper;
import com.example.backend.search.service.SearchIndexService;
import com.example.backend.project.service.ProjectService;
import com.example.backend.user.service.UserFavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {
    private final ProjectMapper projectMapper;
    private final UserFavoriteService userFavoriteService;
    private final ObjectProvider<SearchIndexService> searchIndexServiceProvider;
    private final RedisCacheService redisCacheService;
    private final RedisKeys redisKeys;

    @Override
    public PageResponse<Project> getProjects(int page, int size, String category, String search) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        size = Math.min(size, 100);
        int offset = (page - 1) * size;
        List<Project> projects = projectMapper.list(offset, size, normalizeAll(category), search);
        long total = projectMapper.count(normalizeAll(category), search);
        for (Project project : projects) {
            long starCount = userFavoriteService.getFavoriteCount(project.getId());
            project.setStarCount((int) starCount);
        }
        return new PageResponse<>(projects, total, page, size);
    }

    @Override
    public Project getProjectById(Long id) {
        if (id == null) return null;
        String key = redisKeys.projectDetail(id);
        if (redisCacheService.hasNull(key)) return null;
        Project cached = redisCacheService.get(key, Project.class).orElse(null);
        if (cached != null) return cached;
        Project project = projectMapper.findById(id);
        if (project == null) redisCacheService.setNull(key);
        else redisCacheService.set(key, project, redisCacheService.detailTtl());
        return project;
    }

    @Override
    public void incrementViewCount(Long id) {
        projectMapper.incrementViewCount(id);
        redisCacheService.evict(redisKeys.projectDetail(id));
    }

    @Override
    public void incrementStarCount(Long id) {
        projectMapper.incrementStarCount(id);
        redisCacheService.evict(redisKeys.projectDetail(id));
    }

    @Override
    public Project createProject(Project project) {
        projectMapper.insert(project);
        evictAndIndex(project.getId());
        return project;
    }

    @Override
    public Project updateProject(Project project) {
        projectMapper.update(project);
        evictAndIndex(project.getId());
        return projectMapper.findById(project.getId());
    }

    @Override
    public void deleteProject(Long id) {
        projectMapper.deleteById(id);
        redisCacheService.evict(redisKeys.projectDetail(id));
        SearchIndexService indexService = searchIndexServiceProvider.getIfAvailable();
        if (indexService != null) indexService.delete("project", id);
    }

    @Override
    public List<String> getAllCategories() { return projectMapper.getAllCategories(); }

    @Override
    public PageResponse<Project> getProjectsByAuthor(Long authorId, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        size = Math.min(size, 100);
        int offset = (page - 1) * size;
        List<Project> projects = projectMapper.listByAuthor(authorId, offset, size);
        long total = projectMapper.countByAuthor(authorId);
        return new PageResponse<>(projects, total, page, size);
    }

    private void evictAndIndex(Long id) {
        redisCacheService.evict(redisKeys.projectDetail(id));
        SearchIndexService indexService = searchIndexServiceProvider.getIfAvailable();
        if (indexService != null) indexService.upsertProject(id);
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
