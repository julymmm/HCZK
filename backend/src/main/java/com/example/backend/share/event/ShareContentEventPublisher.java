package com.example.backend.share.event;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class ShareContentEventPublisher {
    public static final String TOPIC = "hczk.share.content-events";

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectProvider<KafkaTemplate<String, ShareContentEvent>> kafkaTemplateProvider;

    @Value("${app.kafka.enabled:false}")
    private boolean kafkaEnabled;

    /**
     * 数据库事务提交后再发布内容变更事件，避免索引或缓存读取到未提交数据。
     */
    public void publishAfterCommit(ShareContentEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishNow(event);
                }
            });
        } else {
            publishNow(event);
        }
    }

    private void publishNow(ShareContentEvent event) {
        KafkaTemplate<String, ShareContentEvent> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
        if (kafkaEnabled && kafkaTemplate != null) {
            kafkaTemplate.send(TOPIC, String.valueOf(event.shareId()), event);
            return;
        }
        applicationEventPublisher.publishEvent(event);
    }
}
