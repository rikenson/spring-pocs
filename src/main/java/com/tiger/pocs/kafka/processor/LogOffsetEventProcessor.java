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
public class LogOffsetEventProcessor {


    private final ApplicationEventPublisher eventPublisher;
    private final KafkaEventConverter kafkaEventConverter;

    public void processLogOffsetMessage(KafkaMessage<String> message) {
        log.info("🟨 [LOG OFFSET PROCESSOR] Processing log offset message: key={}, topic={}", message.getKey(), message.getTopic());

        try {
            handleLogOffsetEvent(message);
            log.info("🟨 [LOG OFFSET PROCESSOR] Successfully processed log offset message");
        } catch (Exception e) {
            log.error("❌ [LOG OFFSET PROCESSOR] Failed to process log offset message: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void handleLogOffsetEvent(KafkaMessage<String> message) {
        log.info("🟨 Processing log offset data ...");
        
        var event = kafkaEventConverter.convertToIngestionEvent(message, "LogOffsetEventProcessor");
        eventPublisher.publishEvent(event);
        log.info("🟨 Log offset data MessageProcessedEvent published successfully.");
    }
}