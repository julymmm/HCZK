package com.example.backend.resource.mapper;

import com.example.backend.resource.model.Comment;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CommentMapper {

    @Insert("INSERT INTO comments (resource_id, user_id, parent_id, content, created_at) " +
            "VALUES (#{resourceId}, #{userId}, #{parentId}, #{content}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Comment comment);

    @Select("SELECT c.id, c.resource_id AS resourceId, c.user_id AS userId, c.parent_id AS parentId, c.content, c.created_at AS createdAt, " +
            "u.username AS username, u.avatar_url AS avatarUrl " +
            "FROM comments c LEFT JOIN users u ON c.user_id = u.id " +
            "WHERE c.resource_id = #{resourceId} ORDER BY c.created_at ASC LIMIT ${limit} OFFSET ${offset}")
    List<Comment> listByResourceId(@Param("resourceId") Long resourceId, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM comments WHERE resource_id = #{resourceId}")
    long countByResourceId(@Param("resourceId") Long resourceId);
}
