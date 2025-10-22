package com.tiger.pocs.kafka.domain;

import com.tiger.pocs.ingestion.domain.MessageProcessedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KafkaEventConverterTest {

    private KafkaEventConverter converter;

    @BeforeEach
    void setUp() {
        converter = new KafkaEventConverter();
    }

    @Test
    void shouldConvertKafkaMessageToIngestionEvent() {
        // Given
        Map<String, String> headers = Map.of(
                "content-type", "application/json",
                "source", "kafka"
        );
        
        KafkaMessage<String> kafkaMessage = KafkaMessage.<String>builder()
                .key("test-key")
                .value("test-value")
                .topic("test-topic")
                .partition(1)
                .offset(123L)
                .timestamp(Instant.ofEpochMilli(1234567890L))
                .headers(headers)
                .build();

        // When
        MessageProcessedEvent result = converter.convertToIngestionEvent(kafkaMessage, "test-handler");

        // Then
        assertNotNull(result);
        assertEquals("test-handler", result.getHandlerName());
        assertEquals("kafka-module", result.getProcessedBy());
        assertNotNull(result.getProcessingTime());
        
        // Check message conversion
        assertNotNull(result.getMessage());
        assertEquals("test-key", result.getMessage().getKey());
        assertEquals("test-value", result.getMessage().getValue());
        assertEquals("test-topic", result.getMessage().getTopic());
        assertEquals(1, result.getMessage().getPartition());
        assertEquals(123L, result.getMessage().getOffset());
        assertEquals(Instant.ofEpochMilli(1234567890L), result.getMessage().getTimestamp());
        
        // Check headers
        assertNotNull(result.getMessage().getHeaders());
        assertEquals("application/json", result.getMessage().getHeaders().get("content-type"));
        assertEquals("kafka", result.getMessage().getHeaders().get("source"));
    }

    @Test
    void shouldHandleHeadersWithDotsInKeys() {
        // Given
        Map<String, String> headers = Map.of(
                "content.type", "application/json",
                "kafka.source.id", "test-source",
                "normal-header", "normal-value"
        );
        
        KafkaMessage<String> kafkaMessage = KafkaMessage.<String>builder()
                .key("test-key")
                .value("test-value")
                .topic("test-topic")
                .partition(0)
                .offset(456L)
                .timestamp(Instant.now())
                .headers(headers)
                .build();

        // When
        MessageProcessedEvent result = converter.convertToIngestionEvent(kafkaMessage, "dot-handler");

        // Then
        assertNotNull(result.getMessage().getHeaders());
        
        // Dots should be replaced with underscores
        assertEquals("application/json", result.getMessage().getHeaders().get("content_type"));
        assertEquals("test-source", result.getMessage().getHeaders().get("kafka_source_id"));
        assertEquals("normal-value", result.getMessage().getHeaders().get("normal-header"));
        
        // Original keys with dots should not exist
        assertFalse(result.getMessage().getHeaders().containsKey("content.type"));
        assertFalse(result.getMessage().getHeaders().containsKey("kafka.source.id"));
    }

    @Test
    void shouldHandleNullHeaders() {
        // Given
        KafkaMessage<String> kafkaMessage = KafkaMessage.<String>builder()
                .key("test-key")
                .value("test-value")
                .topic("test-topic")
                .partition(0)
                .offset(789L)
                .timestamp(Instant.now())
                .headers(null)
                .build();

        // When
        MessageProcessedEvent result = converter.convertToIngestionEvent(kafkaMessage, "null-headers-handler");

        // Then
        assertNotNull(result.getMessage().getHeaders());
        assertTrue(result.getMessage().getHeaders().isEmpty());
    }

    @Test
    void shouldHandleEmptyHeaders() {
        // Given
        KafkaMessage<String> kafkaMessage = KafkaMessage.<String>builder()
                .key("test-key")
                .value("test-value")
                .topic("test-topic")
                .partition(0)
                .offset(999L)
                .timestamp(Instant.now())
                .headers(Map.of())
                .build();

        // When
        MessageProcessedEvent result = converter.convertToIngestionEvent(kafkaMessage, "empty-headers-handler");

        // Then
        assertNotNull(result.getMessage().getHeaders());
        assertTrue(result.getMessage().getHeaders().isEmpty());
    }

    @Test
    void shouldHandleNullValues() {
        // Given
        KafkaMessage<String> kafkaMessage = KafkaMessage.<String>builder()
                .key(null)
                .value(null)
                .topic("test-topic")
                .partition(0)
                .offset(111L)
                .timestamp(Instant.now())
                .headers(Map.of("test", "value"))
                .build();

        // When
        MessageProcessedEvent result = converter.convertToIngestionEvent(kafkaMessage, "null-values-handler");

        // Then
        assertNotNull(result);
        assertNull(result.getMessage().getKey());
        assertNull(result.getMessage().getValue());
        assertEquals("test-topic", result.getMessage().getTopic());
    }

    @Test
    void shouldSetProcessingTimeToCurrentTime() {
        // Given
        long beforeConversion = System.currentTimeMillis();
        
        KafkaMessage<String> kafkaMessage = KafkaMessage.<String>builder()
                .key("test-key")
                .value("test-value")
                .topic("test-topic")
                .partition(0)
                .offset(222L)
                .timestamp(Instant.now())
                .headers(Map.of())
                .build();

        // When
        MessageProcessedEvent result = converter.convertToIngestionEvent(kafkaMessage, "timing-handler");
        
        long afterConversion = System.currentTimeMillis();

        // Then
        assertTrue(result.getProcessingTime() >= beforeConversion);
        assertTrue(result.getProcessingTime() <= afterConversion);
    }

    @Test
    void shouldPreserveAllMessageFields() {
        // Given
        Instant timestamp = Instant.ofEpochMilli(1640995200000L); // 2022-01-01T00:00:00Z
        
        KafkaMessage<String> kafkaMessage = KafkaMessage.<String>builder()
                .key("preserve-key")
                .value("preserve-value")
                .topic("preserve-topic")
                .partition(5)
                .offset(987654321L)
                .timestamp(timestamp)
                .headers(Map.of("preserve", "header"))
                .build();

        // When
        MessageProcessedEvent result = converter.convertToIngestionEvent(kafkaMessage, "preserve-handler");

        // Then
        assertEquals("preserve-key", result.getMessage().getKey());
        assertEquals("preserve-value", result.getMessage().getValue());
        assertEquals("preserve-topic", result.getMessage().getTopic());
        assertEquals(5, result.getMessage().getPartition());
        assertEquals(987654321L, result.getMessage().getOffset());
        assertEquals(timestamp, result.getMessage().getTimestamp());
        assertEquals("header", result.getMessage().getHeaders().get("preserve"));
    }
}