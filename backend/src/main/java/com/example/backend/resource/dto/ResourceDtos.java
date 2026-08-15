package com.example.backend.resource.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ResourceDtos {

    /** 列表项：tags 为数组 */
    public static class ResourceListResp {
        private Long id;
        private String title;
        private String description;
        private String category;
        private String type;
        private String resourceUrl;
        private Integer eyeCount;
        private Integer hic;
        private String source;
        private LocalDateTime createdAt;
        private List<String> tags;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getResourceUrl() { return resourceUrl; }
        public void setResourceUrl(String resourceUrl) { this.resourceUrl = resourceUrl; }
        public Integer getEyeCount() { return eyeCount; }
        public void setEyeCount(Integer eyeCount) { this.eyeCount = eyeCount; }
        public Integer getHic() { return hic; }
        public void setHic(Integer hic) { this.hic = hic; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
    }

    /** 详情：tags 为数组；likeCount/liked 由 Controller 填充；contentUrl 为 Markdown 内容路径 */
    public static class ResourceDetailResp {
        private Long id;
        private String title;
        private String description;
        private String category;
        private String type;
        private String resourceUrl;
        private Integer eyeCount;
        private Integer hic;
        private String source;
        private LocalDateTime createdAt;
        private List<String> tags;
        private Long likeCount;
        private Boolean liked;
        private String contentUrl;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getResourceUrl() { return resourceUrl; }
        public void setResourceUrl(String resourceUrl) { this.resourceUrl = resourceUrl; }
        public Integer getEyeCount() { return eyeCount; }
        public void setEyeCount(Integer eyeCount) { this.eyeCount = eyeCount; }
        public Integer getHic() { return hic; }
        public void setHic(Integer hic) { this.hic = hic; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        public Long getLikeCount() { return likeCount; }
        public void setLikeCount(Long likeCount) { this.likeCount = likeCount; }
        public Boolean getLiked() { return liked; }
        public void setLiked(Boolean liked) { this.liked = liked; }
        public String getContentUrl() { return contentUrl; }
        public void setContentUrl(String contentUrl) { this.contentUrl = contentUrl; }
    }
}
