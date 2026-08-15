package com.example.backend.resource.service.impl;

import com.example.backend.common.response.PageResponse;
import com.example.backend.resource.model.Comment;
import com.example.backend.resource.mapper.CommentMapper;
import com.example.backend.resource.service.CommentService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;

    public CommentServiceImpl(CommentMapper commentMapper) {
        this.commentMapper = commentMapper;
    }

    @Override
    public PageResponse<Comment> listByResourceId(Long resourceId, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        size = Math.min(size, 100);
        int offset = (page - 1) * size;
        List<Comment> data = commentMapper.listByResourceId(resourceId, offset, size);
        long total = commentMapper.countByResourceId(resourceId);
        return new PageResponse<>(data, total, page, size);
    }

    @Override
    public Comment add(Long resourceId, Long userId, Long parentId, String content) {
        Comment c = new Comment();
        c.setResourceId(resourceId);
        c.setUserId(userId);
        c.setParentId(parentId);
        c.setContent(content != null ? content.trim() : "");
        commentMapper.insert(c);
        return c;
    }
}
