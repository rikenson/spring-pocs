package com.tiger.pocs.kafka.processor;

import com.tiger.pocs.kafka.domain.KafkaMessage;
import com.tiger.pocs.kafka.domain.KafkaEventConverter;
import com.tiger.pocs.ingestion.domain.MessageProcessedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserEventProcessorTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;
    
    @Mock
    private KafkaEventConverter kafkaEventConverter;
    
    @Mock
    private MessageProcessedEvent messageProcessedEvent;
    
    private UserEventProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new UserEventProcessor(eventPublisher, kafkaEventConverter);
    }

    @Test
    void shouldProcessUserMessageSuccessfully() {
        // Given
        KafkaMessage<String> message = createKafkaMessage("user-key", "user-value", "users-topic");
        when(kafkaEventConverter.convertToIngestionEvent(message, "UserEventProcessor"))
                .thenReturn(messageProcessedEvent);

        // When
        processor.processUserMessage(message);

        // Then
        verify(kafkaEventConverter).convertToIngestionEvent(message, "UserEventProcessor");
        verify(eventPublisher).publishEvent(messageProcessedEvent);
    }

    @Test
    void shouldHandleExceptionInUserEventProcessing() {
        // Given
        KafkaMessage<String> message = createKafkaMessage("user-key", "user-value", "users-topic");
        when(kafkaEventConverter.convertToIngestionEvent(message, "UserEventProcessor"))
                .thenThrow(new RuntimeException("Conversion failed"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            processor.processUserMessage(message));
        
        assertEquals("Conversion failed", exception.getMessage());
        verify(kafkaEventConverter).convertToIngestionEvent(message, "UserEventProcessor");
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldHandleExceptionInEventPublishing() {
        // Given
        KafkaMessage<String> message = createKafkaMessage("user-key", "user-value", "users-topic");
        when(kafkaEventConverter.convertToIngestionEvent(message, "UserEventProcessor"))
                .thenReturn(messageProcessedEvent);
        doThrow(new RuntimeException("Publishing failed")).when(eventPublisher).publishEvent(messageProcessedEvent);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            processor.processUserMessage(message));
        
        assertEquals("Publishing failed", exception.getMessage());
        verify(kafkaEventConverter).convertToIngestionEvent(message, "UserEventProcessor");
        verify(eventPublisher).publishEvent(messageProcessedEvent);
    }

    @Test
    void shouldHandleNullKey() {
        // Given
        KafkaMessage<String> message = createKafkaMessage(null, "user-value", "users-topic");
        when(kafkaEventConverter.convertToIngestionEvent(message, "UserEventProcessor"))
                .thenReturn(messageProcessedEvent);

        // When
        processor.processUserMessage(message);

        // Then
        verify(kafkaEventConverter).convertToIngestionEvent(message, "UserEventProcessor");
        verify(eventPublisher).publishEvent(messageProcessedEvent);
    }

    @Test
    void shouldHandleEmptyValue() {
        // Given
        KafkaMessage<String> message = createKafkaMessage("user-key", "", "users-topic");
        when(kafkaEventConverter.convertToIngestionEvent(message, "UserEventProcessor"))
                .thenReturn(messageProcessedEvent);

        // When
        processor.processUserMessage(message);

        // Then
        verify(kafkaEventConverter).convertToIngestionEvent(message, "UserEventProcessor");
        verify(eventPublisher).publishEvent(messageProcessedEvent);
    }

    private KafkaMessage<String> createKafkaMessage(String key, String value, String topic) {
        return KafkaMessage.<String>builder()
                .key(key)
                .value(value)
                .topic(topic)
                .partition(0)
                .offset(100L)
                .timestamp(Instant.now())
                .headers(Map.of())
                .build();
    }
}