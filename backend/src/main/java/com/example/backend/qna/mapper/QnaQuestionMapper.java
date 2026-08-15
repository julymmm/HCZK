package com.example.backend.qna.mapper;

import com.example.backend.qna.model.QnaQuestion;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface QnaQuestionMapper {

    @Select({
        "<script>",
        "SELECT q.id, q.user_id AS userId, q.title, q.content, q.tags, q.view_count AS viewCount, q.answer_count AS answerCount,",
        "q.accepted_answer_id AS acceptedAnswerId, q.created_at AS createdAt,",
        "COALESCE(u.nickname, u.username) AS authorName",
        "FROM qna_questions q LEFT JOIN users u ON q.user_id = u.id",
        "<where>",
        "<if test='search != null and search != \"\"'>",
        "AND (q.title LIKE CONCAT('%', #{search}, '%') OR q.content LIKE CONCAT('%', #{search}, '%'))",
        "</if>",
        "<if test='tags != null and tags != \"\"'>",
        "AND q.tags LIKE CONCAT('%', #{tags}, '%')",
        "</if>",
        "</where>",
        "ORDER BY q.created_at DESC, q.id DESC",
        "LIMIT ${limit} OFFSET ${offset}",
        "</script>"
    })
    List<QnaQuestion> list(@Param("offset") int offset, @Param("limit") int limit, @Param("search") String search, @Param("tags") String tags);

    @Select({
        "<script>",
        "SELECT COUNT(*) FROM qna_questions q",
        "<where>",
        "<if test='search != null and search != \"\"'>",
        "AND (q.title LIKE CONCAT('%', #{search}, '%') OR q.content LIKE CONCAT('%', #{search}, '%'))",
        "</if>",
        "<if test='tags != null and tags != \"\"'>",
        "AND q.tags LIKE CONCAT('%', #{tags}, '%')",
        "</if>",
        "</where>",
        "</script>"
    })
    long count(@Param("search") String search, @Param("tags") String tags);

    @Select("SELECT q.id, q.user_id AS userId, q.title, q.content, q.tags, q.view_count AS viewCount, q.answer_count AS answerCount, " +
            "q.accepted_answer_id AS acceptedAnswerId, q.created_at AS createdAt, " +
            "COALESCE(u.nickname, u.username) AS authorName " +
            "FROM qna_questions q LEFT JOIN users u ON q.user_id = u.id WHERE q.id = #{id}")
    QnaQuestion getById(@Param("id") Long id);

    @Update("UPDATE qna_questions SET view_count = view_count + 1 WHERE id = #{id}")
    void incrementViewCount(@Param("id") Long id);

    @Update("UPDATE qna_questions SET answer_count = answer_count + 1 WHERE id = #{questionId}")
    void incrementAnswerCount(@Param("questionId") Long questionId);

    @Update("UPDATE qna_questions SET accepted_answer_id = #{answerId} WHERE id = #{questionId}")
    void setAcceptedAnswer(@Param("questionId") Long questionId, @Param("answerId") Long answerId);

    @Insert("INSERT INTO qna_questions (user_id, title, content, tags, view_count, answer_count, created_at) " +
            "VALUES (#{userId}, #{title}, #{content}, #{tags}, #{viewCount}, #{answerCount}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(QnaQuestion q);
}
