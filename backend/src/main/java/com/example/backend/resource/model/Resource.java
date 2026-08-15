package com.example.backend.resource.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Resource {
    private Long id;
    private String title;
    private String description;
    private String category; // software, hardware, embed, ai, other
    private String type; // document, video, project, other
    private String resourceUrl;
    private Integer eyeCount;
    private Integer hic; // HIC 认证要求：0-不需要认证，1-需要认证
    /** 本科/研究生标签：undergrad-本科，postgrad-研究生 */
    private String source;

    private LocalDateTime createdAt;

    private String tags;

    /** Markdown 内容地址，当前应保存 OSS 可访问 URL。 */
    private String contentUrl;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getResourceUrl() {
        return resourceUrl;
    }

    public void setResourceUrl(String resourceUrl) {
        this.resourceUrl = resourceUrl;
    }

    public Integer getEyeCount() {
        return eyeCount;
    }

    public void setEyeCount(Integer eyeCount) {
        this.eyeCount = eyeCount;
    }

    public Integer getHic() {
        return hic;
    }

    public void setHic(Integer hic) {
        this.hic = hic;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }
}