package com.tiger.pocs.ingestion.service;

import com.tiger.pocs.ingestion.domain.Message;
import com.tiger.pocs.ingestion.domain.MessageProcessedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class PayloadValidator {

    public String validateAndExtractPayload(MessageProcessedEvent event) {
        var context = extractMessageContext(event);
        var kafkaMessage = validateKafkaMessage(event, context);
        var payload = extractPayload(kafkaMessage);

        return validatePayloadContent(payload, context);
    }

    /**
     * Extracts message context information for logging purposes.
     */
    private MessageContext extractMessageContext(MessageProcessedEvent event) {
        var kafkaMessage = event.getMessage();
        String topic = kafkaMessage != null ? kafkaMessage.getTopic() : "unknown";
        String messageKey = kafkaMessage != null ? kafkaMessage.getKey() : "unknown";

        return new MessageContext(topic, messageKey);
    }

    /**
     * Validates that the Kafka message exists and is not null.
     */
    private Message<?> validateKafkaMessage(MessageProcessedEvent event, MessageContext context) {
        var kafkaMessage = event.getMessage();

        if (kafkaMessage == null) {
            log.error("❌ VALIDATOR: Kafka message is null for event - topic: {}, key: {}",
                    context.topic(), context.messageKey());
            throw new IllegalArgumentException("Kafka message cannot be null");
        }

        log.debug("VALIDATOR: Kafka message validation passed for topic: {} (key: {})",
                context.topic(), context.messageKey());
        return kafkaMessage;
    }

    /**
     * Extracts the payload from the Kafka message value.
     */
    private String extractPayload(Message<?> kafkaMessage) {
        return kafkaMessage.getValue() != null ? kafkaMessage.getValue().toString() : null;
    }

    /**
     * Validates the payload content and determines if processing should continue.
     */
    private String validatePayloadContent(String payload, MessageContext context) {
        if (payload == null || payload.trim().isEmpty()) {
            log.warn("⚠️ VALIDATOR: Empty payload for topic: {} (key: {}) - skipping processing",
                    context.topic(), context.messageKey());
            return null; // Return null to indicate skip processing
        }

        log.debug("✅ VALIDATOR: Valid payload extracted for topic: {} (key: {}) - length: {} chars",
                context.topic(), context.messageKey(), payload.length());
        return payload;
    }

    /**
     * Simple record to hold message context information for logging.
     */
    private record MessageContext(String topic, String messageKey) {
    }
}