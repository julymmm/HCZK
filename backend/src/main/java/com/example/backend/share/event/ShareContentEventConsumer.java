package com.example.backend.share.event;

import com.example.backend.ai.share.ShareRagService;
import com.example.backend.ai.share.ShareSummaryService;
import com.example.backend.infrastructure.cache.RedisCacheService;
import com.example.backend.infrastructure.cache.RedisKeys;
import com.example.backend.search.service.SearchIndexService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShareContentEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(ShareContentEventConsumer.class);

    private final ObjectProvider<SearchIndexService> searchIndexServiceProvider;
    private final ObjectProvider<ShareRagService> ragServiceProvider;
    private final ObjectProvider<ShareSummaryService> summaryServiceProvider;
    private final RedisCacheService redisCacheService;
    private final RedisKeys redisKeys;

    @EventListener
    public void onLocalEvent(ShareContentEvent event) {
        handle(event);
    }

    @KafkaListener(topics = ShareContentEventPublisher.TOPIC, groupId = "hczk-share-content", autoStartup = "${app.kafka.enabled:false}")
    public void onKafkaEvent(ShareContentEvent event) {
        handle(event);
    }

    /**
     * 发布/更新后的耗时任务集中在事件消费者中执行，主事务只负责写库。
     */
    public void handle(ShareContentEvent event) {
        if (event == null || event.shareId() == null) return;
        Long shareId = event.shareId();
        redisCacheService.evict(redisKeys.shareDetail(shareId), redisKeys.aiShareSummary(shareId));
        try {
            SearchIndexService indexService = searchIndexServiceProvider.getIfAvailable();
            if (indexService != null) {
                if (event.type() == ShareContentEventType.DELETED) indexService.delete("share", shareId);
                else indexService.upsertShare(shareId);
            }
        } catch (Exception e) {
            log.warn("Share search index event failed, shareId={}: {}", shareId, e.getMessage());
        }
        if (event.type() != ShareContentEventType.DELETED) {
            try {
                ShareRagService ragService = ragServiceProvider.getIfAvailable();
                if (ragService != null) ragService.reindex(shareId);
            } catch (Exception e) {
                log.warn("Share RAG pre-index event failed, shareId={}: {}", shareId, e.getMessage());
            }
            try {
                ShareSummaryService summaryService = summaryServiceProvider.getIfAvailable();
                if (summaryService != null) summaryService.summaryForShare(shareId);
            } catch (Exception e) {
                log.warn("Share AI summary event failed, shareId={}: {}", shareId, e.getMessage());
            }
        }
    }
}
