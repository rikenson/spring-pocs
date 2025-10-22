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
public class ClientEventProcessor {

    private final ApplicationEventPublisher eventPublisher;
    private final KafkaEventConverter kafkaEventConverter;

    public void processClientMessage(KafkaMessage<String> message) {
        log.info("🟦 [CLIENT PROCESSOR] Processing client message: key={}, topic={}", message.getKey(), message.getTopic());
        try {
            handleClientEvent(message);
            log.info("🟦 [CLIENT PROCESSOR] Successfully processed client message");
        } catch (Exception e) {
            log.error("❌ [CLIENT PROCESSOR] Failed to process client message: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void handleClientEvent(KafkaMessage<String> message) {
        log.info("🟦 Consuming client data ...");
        
        var event = kafkaEventConverter.convertToIngestionEvent(message, "ClientEventProcessor");
        eventPublisher.publishEvent(event);
        log.info("🟦 Client data MessageProcessedEvent published successfully.");
    }
}