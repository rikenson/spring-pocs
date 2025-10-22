package com.tiger.pocs.ingestion.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiger.pocs.ingestion.domain.Message;
import com.tiger.pocs.ingestion.domain.MessageProcessedEvent;
import com.tiger.pocs.ingestion.domain.*;
import com.tiger.pocs.kafka.domain.KafkaMessage;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.time.Instant;
import java.util.Map;

/**
 * Parameter resolver pour les tests du module ingestion.
 * Fournit des instances pré-configurées pour les tests.
 */
public class IngestionTestParameterResolver implements ParameterResolver {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> type = parameterContext.getParameter().getType();
        return type == ObjectMapper.class ||
                type == MessageProcessedEvent.class ||
                type == KafkaMessage.class ||
                type == ClientEntity.class ||
                type == AccountEntity.class ||
                type == UserEntity.class ||
                type == UserAccessEntity.class ||
                type == LogOffsetEntity.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> type = parameterContext.getParameter().getType();

        return switch (type.getSimpleName()) {
            case "ObjectMapper" -> objectMapper;
            case "MessageProcessedEvent" -> createMessageProcessedEvent();
            case "KafkaMessage" -> createKafkaMessage();
            case "ClientEntity" -> createClientEntity();
            case "AccountEntity" -> createAccountEntity();
            case "UserEntity" -> createUserEntity();
            case "UserAccessEntity" -> createUserAccessEntity();
            case "LogOffsetEntity" -> createLogOffsetEntity();
            default -> throw new IllegalArgumentException("Unsupported parameter type: " + type);
        };
    }

    private MessageProcessedEvent createMessageProcessedEvent() {
        Message<String> message = createMessage();
        return MessageProcessedEvent.builder()
                .message(message)
                .handlerName("ClientEventProcessor")
                .processingTime(0L)
                .processedBy("test")
                .build();
    }

    private KafkaMessage<String> createKafkaMessage() {
        return KafkaMessage.<String>builder()
                .key("test-key")
                .value("{\"id\":\"CLI-001\"}")
                .topic("clients")
                .partition(0)
                .offset(100L)
                .timestamp(Instant.now())
                .headers(Map.of())
                .build();
    }

    private Message<String> createMessage() {
        return Message.<String>builder()
                .key("test-key")
                .value("{\"id\":\"CLI-001\"}")
                .topic("clients")
                .partition(0)
                .offset(100L)
                .timestamp(Instant.now())
                .headers(Map.of())
                .build();
    }

    private ClientEntity createClientEntity() {
        return ClientEntity.builder()
                .id("CLI-001")
                .build();
    }

    private AccountEntity createAccountEntity() {
        return AccountEntity.builder()
                .id("ACC001")
                .build();
    }

    private UserEntity createUserEntity() {
        return UserEntity.builder()
                .id("USER001")
                .build();
    }

    private UserAccessEntity createUserAccessEntity() {
        return UserAccessEntity.builder()
                .id("UA001")
                .build();
    }

    private LogOffsetEntity createLogOffsetEntity() {
        return LogOffsetEntity.builder()
                .id("LOG001")
                .build();
    }

    // Méthodes utilitaires pour créer des variations des objets

    public static MessageProcessedEvent createEventWithHandler(String handlerName) {
        Message<String> message = Message.<String>builder()
                .key("test-key")
                .value("{\"data\":\"test\"}")
                .topic("test-topic")
                .partition(0)
                .offset(100L)
                .timestamp(Instant.now())
                .headers(Map.of())
                .build();

        return MessageProcessedEvent.builder()
                .message(message)
                .handlerName(handlerName)
                .processingTime(0L)
                .processedBy("test")
                .build();
    }

    public static MessageProcessedEvent createEventWithTopic(String topic) {
        Message<String> message = Message.<String>builder()
                .key("test-key")
                .value("{\"data\":\"test\"}")
                .topic(topic)
                .partition(0)
                .offset(100L)
                .timestamp(Instant.now())
                .headers(Map.of())
                .build();

        return MessageProcessedEvent.builder()
                .message(message)
                .handlerName("TestHandler")
                .processingTime(0L)
                .processedBy("test")
                .build();
    }

    public static MessageProcessedEvent createEventWithPayload(String payload) {
        Message<String> message = Message.<String>builder()
                .key("test-key")
                .value(payload)
                .topic("test-topic")
                .partition(0)
                .offset(100L)
                .timestamp(Instant.now())
                .headers(Map.of())
                .build();

        return MessageProcessedEvent.builder()
                .message(message)
                .handlerName("TestHandler")
                .processingTime(0L)
                .processedBy("test")
                .build();
    }

    public static MessageProcessedEvent createEventWithNullMessage() {
        return MessageProcessedEvent.builder()
                .message(null)
                .handlerName("TestHandler")
                .processingTime(0L)
                .processedBy("test")
                .build();
    }
}