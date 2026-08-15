package com.example.backend.resource.mapper;

import com.example.backend.resource.model.ResourceLike;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ResourceLikeMapper {

    @Insert("INSERT INTO resource_likes (resource_id, user_id, created_at) VALUES (#{resourceId}, #{userId}, NOW())")
    int insert(ResourceLike like);

    @Delete("DELETE FROM resource_likes WHERE resource_id = #{resourceId} AND user_id = #{userId}")
    int delete(@Param("resourceId") Long resourceId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM resource_likes WHERE resource_id = #{resourceId}")
    long countByResourceId(@Param("resourceId") Long resourceId);

    @Select("SELECT 1 FROM resource_likes WHERE resource_id = #{resourceId} AND user_id = #{userId} LIMIT 1")
    Integer exists(@Param("resourceId") Long resourceId, @Param("userId") Long userId);
}
