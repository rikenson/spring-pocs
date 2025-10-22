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
public class UserEventProcessor {

    private final ApplicationEventPublisher eventPublisher;
    private final KafkaEventConverter kafkaEventConverter;

    public void processUserMessage(KafkaMessage<String> message) {
        log.info("🟩 [USER PROCESSOR] Processing user message: key={}, topic={}", message.getKey(), message.getTopic());

        try {
            handleUserEvent(message);
            log.info("✅ [USER PROCESSOR] Successfully processed user message");
        } catch (Exception e) {
            log.error("❌ [USER PROCESSOR] Failed to process user message: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void handleUserEvent(KafkaMessage<String> message) {
        log.info("🟩 Processing user data ...");
        
        var event = kafkaEventConverter.convertToIngestionEvent(message, "UserEventProcessor");
        eventPublisher.publishEvent(event);
        log.info("🟩 User data MessageProcessedEvent published successfully.");
    }
}