package com.example.backend.qna.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 答疑解惑-问题实体
 */
@Data
public class QnaQuestion {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String tags;
    private Integer viewCount;
    private Integer answerCount;
    private Long acceptedAnswerId;
    private LocalDateTime createdAt;
    private String authorName;
}
