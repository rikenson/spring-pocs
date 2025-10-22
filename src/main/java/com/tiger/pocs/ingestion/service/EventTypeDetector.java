package com.tiger.pocs.ingestion.service;

import com.tiger.pocs.ingestion.domain.MessageProcessedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class EventTypeDetector {

    private final EventTypeMapping topicMapping;

    public enum EventType {CLIENT, ACCOUNT, USER, USER_ACCESS, LOG_OFFSET, UNKNOWN}

    public EventType detectEventType(MessageProcessedEvent event) {
        String topic = event.getMessage() != null ? event.getMessage().getTopic() : null;
        return detectFromTopic(topic);
    }

    private EventType detectFromTopic(String topic) {
        return topicMapping.getEventTypeForTopic(topic);
    }
}