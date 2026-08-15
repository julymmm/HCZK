package com.example.backend.search.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchItem {
    private String contentId;
    private String contentType;
    private String title;
    private String description;
    private String category;
    private List<String> tags;
    private Long authorId;
    private String authorName;
    private String coverUrl;
    private String url;
    private Long publishTime;
    private Long viewCount;
    private Long likeCount;
    private Boolean hicProtected;
    private String status;
    private Map<String, List<String>> highlights;
}
