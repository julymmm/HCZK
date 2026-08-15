package com.example.backend.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.search.*;
import com.example.backend.article.model.Article;
import com.example.backend.project.model.Project;
import com.example.backend.resource.model.Resource;
import com.example.backend.share.model.Share;
import com.example.backend.article.mapper.ArticleMapper;
import com.example.backend.project.mapper.ProjectMapper;
import com.example.backend.resource.mapper.ResourceMapper;
import com.example.backend.share.mapper.ShareMapper;
import com.example.backend.search.config.SearchProperties;
import com.example.backend.search.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final ObjectProvider<ElasticsearchClient> elasticsearchClientProvider;
    private final SearchProperties properties;
    private final ShareMapper shareMapper;
    private final ResourceMapper resourceMapper;
    private final ProjectMapper projectMapper;
    private final ArticleMapper articleMapper;

    @SuppressWarnings("unchecked")
    public SearchResponse search(String q, String type, String category, String tagsCsv, int size, String after) {
        int safeSize = Math.max(1, Math.min(size, 30));
        ElasticsearchClient es = elasticsearchClientProvider.getIfAvailable();
        if (properties.isEnabled() && es != null) {
            try {
                List<String> tags = parseCsv(tagsCsv);
                List<FieldValue> afterValues = parseAfter(after);
                List<SortOptions> sorts = List.of(
                        SortOptions.of(s -> s.score(o -> o.order(SortOrder.Desc))),
                        SortOptions.of(s -> s.field(f -> f.field("publishTime").order(SortOrder.Desc).missing("_last"))),
                        SortOptions.of(s -> s.field(f -> f.field("viewCount").order(SortOrder.Desc).missing("_last"))),
                        SortOptions.of(s -> s.field(f -> f.field("contentId.keyword").order(SortOrder.Desc)))
                );
                co.elastic.clients.elasticsearch.core.SearchResponse<Map<String,Object>> resp = es.search(s -> {
                    var builder = s.index(properties.getIndex().getContent()).size(safeSize)
                            .query(query -> query.functionScore(fs -> fs
                                    .query(inner -> inner.bool(b -> {
                                        if (StringUtils.hasText(q)) b.must(m -> m.multiMatch(mm -> mm.query(q).fields("title^3", "description^2", "body")));
                                        else b.must(m -> m.matchAll(ma -> ma));
                                        b.filter(f -> f.term(t -> t.field("status").value("published")));
                                        if (StringUtils.hasText(type)) b.filter(f -> f.term(t -> t.field("contentType").value(type)));
                                        if (StringUtils.hasText(category)) b.filter(f -> f.term(t -> t.field("category").value(category)));
                                        if (!tags.isEmpty()) b.filter(f -> f.terms(t -> t.field("tags").terms(v -> v.value(tags.stream().map(FieldValue::of).toList()))));
                                        return b;
                                    }))
                                    .functions(fn -> fn.fieldValueFactor(fvf -> fvf.field("viewCount").modifier(FieldValueFactorModifier.Log1p)).weight(1.0))
                                    .functions(fn -> fn.fieldValueFactor(fvf -> fvf.field("likeCount").modifier(FieldValueFactorModifier.Log1p)).weight(2.0))
                                    .boostMode(FunctionBoostMode.Sum)))
                            .highlight(h -> h.fields("title", hf -> hf).fields("description", hf -> hf).fields("body", hf -> hf))
                            .sort(sorts);
                    if (!afterValues.isEmpty()) builder = builder.searchAfter(afterValues);
                    return builder;
                }, (Class<Map<String,Object>>)(Class<?>)Map.class);
                List<Hit<Map<String,Object>>> hits = resp.hits() == null ? Collections.emptyList() : resp.hits().hits();
                List<SearchItem> items = hits.stream().map(this::toItem).filter(Objects::nonNull).toList();
                String nextAfter = encodeAfter(hits);
                return new SearchResponse(items, nextAfter, items.size() >= safeSize && nextAfter != null);
            } catch (Exception ignored) {
                return fallbackSearch(q, type, category, tagsCsv, safeSize);
            }
        }
        return fallbackSearch(q, type, category, tagsCsv, safeSize);
    }

    public SuggestResponse suggest(String prefix, int size) {
        if (!StringUtils.hasText(prefix)) return new SuggestResponse(Collections.emptyList());
        SearchResponse resp = search(prefix, null, null, null, Math.max(1, Math.min(size, 10)), null);
        Set<String> titles = new LinkedHashSet<>();
        for (SearchItem item : resp.getItems()) { if (StringUtils.hasText(item.getTitle())) titles.add(stripHighlight(item.getTitle())); if (titles.size() >= size) break; }
        return new SuggestResponse(new ArrayList<>(titles));
    }

    private SearchResponse fallbackSearch(String q, String type, String category, String tagsCsv, int size) {
        List<SearchItem> items = new ArrayList<>();
        if (include(type, "share")) for (Share s : shareMapper.list(0, size, q, category)) items.add(SearchDocumentFactory.fromShare(s, false));
        if (include(type, "resource")) for (Resource r : resourceMapper.list(0, size, category, null, q, parseCsv(tagsCsv), null)) items.add(SearchDocumentFactory.fromResource(r, false));
        if (include(type, "project")) for (Project p : projectMapper.list(0, size, category, q)) items.add(SearchDocumentFactory.fromProject(p, false));
        if (include(type, "article")) for (Article a : articleMapper.list(0, size, q, null)) items.add(SearchDocumentFactory.fromArticle(a, false));
        return new SearchResponse(items.stream().limit(size).toList(), null, false);
    }

    private boolean include(String requested, String actual) { return !StringUtils.hasText(requested) || actual.equalsIgnoreCase(requested); }
    private SearchItem toItem(Hit<Map<String,Object>> hit) { Map<String,Object> s = hit.source(); if (s == null) return null; SearchItem i = new SearchItem(); i.setContentId(asString(s.get("contentId"))); i.setContentType(asString(s.get("contentType"))); i.setTitle(firstHighlight(hit,"title",asString(s.get("title")))); i.setDescription(firstHighlight(hit,"description",firstHighlight(hit,"body",asString(s.get("description"))))); i.setCategory(asString(s.get("category"))); i.setTags(asStringList(s.get("tags"))); i.setAuthorId(asLong(s.get("authorId"))); i.setAuthorName(asString(s.get("authorName"))); i.setCoverUrl(asString(s.get("coverUrl"))); i.setUrl(asString(s.get("url"))); i.setPublishTime(asLong(s.get("publishTime"))); i.setViewCount(asLong(s.get("viewCount"))); i.setLikeCount(asLong(s.get("likeCount"))); i.setHicProtected(asBoolean(s.get("hicProtected"))); i.setStatus(asString(s.get("status"))); i.setHighlights(hit.highlight()); return i; }
    private String firstHighlight(Hit<Map<String,Object>> hit, String field, String fallback) { List<String> v = hit.highlight() == null ? null : hit.highlight().get(field); return v == null || v.isEmpty() ? fallback : String.join(" ", v); }
    private List<String> parseCsv(String csv) { if (!StringUtils.hasText(csv)) return Collections.emptyList(); return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList(); }
    private List<FieldValue> parseAfter(String after) { if (!StringUtils.hasText(after)) return Collections.emptyList(); try { String[] p = new String(Base64.getUrlDecoder().decode(after), StandardCharsets.UTF_8).split("\\|", -1); return List.of(FieldValue.of(Double.parseDouble(p[0])), FieldValue.of(Long.parseLong(p[1])), FieldValue.of(Long.parseLong(p[2])), FieldValue.of(p[3])); } catch (Exception e) { return Collections.emptyList(); } }
    private String encodeAfter(List<Hit<Map<String,Object>>> hits) { if (hits == null || hits.isEmpty()) return null; List<FieldValue> sort = hits.get(hits.size() - 1).sort(); if (sort == null || sort.isEmpty()) return null; return Base64.getUrlEncoder().withoutPadding().encodeToString(String.join("|", sort.stream().map(f -> String.valueOf(f._get())).toList()).getBytes(StandardCharsets.UTF_8)); }
    private String stripHighlight(String s) { return s == null ? null : s.replace("<em>", "").replace("</em>", ""); }
    private String asString(Object v) { return v == null ? null : String.valueOf(v); }
    private Long asLong(Object v) { if (v instanceof Number n) return n.longValue(); try { return v == null ? null : Long.parseLong(String.valueOf(v)); } catch (Exception e) { return null; } }
    private Boolean asBoolean(Object v) { if (v instanceof Boolean b) return b; if (v instanceof Number n) return n.intValue() != 0; return v == null ? null : Boolean.parseBoolean(String.valueOf(v)); }
    private List<String> asStringList(Object v) { if (v instanceof List<?> l) return l.stream().filter(Objects::nonNull).map(String::valueOf).toList(); return parseCsv(asString(v)); }
}