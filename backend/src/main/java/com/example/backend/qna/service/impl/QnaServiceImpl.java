package com.example.backend.qna.service.impl;

import com.example.backend.common.exception.BusinessException;
import com.example.backend.common.exception.ErrorCode;
import com.example.backend.common.response.PageResponse;
import com.example.backend.qna.model.QnaAnswer;
import com.example.backend.qna.model.QnaQuestion;
import com.example.backend.qna.mapper.PointMapper;
import com.example.backend.qna.mapper.QnaAnswerMapper;
import com.example.backend.qna.mapper.QnaQuestionMapper;
import com.example.backend.qna.service.PointService;
import com.example.backend.qna.service.QnaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QnaServiceImpl implements QnaService {

    private final QnaQuestionMapper questionMapper;
    private final QnaAnswerMapper answerMapper;
    private final PointService pointService;
    private final PointMapper pointMapper;

    @Value("${app.qna.points.accepted:10}")
    private int pointsAccepted;
    @Value("${app.qna.points.like-threshold:5}")
    private int likeThreshold;
    @Value("${app.qna.points.like-reward:2}")
    private int likeReward;

    public QnaServiceImpl(QnaQuestionMapper questionMapper, QnaAnswerMapper answerMapper,
                          PointService pointService, PointMapper pointMapper) {
        this.questionMapper = questionMapper;
        this.answerMapper = answerMapper;
        this.pointService = pointService;
        this.pointMapper = pointMapper;
    }

    @Override
    public PageResponse<QnaQuestion> listQuestions(int page, int size, String search, String tags) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        size = Math.min(size, 100);
        int offset = (page - 1) * size;
        List<QnaQuestion> data = questionMapper.list(offset, size, search, tags);
        long total = questionMapper.count(search, tags);
        return new PageResponse<>(data, total, page, size);
    }

    @Override
    public QnaQuestion getQuestionById(Long id, boolean increaseView) {
        if (increaseView) questionMapper.incrementViewCount(id);
        return questionMapper.getById(id);
    }

    @Override
    @Transactional
    public QnaQuestion createQuestion(Long userId, String title, String content, String tags) {
        QnaQuestion q = new QnaQuestion();
        q.setUserId(userId);
        q.setTitle(title);
        q.setContent(content);
        q.setTags(tags);
        q.setViewCount(0);
        q.setAnswerCount(0);
        q.setCreatedAt(LocalDateTime.now());
        questionMapper.insert(q);
        return questionMapper.getById(q.getId());
    }

    @Override
    public PageResponse<QnaAnswer> listAnswers(Long questionId, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 20;
        List<QnaAnswer> all = answerMapper.listByQuestionId(questionId);
        long total = all.size();
        int offset = (page - 1) * size;
        int end = Math.min(offset + size, all.size());
        List<QnaAnswer> data = offset < all.size() ? all.subList(offset, end) : List.of();
        return new PageResponse<>(data, total, page, size);
    }

    @Override
    @Transactional
    public QnaAnswer createAnswer(Long userId, Long questionId, String content) {
        QnaAnswer a = new QnaAnswer();
        a.setQuestionId(questionId);
        a.setUserId(userId);
        a.setContent(content);
        a.setLikeCount(0);
        a.setIsAccepted(0);
        a.setCreatedAt(LocalDateTime.now());
        answerMapper.insert(a);
        questionMapper.incrementAnswerCount(questionId);
        return answerMapper.getById(a.getId());
    }

    @Override
    @Transactional
    public QnaAnswer acceptAnswer(Long questionOwnerId, Long answerId) {
        QnaAnswer ans = answerMapper.getById(answerId);
        if (ans == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Answer not found");
        }
        QnaQuestion q = questionMapper.getById(ans.getQuestionId());
        if (q == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Question not found");
        }
        if (!q.getUserId().equals(questionOwnerId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Only the question owner can accept answers");
        }
        if (ans.getIsAccepted() != null && ans.getIsAccepted() == 1) return ans;

        answerMapper.clearAcceptedByQuestionId(ans.getQuestionId());
        answerMapper.setAccepted(answerId);
        questionMapper.setAcceptedAnswer(ans.getQuestionId(), answerId);

        if (pointMapper.countByRef("qna_accept", answerId) == 0) {
            pointService.addPoints(ans.getUserId(), pointsAccepted, "Answer accepted", "qna_accept", answerId);
        }
        return answerMapper.getById(answerId);
    }

    @Override
    @Transactional
    public Map<String, Object> toggleLike(Long userId, Long answerId) {
        QnaAnswer ans = answerMapper.getById(answerId);
        if (ans == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Answer not found");
        }
        int hasLiked = answerMapper.hasLiked(answerId, userId);
        boolean liked;
        if (hasLiked > 0) {
            answerMapper.removeLike(answerId, userId);
            answerMapper.decrementLikeCount(answerId);
            liked = false;
        } else {
            answerMapper.addLike(answerId, userId);
            answerMapper.incrementLikeCount(answerId);
            liked = true;
            int newCount = ans.getLikeCount() == null ? 1 : ans.getLikeCount() + 1;
            if (newCount >= likeThreshold && pointMapper.countByRef("qna_like_reward", answerId) == 0) {
                pointService.addPoints(ans.getUserId(), likeReward, "Answer like reward", "qna_like_reward", answerId);
            }
        }
        QnaAnswer updated = answerMapper.getById(answerId);
        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        result.put("likeCount", updated.getLikeCount());
        return result;
    }
}
