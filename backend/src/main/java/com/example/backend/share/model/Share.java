package com.example.backend.share.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Share {
    private Long id;
    private String title;
    private String content;
    private String category;
    private String tags;
    private Long authorId;
    private String authorName;
    private String textUrl;
    private String aiSummary;
    private String status;
    private String contentObjectKey;
    private String contentEtag;
    private Long contentSize;
    private String contentSha256;
    private LocalDateTime publishTime;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
