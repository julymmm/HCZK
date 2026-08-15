package com.example.backend.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import com.example.backend.article.model.Article;
import com.example.backend.project.model.Project;
import com.example.backend.resource.model.Resource;
import com.example.backend.share.model.Share;
import com.example.backend.article.mapper.ArticleMapper;
import com.example.backend.project.mapper.ProjectMapper;
import com.example.backend.resource.mapper.ResourceMapper;
import com.example.backend.share.mapper.ShareMapper;
import com.example.backend.search.config.SearchProperties;
import com.example.backend.search.dto.SearchItem;
import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SearchIndexService {
    private static final Logger log = LoggerFactory.getLogger(SearchIndexService.class);
    private final ObjectProvider<ElasticsearchClient> elasticsearchClientProvider;
    private final SearchProperties properties;
    private final ShareMapper shareMapper;
    private final ResourceMapper resourceMapper;
    private final ProjectMapper projectMapper;
    private final ArticleMapper articleMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void backfillIfEmpty() {
        ElasticsearchClient es = client();
        if (es == null) return;
        try {
            boolean exists = es.indices().exists(e -> e.index(properties.getIndex().getContent())).value();
            if (!exists) es.indices().create(c -> c.index(properties.getIndex().getContent()));
            if (es.count(c -> c.index(properties.getIndex().getContent())).count() == 0) backfill(200);
        } catch (Exception e) { log.warn("Search backfill skipped: {}", e.getMessage()); }
    }

    public void backfill(int limit) {
        try {
            for (Share s : shareMapper.list(0, limit, null, null)) upsertShare(s.getId());
            for (Resource r : resourceMapper.list(0, limit, null, null, null, null, null)) upsertResource(r.getId());
            for (Project p : projectMapper.list(0, limit, null, null)) upsertProject(p.getId());
            for (Article a : articleMapper.list(0, limit, null, null)) upsertArticle(a.getId());
        } catch (Exception e) { log.warn("Search backfill failed: {}", e.getMessage()); }
    }
    public void upsertShare(Long id) { if (id == null) return; Share s = shareMapper.getById(id); if (s == null) return; if (!"published".equalsIgnoreCase(String.valueOf(s.getStatus()))) { delete("share", id); return; } upsert("share", id, SearchDocumentFactory.fromShare(s, true), s.getContent()); }
    public void upsertResource(Long id) { if (id == null) return; Resource r = resourceMapper.findById(id); if (r != null) upsert("resource", id, SearchDocumentFactory.fromResource(r, true), r.getHic() != null && r.getHic() == 1 ? null : r.getDescription()); }
    public void upsertProject(Long id) { if (id == null) return; Project p = projectMapper.findById(id); if (p != null) upsert("project", id, SearchDocumentFactory.fromProject(p, true), p.getDetailedDescription()); }
    public void upsertArticle(Long id) { if (id == null) return; Article a = articleMapper.findById(id); if (a != null) upsert("article", id, SearchDocumentFactory.fromArticle(a, true), a.getSummary()); }
    public void delete(String type, Long id) { ElasticsearchClient es = client(); if (es == null || id == null) return; try { es.delete(d -> d.index(properties.getIndex().getContent()).id(type + ":" + id).refresh(Refresh.WaitFor)); } catch (Exception e) { log.warn("Search delete failed for {}:{}: {}", type, id, e.getMessage()); } }
    private void upsert(String type, Long id, SearchItem item, String body) { ElasticsearchClient es = client(); if (es == null) return; try { Map<String,Object> doc = SearchDocumentFactory.toDocument(item, body); es.index(i -> i.index(properties.getIndex().getContent()).id(type + ":" + id).document(doc).refresh(Refresh.WaitFor)); } catch (Exception e) { log.warn("Search index upsert failed for {}:{}: {}", type, id, e.getMessage()); } }
    private ElasticsearchClient client() { return properties.isEnabled() ? elasticsearchClientProvider.getIfAvailable() : null; }
}
