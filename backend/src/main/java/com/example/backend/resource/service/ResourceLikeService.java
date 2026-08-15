package com.example.backend.resource.service;

public interface ResourceLikeService {
    void like(Long resourceId, Long userId);
    void unlike(Long resourceId, Long userId);
    long countByResourceId(Long resourceId);
    boolean isLiked(Long resourceId, Long userId);
}
