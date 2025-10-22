package com.tiger.pocs.ingestion.service;

import com.tiger.pocs.ingestion.domain.MessageProcessedEvent;
import com.tiger.pocs.ingestion.support.IngestionTestParameterResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, IngestionTestParameterResolver.class})
class EventTypeDetectorTest {

    @Mock(lenient = true)
    private EventTypeMapping topicMapping;
    
    private EventTypeDetector detector;

    @BeforeEach
    void setUp() {
        detector = new EventTypeDetector(topicMapping);
        setupMockTopicMappings();
    }
    
    private void setupMockTopicMappings() {
        // Setup the mock to return appropriate event types for topics
        when(topicMapping.getEventTypeForTopic("clients")).thenReturn(EventTypeDetector.EventType.CLIENT);
        when(topicMapping.getEventTypeForTopic("accounts")).thenReturn(EventTypeDetector.EventType.ACCOUNT);
        when(topicMapping.getEventTypeForTopic("users")).thenReturn(EventTypeDetector.EventType.USER);
        when(topicMapping.getEventTypeForTopic("useraccess")).thenReturn(EventTypeDetector.EventType.USER_ACCESS);
        when(topicMapping.getEventTypeForTopic("logoffset")).thenReturn(EventTypeDetector.EventType.LOG_OFFSET);
        when(topicMapping.getEventTypeForTopic("CLIENTS")).thenReturn(EventTypeDetector.EventType.CLIENT);
        when(topicMapping.getEventTypeForTopic("unknown-topic")).thenReturn(EventTypeDetector.EventType.UNKNOWN);
        when(topicMapping.getEventTypeForTopic(null)).thenReturn(EventTypeDetector.EventType.UNKNOWN);
        when(topicMapping.getEventTypeForTopic("custom-topic")).thenReturn(EventTypeDetector.EventType.CLIENT);
    }


    @Test
    void shouldDetectClientFromTopic() {
        MessageProcessedEvent event = IngestionTestParameterResolver.createEventWithTopic("clients");
        EventTypeDetector.EventType result = detector.detectEventType(event);
        assertEquals(EventTypeDetector.EventType.CLIENT, result);
    }

    @Test
    void shouldDetectAccountFromTopic() {
        MessageProcessedEvent event = IngestionTestParameterResolver.createEventWithTopic("accounts");
        EventTypeDetector.EventType result = detector.detectEventType(event);
        assertEquals(EventTypeDetector.EventType.ACCOUNT, result);
    }

    @Test
    void shouldDetectUserFromTopic() {
        MessageProcessedEvent event = IngestionTestParameterResolver.createEventWithTopic("users");
        EventTypeDetector.EventType result = detector.detectEventType(event);
        assertEquals(EventTypeDetector.EventType.USER, result);
    }

    @Test
    void shouldDetectUserAccessFromTopic() {
        MessageProcessedEvent event = IngestionTestParameterResolver.createEventWithTopic("useraccess");
        EventTypeDetector.EventType result = detector.detectEventType(event);
        assertEquals(EventTypeDetector.EventType.USER_ACCESS, result);
    }

    @Test
    void shouldDetectLogOffsetFromTopic() {
        MessageProcessedEvent event = IngestionTestParameterResolver.createEventWithTopic("logoffset");
        EventTypeDetector.EventType result = detector.detectEventType(event);
        assertEquals(EventTypeDetector.EventType.LOG_OFFSET, result);
    }


    @Test
    void shouldReturnUnknownForUnrecognizedTopic() {
        MessageProcessedEvent event = IngestionTestParameterResolver.createEventWithTopic("unknown-topic");
        EventTypeDetector.EventType result = detector.detectEventType(event);
        assertEquals(EventTypeDetector.EventType.UNKNOWN, result);
    }


    @Test
    void shouldReturnUnknownForNullTopic() {
        MessageProcessedEvent event = IngestionTestParameterResolver.createEventWithTopic(null);
        EventTypeDetector.EventType result = detector.detectEventType(event);
        assertEquals(EventTypeDetector.EventType.UNKNOWN, result);
    }

    @Test
    void shouldReturnUnknownForNullMessage() {
        MessageProcessedEvent event = IngestionTestParameterResolver.createEventWithNullMessage();
        EventTypeDetector.EventType result = detector.detectEventType(event);
        assertEquals(EventTypeDetector.EventType.UNKNOWN, result);
    }


    @Test
    void shouldHandleCaseInsensitiveTopics() {
        MessageProcessedEvent event = IngestionTestParameterResolver.createEventWithTopic("CLIENTS");
        EventTypeDetector.EventType result = detector.detectEventType(event);
        assertEquals(EventTypeDetector.EventType.CLIENT, result);
        verify(topicMapping).getEventTypeForTopic("CLIENTS");
    }
    
    @Test
    void shouldUseTopicMappingConfiguration() {
        // Test that the detector properly delegates to the topic mapping configuration
        MessageProcessedEvent event = IngestionTestParameterResolver.createEventWithTopic("custom-topic");
        when(topicMapping.getEventTypeForTopic("custom-topic")).thenReturn(EventTypeDetector.EventType.CLIENT);
        
        EventTypeDetector.EventType result = detector.detectEventType(event);
        
        assertEquals(EventTypeDetector.EventType.CLIENT, result);
        verify(topicMapping).getEventTypeForTopic("custom-topic");
    }
    
    @Test
    void shouldHandleNullTopicGracefully() {
        MessageProcessedEvent event = IngestionTestParameterResolver.createEventWithTopic(null);
        
        EventTypeDetector.EventType result = detector.detectEventType(event);
        
        assertEquals(EventTypeDetector.EventType.UNKNOWN, result);
        verify(topicMapping).getEventTypeForTopic(null);
    }
}