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
class AccountEventProcessorTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;
    
    @Mock
    private KafkaEventConverter kafkaEventConverter;
    
    @Mock
    private MessageProcessedEvent messageProcessedEvent;
    
    private AccountEventProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new AccountEventProcessor(eventPublisher, kafkaEventConverter);
    }

    @Test
    void shouldProcessAccountMessageSuccessfully() {
        // Given
        KafkaMessage<String> message = createKafkaMessage("account-key", "account-value", "accounts-topic");
        when(kafkaEventConverter.convertToIngestionEvent(message, "AccountEventProcessor"))
                .thenReturn(messageProcessedEvent);

        // When
        processor.processAccountMessage(message);

        // Then
        verify(kafkaEventConverter).convertToIngestionEvent(message, "AccountEventProcessor");
        verify(eventPublisher).publishEvent(messageProcessedEvent);
    }

    @Test
    void shouldHandleExceptionInAccountEventProcessing() {
        // Given
        KafkaMessage<String> message = createKafkaMessage("account-key", "account-value", "accounts-topic");
        when(kafkaEventConverter.convertToIngestionEvent(message, "AccountEventProcessor"))
                .thenThrow(new RuntimeException("Conversion failed"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            processor.processAccountMessage(message));
        
        assertEquals("Conversion failed", exception.getMessage());
        verify(kafkaEventConverter).convertToIngestionEvent(message, "AccountEventProcessor");
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldHandleExceptionInEventPublishing() {
        // Given
        KafkaMessage<String> message = createKafkaMessage("account-key", "account-value", "accounts-topic");
        when(kafkaEventConverter.convertToIngestionEvent(message, "AccountEventProcessor"))
                .thenReturn(messageProcessedEvent);
        doThrow(new RuntimeException("Publishing failed")).when(eventPublisher).publishEvent(messageProcessedEvent);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            processor.processAccountMessage(message));
        
        assertEquals("Publishing failed", exception.getMessage());
        verify(kafkaEventConverter).convertToIngestionEvent(message, "AccountEventProcessor");
        verify(eventPublisher).publishEvent(messageProcessedEvent);
    }

    @Test
    void shouldHandleNullKey() {
        // Given
        KafkaMessage<String> message = createKafkaMessage(null, "account-value", "accounts-topic");
        when(kafkaEventConverter.convertToIngestionEvent(message, "AccountEventProcessor"))
                .thenReturn(messageProcessedEvent);

        // When
        processor.processAccountMessage(message);

        // Then
        verify(kafkaEventConverter).convertToIngestionEvent(message, "AccountEventProcessor");
        verify(eventPublisher).publishEvent(messageProcessedEvent);
    }

    @Test
    void shouldHandleEmptyValue() {
        // Given
        KafkaMessage<String> message = createKafkaMessage("account-key", "", "accounts-topic");
        when(kafkaEventConverter.convertToIngestionEvent(message, "AccountEventProcessor"))
                .thenReturn(messageProcessedEvent);

        // When
        processor.processAccountMessage(message);

        // Then
        verify(kafkaEventConverter).convertToIngestionEvent(message, "AccountEventProcessor");
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