package com.tiger.pocs.kafka.domain;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;


@Data
@Builder
public class KafkaMessage<T> {
    private String key;
    private T value;
    private String topic;
    private int partition;
    private long offset;
    private Instant timestamp;
    private Map<String, String> headers;
}