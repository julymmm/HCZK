package com.example.backend.portal.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Tag {
    private Long id;              // 标签ID
    private String name;          // 标签名称，例如：C++,CV,考研
    private LocalDateTime createdAt; // 创建时间

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}