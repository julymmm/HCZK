package com.example.backend.resource.service;

import com.example.backend.common.response.PageResponse;
import com.example.backend.resource.model.Comment;

public interface CommentService {
    PageResponse<Comment> listByResourceId(Long resourceId, int page, int size);
    Comment add(Long resourceId, Long userId, Long parentId, String content);
}
