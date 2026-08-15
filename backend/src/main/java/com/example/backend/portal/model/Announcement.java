package com.example.backend.portal.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Announcement {
    private Long id;
    private String title;
    private String summary;
    /** 内容字段可保存 Markdown 正文或 OSS Markdown 文件 URL。 */
    private String content;
    private String coverUrl;
    private Integer viewCount;
    private LocalDateTime publishTime;
}