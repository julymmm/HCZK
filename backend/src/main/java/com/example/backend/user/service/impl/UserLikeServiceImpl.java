package com.example.backend.user.service.impl;

import com.example.backend.common.exception.BusinessException;
import com.example.backend.common.exception.ErrorCode;
import com.example.backend.user.mapper.UserLikeMapper;
import com.example.backend.user.model.User;
import com.example.backend.user.model.UserLike;
import com.example.backend.user.service.UserLikeService;
import com.example.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserLikeServiceImpl implements UserLikeService {
    private final UserLikeMapper userLikeMapper;
    private final UserService userService;

    @Override
    @Transactional
    public boolean toggleLike(String username, String resourceType, Long resourceId) {
        User user = userService.getByUsername(username);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Login is required");
        }
        Long userId = user.getId();
        int exists = userLikeMapper.checkUserLike(userId, resourceType, resourceId);
        if (exists > 0) {
            userLikeMapper.deleteUserLike(userId, resourceType, resourceId);
            return false;
        }
        UserLike userLike = new UserLike();
        userLike.setUserId(userId);
        userLike.setResourceType(resourceType);
        userLike.setResourceId(resourceId);
        userLike.setCreatedAt(LocalDateTime.now());
        userLikeMapper.insertUserLike(userLike);
        return true;
    }

    @Override
    public boolean isLiked(String username, String resourceType, Long resourceId) {
        User user = userService.getByUsername(username);
        if (user == null) {
            return false;
        }
        return userLikeMapper.checkUserLike(user.getId(), resourceType, resourceId) > 0;
    }

    @Override
    public int getLikeCount(String resourceType, Long resourceId) {
        return userLikeMapper.getLikeCount(resourceType, resourceId);
    }
}