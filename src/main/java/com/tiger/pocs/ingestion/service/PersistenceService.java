package com.tiger.pocs.ingestion.service;

import com.tiger.pocs.ingestion.service.EventTypeDetector.EventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class PersistenceService {

    private final ReactiveMongoTemplate mongoTemplate;

    public void persistEntity(Object entity, EventType eventType, String topic, String messageKey) {
        if (entity != null) {
            log.info("PERSISTENCE: Saving {} entity to MongoDB for topic: {}", eventType, topic);

            mongoTemplate.save(entity).subscribe(
                    result -> log.info(
                            "🟢 PERSISTENCE: {} entity saved successfully for topic: {} (key: {})",
                            eventType, topic, messageKey),

                    error -> log.error(
                            "🔴 PERSISTENCE: ❌ Failed to save {} entity for topic: {} (key: {}) - {}",
                            eventType, topic, messageKey, error.getMessage(), error)
            );
        } else {
            log.info("PERSISTENCE: Skipping save for {} event type from topic: {} (key: {}) - entity is null",
                    eventType, topic, messageKey);
        }
    }
}