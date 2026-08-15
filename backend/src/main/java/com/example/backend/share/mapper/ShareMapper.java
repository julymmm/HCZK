package com.example.backend.share.mapper;

import com.example.backend.share.model.Share;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ShareMapper {
    String SHARE_COLUMNS = "s.id, s.title, s.content, s.category, s.tags, s.author_id AS authorId, "
            + "s.text_url AS textUrl, s.ai_summary AS aiSummary, s.status, "
            + "s.content_object_key AS contentObjectKey, s.content_etag AS contentEtag, "
            + "s.content_size AS contentSize, s.content_sha256 AS contentSha256, "
            + "s.publish_time AS publishTime, s.view_count AS viewCount, s.created_at AS createdAt, s.updated_at AS updatedAt";

    @Select({
            "<script>",
            "SELECT " + SHARE_COLUMNS + ", COALESCE(u.nickname, u.username) AS authorName",
            "FROM shares s LEFT JOIN users u ON s.author_id = u.id",
            "<where>",
            "AND s.status = 'published'",
            "<if test='category != null and category != \"\" and category != \"全部\"'>",
            "AND s.category = #{category}",
            "</if>",
            "<if test='search != null and search != \"\"'>",
            "AND (s.title LIKE CONCAT('%', #{search}, '%') OR s.content LIKE CONCAT('%', #{search}, '%'))",
            "</if>",
            "</where>",
            "ORDER BY COALESCE(s.publish_time, s.created_at) DESC, s.id DESC",
            "LIMIT ${limit} OFFSET ${offset}",
            "</script>"
    })
    List<Share> list(@Param("offset") int offset, @Param("limit") int limit, @Param("search") String search, @Param("category") String category);

    @Select({
            "<script>",
            "SELECT COUNT(*) FROM shares s",
            "<where>",
            "AND s.status = 'published'",
            "<if test='category != null and category != \"\" and category != \"全部\"'>",
            "AND s.category = #{category}",
            "</if>",
            "<if test='search != null and search != \"\"'>",
            "AND (s.title LIKE CONCAT('%', #{search}, '%') OR s.content LIKE CONCAT('%', #{search}, '%'))",
            "</if>",
            "</where>",
            "</script>"
    })
    long count(@Param("search") String search, @Param("category") String category);

    @Select("SELECT " + SHARE_COLUMNS + ", COALESCE(u.nickname, u.username) AS authorName "
            + "FROM shares s LEFT JOIN users u ON s.author_id = u.id WHERE s.id = #{id}")
    Share getById(@Param("id") Long id);

    @Select("SELECT " + SHARE_COLUMNS + ", COALESCE(u.nickname, u.username) AS authorName "
            + "FROM shares s LEFT JOIN users u ON s.author_id = u.id WHERE s.id = #{id} AND s.author_id = #{authorId}")
    Share getOwnedById(@Param("id") Long id, @Param("authorId") Long authorId);

    @Update("UPDATE shares SET view_count = view_count + 1 WHERE id = #{id} AND status = 'published'")
    void incrementViewCount(@Param("id") Long id);

    @Insert("INSERT INTO shares (title, content, category, tags, author_id, text_url, ai_summary, status, view_count, created_at, updated_at) "
            + "VALUES (#{title}, #{content}, #{category}, #{tags}, #{authorId}, #{textUrl}, #{aiSummary}, #{status}, #{viewCount}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Share share);

    @Update("UPDATE shares SET text_url = #{textUrl}, updated_at = NOW() WHERE id = #{id}")
    void updateTextUrl(@Param("id") Long id, @Param("textUrl") String textUrl);

    @Update("UPDATE shares SET ai_summary = #{summary}, updated_at = NOW() WHERE id = #{id}")
    void updateAiSummary(@Param("id") Long id, @Param("summary") String summary);

    @Update("UPDATE shares SET text_url = #{textUrl}, content_object_key = #{objectKey}, content_etag = #{etag}, "
            + "content_size = #{size}, content_sha256 = #{sha256}, updated_at = NOW() "
            + "WHERE id = #{id} AND author_id = #{authorId} AND status IN ('draft', 'editing')")
    int confirmContent(@Param("id") Long id,
                       @Param("authorId") Long authorId,
                       @Param("textUrl") String textUrl,
                       @Param("objectKey") String objectKey,
                       @Param("etag") String etag,
                       @Param("size") Long size,
                       @Param("sha256") String sha256);

    @Update({
            "<script>",
            "UPDATE shares",
            "<set>",
            "<if test='title != null'>title = #{title},</if>",
            "<if test='content != null'>content = #{content},</if>",
            "<if test='category != null'>category = #{category},</if>",
            "<if test='tags != null'>tags = #{tags},</if>",
            "<if test='aiSummary != null'>ai_summary = #{aiSummary},</if>",
            "updated_at = NOW()",
            "</set>",
            "WHERE id = #{id} AND author_id = #{authorId} AND status IN ('draft', 'editing', 'published')",
            "</script>"
    })
    int updateMetadata(@Param("id") Long id,
                       @Param("authorId") Long authorId,
                       @Param("title") String title,
                       @Param("content") String content,
                       @Param("category") String category,
                       @Param("tags") String tags,
                       @Param("aiSummary") String aiSummary);

    @Update("UPDATE shares SET status = 'published', publish_time = COALESCE(publish_time, NOW()), updated_at = NOW() "
            + "WHERE id = #{id} AND author_id = #{authorId} AND status IN ('draft', 'editing')")
    int publish(@Param("id") Long id, @Param("authorId") Long authorId);
}
