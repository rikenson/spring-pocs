package com.tiger.pocs.ingestion.config;

import com.tiger.pocs.ingestion.service.EventTypeDetector.EventType;
import com.tiger.pocs.ingestion.service.EventTypeMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
class EventTypeMappingTest {

    private EventTypeMapping mapping;

    @BeforeEach
    void setUp() {
        mapping = new EventTypeMapping();
    }

    @Test
    void shouldMapClientTopicToClientEventType() {
        assertEquals(EventType.CLIENT, mapping.getEventTypeForTopic("clientsTopic"));
        assertEquals(EventType.CLIENT, mapping.getEventTypeForTopic("CLIENTSTOPIC"));
    }

    @Test
    void shouldMapAccountTopicToAccountEventType() {
        assertEquals(EventType.ACCOUNT, mapping.getEventTypeForTopic("accountsTopic"));
        assertEquals(EventType.ACCOUNT, mapping.getEventTypeForTopic("ACCOUNTSTOPIC"));
    }

    @Test
    void shouldMapUserTopicToUserEventType() {
        assertEquals(EventType.USER, mapping.getEventTypeForTopic("usersTopic"));
        assertEquals(EventType.USER, mapping.getEventTypeForTopic("USERSTOPIC"));
    }

    @Test
    void shouldMapUserAccessTopicToUserAccessEventType() {
        assertEquals(EventType.USER_ACCESS, mapping.getEventTypeForTopic("userAccessTopic"));
        assertEquals(EventType.USER_ACCESS, mapping.getEventTypeForTopic("USERACCESSTOPIC"));
    }

    @Test
    void shouldMapLogOffsetTopicToLogOffsetEventType() {
        assertEquals(EventType.LOG_OFFSET, mapping.getEventTypeForTopic("logOffsetsTopic"));
        assertEquals(EventType.LOG_OFFSET, mapping.getEventTypeForTopic("LOGOFFSETSTOPIC"));
    }

    @Test
    void shouldReturnUnknownForUnmappedTopic() {
        EventType result = mapping.getEventTypeForTopic("unknown-topic");
        assertEquals(EventType.UNKNOWN, result);
    }

    @Test
    void shouldReturnUnknownForNullTopic() {
        EventType result = mapping.getEventTypeForTopic(null);
        assertEquals(EventType.UNKNOWN, result);
    }

    @Test
    void shouldHandleCaseInsensitiveTopicNames() {
        assertEquals(EventType.CLIENT, mapping.getEventTypeForTopic("CLIENTSTOPIC"));
        assertEquals(EventType.ACCOUNT, mapping.getEventTypeForTopic("ACCOUNTSTOPIC"));
        assertEquals(EventType.USER, mapping.getEventTypeForTopic("USERSTOPIC"));
        assertEquals(EventType.USER_ACCESS, mapping.getEventTypeForTopic("USERACCESSTOPIC"));
        assertEquals(EventType.LOG_OFFSET, mapping.getEventTypeForTopic("LOGOFFSETSTOPIC"));
    }

    @Test
    void shouldHandleEmptyConfiguration() {
        EventTypeMapping emptyMapping = new EventTypeMapping();
        
        EventType result = emptyMapping.getEventTypeForTopic("any-unknown-topic");
        assertEquals(EventType.UNKNOWN, result);
    }

    @Test
    void shouldHandleEmptyStringTopic() {
        EventType result = mapping.getEventTypeForTopic("");
        assertEquals(EventType.UNKNOWN, result);
    }
}