package com.example.backend.resource.service.impl;

import com.example.backend.resource.model.ResourceLike;
import com.example.backend.resource.mapper.ResourceLikeMapper;
import com.example.backend.resource.service.ResourceLikeService;
import org.springframework.stereotype.Service;

@Service
public class ResourceLikeServiceImpl implements ResourceLikeService {

    private final ResourceLikeMapper resourceLikeMapper;

    public ResourceLikeServiceImpl(ResourceLikeMapper resourceLikeMapper) {
        this.resourceLikeMapper = resourceLikeMapper;
    }

    @Override
    public void like(Long resourceId, Long userId) {
        if (resourceLikeMapper.exists(resourceId, userId) != null) {
            return;
        }
        ResourceLike like = new ResourceLike();
        like.setResourceId(resourceId);
        like.setUserId(userId);
        resourceLikeMapper.insert(like);
    }

    @Override
    public void unlike(Long resourceId, Long userId) {
        resourceLikeMapper.delete(resourceId, userId);
    }

    @Override
    public long countByResourceId(Long resourceId) {
        return resourceLikeMapper.countByResourceId(resourceId);
    }

    @Override
    public boolean isLiked(Long resourceId, Long userId) {
        return resourceLikeMapper.exists(resourceId, userId) != null;
    }
}
