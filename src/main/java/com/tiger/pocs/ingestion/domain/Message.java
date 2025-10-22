package com.tiger.pocs.ingestion.domain;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;


@Data
@Builder
public class Message<T> {
    private String key;
    private T value;
    private String topic;
    private int partition;
    private long offset;
    private Instant timestamp;
    private Map<String, String> headers;
}