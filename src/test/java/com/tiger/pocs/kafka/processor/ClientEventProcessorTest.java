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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientEventProcessorTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;
    
    @Mock
    private KafkaEventConverter kafkaEventConverter;
    
    @Mock
    private MessageProcessedEvent messageProcessedEvent;
    
    private ClientEventProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new ClientEventProcessor(eventPublisher, kafkaEventConverter);
    }

    @Test
    void shouldProcessClientMessageSuccessfully() {
        // Given
        KafkaMessage<String> message = createKafkaMessage("client-key", "client-value", "clients-topic");
        when(kafkaEventConverter.convertToIngestionEvent(message, "ClientEventProcessor"))
                .thenReturn(messageProcessedEvent);

        // When
        processor.processClientMessage(message);

        // Then
        verify(kafkaEventConverter).convertToIngestionEvent(message, "ClientEventProcessor");
        verify(eventPublisher).publishEvent(messageProcessedEvent);
    }

    @Test
    void shouldHandleExceptionInClientEventProcessing() {
        // Given
        KafkaMessage<String> message = createKafkaMessage("client-key", "client-value", "clients-topic");
        when(kafkaEventConverter.convertToIngestionEvent(message, "ClientEventProcessor"))
                .thenThrow(new RuntimeException("Conversion failed"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            processor.processClientMessage(message));
        
        assertEquals("Conversion failed", exception.getMessage());
        verify(kafkaEventConverter).convertToIngestionEvent(message, "ClientEventProcessor");
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldHandleExceptionInEventPublishing() {
        // Given
        KafkaMessage<String> message = createKafkaMessage("client-key", "client-value", "clients-topic");
        when(kafkaEventConverter.convertToIngestionEvent(message, "ClientEventProcessor"))
                .thenReturn(messageProcessedEvent);
        doThrow(new RuntimeException("Publishing failed")).when(eventPublisher).publishEvent(messageProcessedEvent);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            processor.processClientMessage(message));
        
        assertEquals("Publishing failed", exception.getMessage());
        verify(kafkaEventConverter).convertToIngestionEvent(message, "ClientEventProcessor");
        verify(eventPublisher).publishEvent(messageProcessedEvent);
    }

    @Test
    void shouldHandleNullKey() {
        // Given
        KafkaMessage<String> message = createKafkaMessage(null, "client-value", "clients-topic");
        when(kafkaEventConverter.convertToIngestionEvent(message, "ClientEventProcessor"))
                .thenReturn(messageProcessedEvent);

        // When
        processor.processClientMessage(message);

        // Then
        verify(kafkaEventConverter).convertToIngestionEvent(message, "ClientEventProcessor");
        verify(eventPublisher).publishEvent(messageProcessedEvent);
    }

    @Test
    void shouldHandleEmptyValue() {
        // Given
        KafkaMessage<String> message = createKafkaMessage("client-key", "", "clients-topic");
        when(kafkaEventConverter.convertToIngestionEvent(message, "ClientEventProcessor"))
                .thenReturn(messageProcessedEvent);

        // When
        processor.processClientMessage(message);

        // Then
        verify(kafkaEventConverter).convertToIngestionEvent(message, "ClientEventProcessor");
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