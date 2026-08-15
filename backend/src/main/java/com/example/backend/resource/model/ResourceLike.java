package com.example.backend.resource.model;

import java.time.LocalDateTime;

public class ResourceLike {
    private Long resourceId;
    private Long userId;
    private LocalDateTime createdAt;

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
