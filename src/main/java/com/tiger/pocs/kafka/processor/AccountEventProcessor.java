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
public class AccountEventProcessor {

    private final ApplicationEventPublisher eventPublisher;
    private final KafkaEventConverter kafkaEventConverter;

    public void processAccountMessage(KafkaMessage<String> message) {
        log.info("🟪 [ACCOUNT PROCESSOR] Processing account message: key={}, topic={}", message.getKey(), message.getTopic());
        try {
            handleAccountEvent(message);
            log.info("🟪 [ACCOUNT PROCESSOR] Successfully processed account message");
        } catch (Exception e) {
            log.error("❌ [ACCOUNT PROCESSOR] Failed to process account message: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void handleAccountEvent(KafkaMessage<String> message) {
        log.info("🟪 Processing account data ...");
        
        var event = kafkaEventConverter.convertToIngestionEvent(message, "AccountEventProcessor");
        eventPublisher.publishEvent(event);
        log.info("🟪 Account data MessageProcessedEvent published successfully.");
    }
}
