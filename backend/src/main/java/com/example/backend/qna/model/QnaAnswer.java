package com.example.backend.qna.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 答疑解惑-回答实体
 */
@Data
public class QnaAnswer {
    private Long id;
    private Long questionId;
    private Long userId;
    private String content;
    private Integer likeCount;
    private Integer isAccepted;
    private LocalDateTime createdAt;
    private String authorName;
}
