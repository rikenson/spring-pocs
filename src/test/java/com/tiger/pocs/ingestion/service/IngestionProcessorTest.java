package com.tiger.pocs.ingestion.service;

import com.tiger.pocs.ingestion.domain.MessageProcessedEvent;
import com.tiger.pocs.ingestion.domain.ClientEntity;
import com.tiger.pocs.ingestion.service.EventTypeDetector.EventType;
import com.tiger.pocs.ingestion.support.IngestionTestParameterResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, IngestionTestParameterResolver.class})
class IngestionProcessorTest {

    @Mock
    private PayloadValidator payloadValidator;

    @Mock
    private EventTypeDetector eventTypeDetector;

    @Mock
    private PayloadConverter payloadConverter;

    @Mock
    private PersistenceService persistenceService;

    private IngestionProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new IngestionProcessor(
                payloadValidator,
                eventTypeDetector,
                payloadConverter,
                persistenceService
        );
    }

    @Test
    void shouldProcessKafkaEventSuccessfully(MessageProcessedEvent event, ClientEntity clientEntity) {
        // Given
        String payload = "{\"id\":1}";
        when(payloadValidator.validateAndExtractPayload(event)).thenReturn(payload);
        when(eventTypeDetector.detectEventType(event)).thenReturn(EventType.CLIENT);
        when(payloadConverter.convertToEntity(payload, EventType.CLIENT)).thenReturn(clientEntity);

        // When
        processor.processKafkaEvent(event);

        // Then
        InOrder inOrder = inOrder(payloadValidator, eventTypeDetector, payloadConverter, persistenceService);
        inOrder.verify(payloadValidator).validateAndExtractPayload(event);
        inOrder.verify(eventTypeDetector).detectEventType(event);
        inOrder.verify(payloadConverter).convertToEntity(payload, EventType.CLIENT);
        inOrder.verify(persistenceService).persistEntity(
                eq(clientEntity), 
                eq(EventType.CLIENT), 
                anyString(), 
                anyString()
        );
    }

    @Test
    void shouldSkipProcessingForInvalidPayload(MessageProcessedEvent event) {
        // Given
        when(payloadValidator.validateAndExtractPayload(event)).thenReturn(null);

        // When
        processor.processKafkaEvent(event);

        // Then
        verify(payloadValidator).validateAndExtractPayload(event);
        verify(eventTypeDetector, never()).detectEventType(any());
        verify(payloadConverter, never()).convertToEntity(any(), any());
        verify(persistenceService, never()).persistEntity(any(), any(), any(), any());
    }

    @Test
    void shouldHandleValidationException(MessageProcessedEvent event) {
        // Given
        RuntimeException validationError = new RuntimeException("Validation failed");
        when(payloadValidator.validateAndExtractPayload(event)).thenThrow(validationError);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            processor.processKafkaEvent(event));
        
        assertEquals("Validation failed", exception.getMessage());
        verify(payloadValidator).validateAndExtractPayload(event);
        verify(eventTypeDetector, never()).detectEventType(any());
    }

    @Test
    void shouldHandleEventTypeDetectionException(MessageProcessedEvent event) {
        // Given
        String payload = "{\"test\":\"data\"}";
        when(payloadValidator.validateAndExtractPayload(event)).thenReturn(payload);
        RuntimeException detectionError = new RuntimeException("Detection failed");
        when(eventTypeDetector.detectEventType(event)).thenThrow(detectionError);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            processor.processKafkaEvent(event));
        
        assertEquals("Detection failed", exception.getMessage());
        verify(payloadValidator).validateAndExtractPayload(event);
        verify(eventTypeDetector).detectEventType(event);
        verify(payloadConverter, never()).convertToEntity(any(), any());
    }

    @Test
    void shouldHandleConversionException(MessageProcessedEvent event) {
        // Given
        String payload = "{\"test\":\"data\"}";
        when(payloadValidator.validateAndExtractPayload(event)).thenReturn(payload);
        when(eventTypeDetector.detectEventType(event)).thenReturn(EventType.CLIENT);
        RuntimeException conversionError = new RuntimeException("Conversion failed");
        when(payloadConverter.convertToEntity(payload, EventType.CLIENT)).thenThrow(conversionError);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            processor.processKafkaEvent(event));
        
        assertEquals("Conversion failed", exception.getMessage());
        verify(payloadValidator).validateAndExtractPayload(event);
        verify(eventTypeDetector).detectEventType(event);
        verify(payloadConverter).convertToEntity(payload, EventType.CLIENT);
        verify(persistenceService, never()).persistEntity(any(), any(), any(), any());
    }

    @Test
    void shouldHandlePersistenceException(MessageProcessedEvent event, ClientEntity clientEntity) {
        // Given
        String payload = "{\"test\":\"data\"}";
        when(payloadValidator.validateAndExtractPayload(event)).thenReturn(payload);
        when(eventTypeDetector.detectEventType(event)).thenReturn(EventType.CLIENT);
        when(payloadConverter.convertToEntity(payload, EventType.CLIENT)).thenReturn(clientEntity);
        
        RuntimeException persistenceError = new RuntimeException("Persistence failed");
        doThrow(persistenceError).when(persistenceService).persistEntity(any(), any(), any(), any());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            processor.processKafkaEvent(event));
        
        assertEquals("Persistence failed", exception.getMessage());
        verify(payloadValidator).validateAndExtractPayload(event);
        verify(eventTypeDetector).detectEventType(event);
        verify(payloadConverter).convertToEntity(payload, EventType.CLIENT);
        verify(persistenceService).persistEntity(eq(clientEntity), eq(EventType.CLIENT), anyString(), anyString());
    }

    @Test
    void shouldHandleNullMessageInEvent() {
        // Given
        MessageProcessedEvent eventWithNullMessage = IngestionTestParameterResolver.createEventWithNullMessage();
        String payload = "test-payload";
        when(payloadValidator.validateAndExtractPayload(eventWithNullMessage)).thenReturn(payload);
        when(eventTypeDetector.detectEventType(eventWithNullMessage)).thenReturn(EventType.UNKNOWN);
        when(payloadConverter.convertToEntity(payload, EventType.UNKNOWN)).thenReturn("raw-data");

        // When
        processor.processKafkaEvent(eventWithNullMessage);

        // Then
        verify(payloadValidator).validateAndExtractPayload(eventWithNullMessage);
        verify(eventTypeDetector).detectEventType(eventWithNullMessage);
        verify(payloadConverter).convertToEntity(payload, EventType.UNKNOWN);
        verify(persistenceService).persistEntity(
                eq("raw-data"), 
                eq(EventType.UNKNOWN), 
                eq("unknown"), 
                eq("unknown")
        );
    }

    @Test
    void shouldProcessDifferentEventTypes(ClientEntity clientEntity) {
        // Test for different event types
        EventType[] eventTypes = {EventType.CLIENT, EventType.ACCOUNT, EventType.USER, EventType.USER_ACCESS, EventType.LOG_OFFSET};
        
        for (EventType eventType : eventTypes) {
            // Given
            MessageProcessedEvent event = IngestionTestParameterResolver.createEventWithHandler(eventType.name() + "EventProcessor");
            String payload = "{\"data\":\"test\"}";
            when(payloadValidator.validateAndExtractPayload(event)).thenReturn(payload);
            when(eventTypeDetector.detectEventType(event)).thenReturn(eventType);
            when(payloadConverter.convertToEntity(payload, eventType)).thenReturn(clientEntity);

            // When
            processor.processKafkaEvent(event);

            // Then
            verify(persistenceService).persistEntity(eq(clientEntity), eq(eventType), anyString(), anyString());
            
            // Reset mocks for next iteration
            reset(payloadValidator, eventTypeDetector, payloadConverter, persistenceService);
        }
    }

    @Test
    void shouldHandleEmptyPayload(MessageProcessedEvent event) {
        // Given - Empty string is still a valid payload, so processing continues
        String emptyPayload = "";
        when(payloadValidator.validateAndExtractPayload(event)).thenReturn(emptyPayload);
        when(eventTypeDetector.detectEventType(event)).thenReturn(EventType.CLIENT);
        when(payloadConverter.convertToEntity(emptyPayload, EventType.CLIENT)).thenReturn("empty");

        // When
        processor.processKafkaEvent(event);

        // Then
        verify(payloadValidator).validateAndExtractPayload(event);
        verify(eventTypeDetector).detectEventType(event);
        verify(payloadConverter).convertToEntity(emptyPayload, EventType.CLIENT);
        verify(persistenceService).persistEntity(eq("empty"), eq(EventType.CLIENT), anyString(), anyString());
    }

    @Test
    void shouldExtractCorrectEventContext(MessageProcessedEvent event, ClientEntity clientEntity) {
        // Given
        String payload = "{\"id\":1}";
        when(payloadValidator.validateAndExtractPayload(event)).thenReturn(payload);
        when(eventTypeDetector.detectEventType(event)).thenReturn(EventType.CLIENT);
        when(payloadConverter.convertToEntity(payload, EventType.CLIENT)).thenReturn(clientEntity);

        // When
        processor.processKafkaEvent(event);

        // Then
        verify(persistenceService).persistEntity(
                eq(clientEntity), 
                eq(EventType.CLIENT), 
                eq("clients"), // Should extract topic from event
                eq("test-key") // Should extract key from event
        );
    }

    @Test
    void shouldHandleNullEntityFromConverter(MessageProcessedEvent event) {
        // Given
        String payload = "{\"test\":\"data\"}";
        when(payloadValidator.validateAndExtractPayload(event)).thenReturn(payload);
        when(eventTypeDetector.detectEventType(event)).thenReturn(EventType.CLIENT);
        when(payloadConverter.convertToEntity(payload, EventType.CLIENT)).thenReturn(null);

        // When
        processor.processKafkaEvent(event);

        // Then
        verify(payloadValidator).validateAndExtractPayload(event);
        verify(eventTypeDetector).detectEventType(event);
        verify(payloadConverter).convertToEntity(payload, EventType.CLIENT);
        verify(persistenceService).persistEntity(isNull(), eq(EventType.CLIENT), anyString(), anyString());
    }
}