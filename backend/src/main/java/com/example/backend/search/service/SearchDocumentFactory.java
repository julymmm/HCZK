package com.example.backend.search.service;

import com.example.backend.article.model.Article;
import com.example.backend.project.model.Project;
import com.example.backend.resource.model.Resource;
import com.example.backend.search.dto.SearchItem;
import com.example.backend.share.model.Share;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SearchDocumentFactory {
    private SearchDocumentFactory() {
    }

    public static SearchItem fromShare(Share share, boolean forIndex) {
        SearchItem item = base("share", share.getId(), share.getTitle(), share.getContent(), share.getCategory(), share.getTags(),
                share.getPublishTime() == null ? share.getCreatedAt() : share.getPublishTime());
        item.setAuthorId(share.getAuthorId());
        item.setAuthorName(share.getAuthorName());
        item.setViewCount(toLong(share.getViewCount()));
        item.setLikeCount(0L);
        item.setUrl("/share/" + share.getId());
        item.setStatus(share.getStatus() == null ? "published" : share.getStatus());
        return item;
    }

    public static SearchItem fromResource(Resource resource, boolean forIndex) {
        SearchItem item = base("resource", resource.getId(), resource.getTitle(), resource.getDescription(), resource.getCategory(), resource.getTags(), resource.getCreatedAt());
        item.setViewCount(toLong(resource.getEyeCount()));
        item.setLikeCount(0L);
        item.setUrl(resource.getResourceUrl());
        item.setHicProtected(resource.getHic() != null && resource.getHic() == 1);
        item.setStatus("published");
        return item;
    }

    public static SearchItem fromProject(Project project, boolean forIndex) {
        SearchItem item = base("project", project.getId(), project.getTitle(), project.getDescription(), project.getCategory(), null, project.getCreatedAt());
        item.setAuthorId(project.getAuthorId());
        item.setAuthorName(project.getAuthorName());
        item.setViewCount(toLong(project.getViewCount()));
        item.setLikeCount(toLong(project.getStarCount()));
        item.setUrl("/projects/" + project.getId());
        item.setStatus("published");
        return item;
    }

    public static SearchItem fromArticle(Article article, boolean forIndex) {
        SearchItem item = base("article", article.getId(), article.getTitle(), article.getSummary(), article.getSource(), null, article.getPublishTime());
        item.setCoverUrl(article.getCoverImage());
        item.setViewCount(toLong(article.getViewCount()));
        item.setLikeCount(0L);
        item.setUrl(article.getLinkUrl());
        item.setStatus("published");
        return item;
    }

    public static Map<String, Object> toDocument(SearchItem item, String body) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("contentId", item.getContentId());
        doc.put("contentType", item.getContentType());
        doc.put("title", item.getTitle());
        doc.put("description", item.getDescription());
        doc.put("body", body == null ? item.getDescription() : body);
        doc.put("category", item.getCategory());
        doc.put("tags", item.getTags());
        doc.put("authorId", item.getAuthorId());
        doc.put("authorName", item.getAuthorName());
        doc.put("coverUrl", item.getCoverUrl());
        doc.put("url", item.getUrl());
        doc.put("publishTime", item.getPublishTime());
        doc.put("viewCount", item.getViewCount() == null ? 0L : item.getViewCount());
        doc.put("likeCount", item.getLikeCount() == null ? 0L : item.getLikeCount());
        doc.put("hicProtected", Boolean.TRUE.equals(item.getHicProtected()));
        doc.put("status", item.getStatus() == null ? "published" : item.getStatus());
        doc.put("titleSuggest", item.getTitle());
        return doc;
    }

    private static SearchItem base(String type, Long id, String title, String description, String category, String tags, LocalDateTime time) {
        SearchItem item = new SearchItem();
        item.setContentId(id == null ? null : String.valueOf(id));
        item.setContentType(type);
        item.setTitle(title);
        item.setDescription(description);
        item.setCategory(category);
        item.setTags(parseTags(tags));
        item.setPublishTime(time == null ? null : time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        item.setHicProtected(false);
        return item;
    }

    private static List<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) return Collections.emptyList();
        return Arrays.stream(tags.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static Long toLong(Integer value) {
        return value == null ? 0L : value.longValue();
    }
}
