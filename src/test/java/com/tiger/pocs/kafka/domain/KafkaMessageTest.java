package com.tiger.pocs.kafka.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KafkaMessageTest {

    @Test
    void shouldCreateKafkaMessageWithBuilder() {
        // Given
        String key = "test-key";
        String value = "test-value";
        String topic = "test-topic";
        int partition = 1;
        long offset = 12345L;
        Instant timestamp = Instant.now();
        Map<String, String> headers = Map.of("header1", "value1", "header2", "value2");

        // When
        KafkaMessage<String> message = KafkaMessage.<String>builder()
                .key(key)
                .value(value)
                .topic(topic)
                .partition(partition)
                .offset(offset)
                .timestamp(timestamp)
                .headers(headers)
                .build();

        // Then
        assertEquals(key, message.getKey());
        assertEquals(value, message.getValue());
        assertEquals(topic, message.getTopic());
        assertEquals(partition, message.getPartition());
        assertEquals(offset, message.getOffset());
        assertEquals(timestamp, message.getTimestamp());
        assertEquals(headers, message.getHeaders());
    }

    @Test
    void shouldCreateKafkaMessageWithNullValues() {
        // When
        KafkaMessage<String> message = KafkaMessage.<String>builder()
                .key(null)
                .value(null)
                .topic("test-topic")
                .partition(0)
                .offset(0L)
                .timestamp(null)
                .headers(null)
                .build();

        // Then
        assertNull(message.getKey());
        assertNull(message.getValue());
        assertEquals("test-topic", message.getTopic());
        assertEquals(0, message.getPartition());
        assertEquals(0L, message.getOffset());
        assertNull(message.getTimestamp());
        assertNull(message.getHeaders());
    }

    @Test
    void shouldCreateKafkaMessageWithDifferentValueType() {
        // Given
        Integer integerValue = 42;
        
        // When
        KafkaMessage<Integer> message = KafkaMessage.<Integer>builder()
                .key("numeric-key")
                .value(integerValue)
                .topic("numeric-topic")
                .partition(2)
                .offset(999L)
                .timestamp(Instant.ofEpochMilli(1234567890L))
                .headers(Map.of("type", "integer"))
                .build();

        // Then
        assertEquals("numeric-key", message.getKey());
        assertEquals(integerValue, message.getValue());
        assertEquals("numeric-topic", message.getTopic());
        assertEquals(2, message.getPartition());
        assertEquals(999L, message.getOffset());
        assertEquals(Instant.ofEpochMilli(1234567890L), message.getTimestamp());
        assertEquals("integer", message.getHeaders().get("type"));
    }

    @Test
    void shouldSupportEqualsAndHashCode() {
        // Given
        Instant timestamp = Instant.ofEpochMilli(1000000L);
        Map<String, String> headers = Map.of("test", "header");
        
        KafkaMessage<String> message1 = KafkaMessage.<String>builder()
                .key("same-key")
                .value("same-value")
                .topic("same-topic")
                .partition(1)
                .offset(100L)
                .timestamp(timestamp)
                .headers(headers)
                .build();

        KafkaMessage<String> message2 = KafkaMessage.<String>builder()
                .key("same-key")
                .value("same-value")
                .topic("same-topic")
                .partition(1)
                .offset(100L)
                .timestamp(timestamp)
                .headers(headers)
                .build();

        // Then
        assertEquals(message1, message2);
        assertEquals(message1.hashCode(), message2.hashCode());
    }

    @Test
    void shouldSupportToString() {
        // Given
        KafkaMessage<String> message = KafkaMessage.<String>builder()
                .key("test-key")
                .value("test-value")
                .topic("test-topic")
                .partition(0)
                .offset(123L)
                .timestamp(Instant.ofEpochMilli(1000L))
                .headers(Map.of("test", "header"))
                .build();

        // When
        String toString = message.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("test-key"));
        assertTrue(toString.contains("test-value"));
        assertTrue(toString.contains("test-topic"));
        assertTrue(toString.contains("123"));
    }

    @Test
    void shouldAllowMutableOperations() {
        // Given
        KafkaMessage<String> message = KafkaMessage.<String>builder()
                .key("original-key")
                .value("original-value")
                .topic("original-topic")
                .partition(0)
                .offset(100L)
                .timestamp(Instant.now())
                .headers(Map.of("original", "header"))
                .build();

        // When
        message.setKey("updated-key");
        message.setValue("updated-value");
        message.setTopic("updated-topic");
        message.setPartition(1);
        message.setOffset(200L);
        Instant newTimestamp = Instant.ofEpochMilli(2000L);
        message.setTimestamp(newTimestamp);
        Map<String, String> newHeaders = Map.of("updated", "header");
        message.setHeaders(newHeaders);

        // Then
        assertEquals("updated-key", message.getKey());
        assertEquals("updated-value", message.getValue());
        assertEquals("updated-topic", message.getTopic());
        assertEquals(1, message.getPartition());
        assertEquals(200L, message.getOffset());
        assertEquals(newTimestamp, message.getTimestamp());
        assertEquals(newHeaders, message.getHeaders());
    }
}