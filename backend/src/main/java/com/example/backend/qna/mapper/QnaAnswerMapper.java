package com.example.backend.qna.mapper;

import com.example.backend.qna.model.QnaAnswer;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface QnaAnswerMapper {

    @Select("SELECT a.id, a.question_id AS questionId, a.user_id AS userId, a.content, a.like_count AS likeCount, " +
            "a.is_accepted AS isAccepted, a.created_at AS createdAt, COALESCE(u.nickname, u.username) AS authorName " +
            "FROM qna_answers a LEFT JOIN users u ON a.user_id = u.id WHERE a.question_id = #{questionId} ORDER BY a.is_accepted DESC, a.like_count DESC, a.created_at ASC")
    List<QnaAnswer> listByQuestionId(@Param("questionId") Long questionId);

    @Select("SELECT a.id, a.question_id AS questionId, a.user_id AS userId, a.content, a.like_count AS likeCount, " +
            "a.is_accepted AS isAccepted, a.created_at AS createdAt, COALESCE(u.nickname, u.username) AS authorName " +
            "FROM qna_answers a LEFT JOIN users u ON a.user_id = u.id WHERE a.id = #{id}")
    QnaAnswer getById(@Param("id") Long id);

    @Insert("INSERT INTO qna_answers (question_id, user_id, content, like_count, is_accepted, created_at) " +
            "VALUES (#{questionId}, #{userId}, #{content}, #{likeCount}, #{isAccepted}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(QnaAnswer a);

    @Update("UPDATE qna_answers SET is_accepted = 0 WHERE question_id = #{questionId}")
    void clearAcceptedByQuestionId(@Param("questionId") Long questionId);

    @Update("UPDATE qna_answers SET is_accepted = 1 WHERE id = #{id}")
    void setAccepted(@Param("id") Long id);

    @Update("UPDATE qna_answers SET like_count = like_count + 1 WHERE id = #{id}")
    void incrementLikeCount(@Param("id") Long id);

    @Update("UPDATE qna_answers SET like_count = like_count - 1 WHERE id = #{id} AND like_count > 0")
    void decrementLikeCount(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM qna_answer_likes WHERE answer_id = #{answerId} AND user_id = #{userId}")
    int hasLiked(@Param("answerId") Long answerId, @Param("userId") Long userId);

    @Insert("INSERT INTO qna_answer_likes (answer_id, user_id) VALUES (#{answerId}, #{userId})")
    void addLike(@Param("answerId") Long answerId, @Param("userId") Long userId);

    @Delete("DELETE FROM qna_answer_likes WHERE answer_id = #{answerId} AND user_id = #{userId}")
    void removeLike(@Param("answerId") Long answerId, @Param("userId") Long userId);
}
