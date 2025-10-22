package com.tiger.pocs.kafka.domain;

import com.tiger.pocs.ingestion.domain.Message;
import com.tiger.pocs.ingestion.domain.MessageProcessedEvent;
import org.springframework.stereotype.Component;


@Component
public class KafkaEventConverter {


    public MessageProcessedEvent convertToIngestionEvent(KafkaMessage<String> message, String handlerName) {
        // Clean headers by replacing dots with underscores to avoid MongoDB key issues
        var cleanHeaders = message.getHeaders() != null ?
                message.getHeaders().entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                entry -> entry.getKey().replace(".", "_"),
                                java.util.Map.Entry::getValue
                        )) : java.util.Map.<String, String>of();

        // Convert kafka domain message to ingestion DTO
        var ingestionMessage = Message.<String>builder()
                .key(message.getKey())
                .value(message.getValue())
                .topic(message.getTopic())
                .partition(message.getPartition())
                .offset(message.getOffset())
                .timestamp(message.getTimestamp())
                .headers(cleanHeaders)
                .build();

        return MessageProcessedEvent.builder()
                .message(ingestionMessage)
                .handlerName(handlerName)
                .processingTime(System.currentTimeMillis())
                .processedBy("kafka-module")
                .build();
    }
}