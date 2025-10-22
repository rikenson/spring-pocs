package com.tiger.pocs.kafka.processor;

import com.tiger.pocs.kafka.domain.KafkaMessage;
import com.tiger.pocs.kafka.domain.KafkaEventConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserAccessEventProcessor {

    private final ApplicationEventPublisher eventPublisher;
    private final KafkaEventConverter kafkaEventConverter;

    public void processUserAccessMessage(KafkaMessage<String> message) {
        log.info("🟧 [USER ACCESS PROCESSOR] Processing user access message: key={}, topic={}", message.getKey(), message.getTopic());

        try {
            handleUserAccessEvent(message);
            log.info("🟧 [USER ACCESS PROCESSOR] Successfully processed user access message");
        } catch (Exception e) {
            log.error("❌ [USER ACCESS PROCESSOR] Failed to process user access message: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void handleUserAccessEvent(KafkaMessage<String> message) {
        log.info("🟧 Processing user access data ...");
        
        var event = kafkaEventConverter.convertToIngestionEvent(message, "UserAccessEventProcessor");
        eventPublisher.publishEvent(event);
        log.info("🟧 User access data MessageProcessedEvent published successfully.");
    }
}