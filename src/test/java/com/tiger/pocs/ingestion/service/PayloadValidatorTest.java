package com.tiger.pocs.ingestion.service;

import com.tiger.pocs.ingestion.domain.Message;
import com.tiger.pocs.ingestion.domain.MessageProcessedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class PayloadValidatorTest {

    private PayloadValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PayloadValidator();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void shouldReturnNullForInvalidPayloads(String payload) {
        MessageProcessedEvent event = createEventWithPayload(payload);
        String result = validator.validateAndExtractPayload(event);
        assertNull(result);
    }

    @ParameterizedTest
    @MethodSource("provideValidPayloads")
    void shouldValidateAndExtractValidPayload(String expectedPayload) {
        MessageProcessedEvent event = createEventWithPayload(expectedPayload);
        String result = validator.validateAndExtractPayload(event);
        assertEquals(expectedPayload, result);
    }

    private static Stream<Arguments> provideValidPayloads() {
        return Stream.of(
                Arguments.of("{\"id\":1}"),
                Arguments.of("12345"),
                Arguments.of("a"),
                Arguments.of("{\"message\":\"Hello! @#$%^&*()_+ Special chars: ñáéíóú\"}"),
                Arguments.of("x".repeat(10000))
        );
    }

    @Test
    void shouldThrowExceptionForNullMessage() {
        MessageProcessedEvent event = createEventWithNullMessage();
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateAndExtractPayload(event)
        );
        
        assertEquals("Kafka message cannot be null", exception.getMessage());
    }

    @Test
    void shouldHandleComplexJsonPayload() {
        String complexPayload = """
                {
                    "id": 1,
                    "details": {
                        "address": "123 Main St",
                        "contacts": ["email@test.com", "phone123"]
                    },
                    "active": true,
                    "balance": 1500.75
                }
                """;
        MessageProcessedEvent event = createEventWithPayload(complexPayload);
        String result = validator.validateAndExtractPayload(event);
        assertEquals(complexPayload, result);
    }

    @Test
    void shouldHandleNullMessageValue() {
        // Given - create a message with null value
        var message = Message.<String>builder()
                .key("test-key")
                .value(null) // Null value
                .topic("test-topic")
                .partition(0)
                .offset(100L)
                .timestamp(java.time.Instant.now())
                .headers(java.util.Map.of())
                .build();

        var event = MessageProcessedEvent.builder()
                .message(message)
                .handlerName("TestHandler")
                .processingTime(0L)
                .processedBy("test")
                .build();

        // When
        String result = validator.validateAndExtractPayload(event);
        assertNull(result);
    }

    private static MessageProcessedEvent createEventWithPayload(String payload) {
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
    
    private static MessageProcessedEvent createEventWithNullMessage() {
        return MessageProcessedEvent.builder()
                .message(null)
                .handlerName("TestHandler")
                .processingTime(0L)
                .processedBy("test")
                .build();
    }
}