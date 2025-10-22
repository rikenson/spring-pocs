package com.tiger.pocs.ingestion.service;

import com.tiger.pocs.ingestion.domain.MessageProcessedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Orchestrator component that coordinates the ingestion pipeline.
 * Applies single responsibility principle by delegating specific tasks to dedicated services.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IngestionProcessor {

    private final PayloadValidator payloadValidator;
    private final EventTypeDetector eventTypeDetector;
    private final PayloadConverter payloadConverter;
    private final PersistenceService entityPersistenceService;


    @EventListener
    public void processKafkaEvent(MessageProcessedEvent event) {
        var eventContext = extractEventContext(event);

        log.info("ORCHESTRATOR: Starting ingestion pipeline for topic: {} (key: {}) via handler: {}",
                eventContext.topic(), eventContext.messageKey(), eventContext.handlerName());

        try {
            // Step 1: Validate and extract payload
            String payload = payloadValidator.validateAndExtractPayload(event);
            if (payload == null) {
                log.info("ORCHESTRATOR: Skipping processing due to invalid payload for topic: {}", eventContext.topic());
                return;
            }

            var eventType = eventTypeDetector.detectEventType(event);
            log.info("ORCHESTRATOR: Detected event type: {} for topic: {}", eventType, eventContext.topic());
            var entity = payloadConverter.convertToEntity(payload, eventType);
            entityPersistenceService.persistEntity(entity, eventType, eventContext.topic(), eventContext.messageKey());

            log.info("ORCHESTRATOR: Successfully completed ingestion pipeline for topic: {} (key: {})",
                    eventContext.topic(), eventContext.messageKey());

        } catch (Exception e) {
            log.error("ORCHESTRATOR: Pipeline failed for topic: {} (key: {}) via handler: {} - {}",
                    eventContext.topic(), eventContext.messageKey(), eventContext.handlerName(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Extracts event context information for logging and processing.
     */
    private EventContext extractEventContext(MessageProcessedEvent event) {
        var kafkaMessage = event.getMessage();
        String topic = kafkaMessage != null ? kafkaMessage.getTopic() : "unknown";
        String messageKey = kafkaMessage != null ? kafkaMessage.getKey() : "unknown";
        String handlerName = event.getHandlerName();

        return new EventContext(topic, messageKey, handlerName);
    }

    /**
     * Simple record to hold event context information.
     */
    private record EventContext(String topic, String messageKey, String handlerName) {
    }
}