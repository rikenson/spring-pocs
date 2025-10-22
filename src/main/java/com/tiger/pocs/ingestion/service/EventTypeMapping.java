package com.tiger.pocs.ingestion.service;

import com.tiger.pocs.ingestion.service.EventTypeDetector.EventType;
import org.springframework.stereotype.Component;

@Component
public class EventTypeMapping {

    public EventType getEventTypeForTopic(String topicName) {
        if (topicName == null) return EventType.UNKNOWN;

        // Map topic names directly to event types
        String normalizedTopic = topicName.toLowerCase();
        return switch (normalizedTopic) {
            case "clientstopic" -> EventType.CLIENT;
            case "accountstopic" -> EventType.ACCOUNT;
            case "userstopic" -> EventType.USER;
            case "useraccesstopic" -> EventType.USER_ACCESS;
            case "logoffsetstopic" -> EventType.LOG_OFFSET;
            default -> EventType.UNKNOWN;
        };
    }
}