package com.example.backend.qna.service;

import com.example.backend.common.response.PageResponse;
import com.example.backend.qna.model.QnaAnswer;
import com.example.backend.qna.model.QnaQuestion;
import java.util.Map;

public interface QnaService {
    PageResponse<QnaQuestion> listQuestions(int page, int size, String search, String tags);
    QnaQuestion getQuestionById(Long id, boolean increaseView);
    QnaQuestion createQuestion(Long userId, String title, String content, String tags);
    PageResponse<QnaAnswer> listAnswers(Long questionId, int page, int size);
    QnaAnswer createAnswer(Long userId, Long questionId, String content);
    QnaAnswer acceptAnswer(Long questionOwnerId, Long answerId);
    Map<String, Object> toggleLike(Long userId, Long answerId);
}
