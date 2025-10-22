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
class LogOffsetEventProcessorTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;
    
    @Mock
    private KafkaEventConverter kafkaEventConverter;
    
    @Mock
    private MessageProcessedEvent messageProcessedEvent;
    
    private LogOffsetEventProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new LogOffsetEventProcessor(eventPublisher, kafkaEventConverter);
    }

    @Test
    void shouldProcessLogOffsetMessageSuccessfully() {
        // Given
        KafkaMessage<String> message = createKafkaMessage("log-key", "log-value", "log-offset-topic");
        when(kafkaEventConverter.convertToIngestionEvent(message, "LogOffsetEventProcessor"))
                .thenReturn(messageProcessedEvent);

        // When
        processor.processLogOffsetMessage(message);

        // Then
        verify(kafkaEventConverter).convertToIngestionEvent(message, "LogOffsetEventProcessor");
        verify(eventPublisher).publishEvent(messageProcessedEvent);
    }

    @Test
    void shouldHandleExceptionInLogOffsetEventProcessing() {
        // Given
        KafkaMessage<String> message = createKafkaMessage("log-key", "log-value", "log-offset-topic");
        when(kafkaEventConverter.convertToIngestionEvent(message, "LogOffsetEventProcessor"))
                .thenThrow(new RuntimeException("Conversion failed"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            processor.processLogOffsetMessage(message));
        
        assertEquals("Conversion failed", exception.getMessage());
        verify(kafkaEventConverter).convertToIngestionEvent(message, "LogOffsetEventProcessor");
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldHandleExceptionInEventPublishing() {
        // Given
        KafkaMessage<String> message = createKafkaMessage("log-key", "log-value", "log-offset-topic");
        when(kafkaEventConverter.convertToIngestionEvent(message, "LogOffsetEventProcessor"))
                .thenReturn(messageProcessedEvent);
        doThrow(new RuntimeException("Publishing failed")).when(eventPublisher).publishEvent(messageProcessedEvent);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            processor.processLogOffsetMessage(message));
        
        assertEquals("Publishing failed", exception.getMessage());
        verify(kafkaEventConverter).convertToIngestionEvent(message, "LogOffsetEventProcessor");
        verify(eventPublisher).publishEvent(messageProcessedEvent);
    }

    @Test
    void shouldHandleNullKey() {
        // Given
        KafkaMessage<String> message = createKafkaMessage(null, "log-value", "log-offset-topic");
        when(kafkaEventConverter.convertToIngestionEvent(message, "LogOffsetEventProcessor"))
                .thenReturn(messageProcessedEvent);

        // When
        processor.processLogOffsetMessage(message);

        // Then
        verify(kafkaEventConverter).convertToIngestionEvent(message, "LogOffsetEventProcessor");
        verify(eventPublisher).publishEvent(messageProcessedEvent);
    }

    @Test
    void shouldHandleEmptyValue() {
        // Given
        KafkaMessage<String> message = createKafkaMessage("log-key", "", "log-offset-topic");
        when(kafkaEventConverter.convertToIngestionEvent(message, "LogOffsetEventProcessor"))
                .thenReturn(messageProcessedEvent);

        // When
        processor.processLogOffsetMessage(message);

        // Then
        verify(kafkaEventConverter).convertToIngestionEvent(message, "LogOffsetEventProcessor");
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