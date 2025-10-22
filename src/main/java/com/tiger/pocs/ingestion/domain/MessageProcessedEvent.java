package com.tiger.pocs.ingestion.domain;

import lombok.Data;
import lombok.Builder;


@Data
@Builder
public class MessageProcessedEvent {
    private Message<?> message;
    private String handlerName;
    private long processingTime;
    private String processedBy;
}