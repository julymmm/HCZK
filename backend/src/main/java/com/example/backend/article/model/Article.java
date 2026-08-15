package com.example.backend.article.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Article {
    private Long id;
    private String title;
    private String summary;
    private String coverImage;
    private Integer viewCount;
    private String linkUrl;
    private LocalDateTime publishTime;
    private LocalDateTime createdAt;
    /** 来源：undergrad-本科，postgrad-研究生 */
    private String source;
}



