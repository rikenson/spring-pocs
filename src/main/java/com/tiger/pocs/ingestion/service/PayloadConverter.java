package com.tiger.pocs.ingestion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiger.pocs.ingestion.domain.*;
import com.tiger.pocs.ingestion.domain.ClientEntity;
import com.tiger.pocs.ingestion.service.EventTypeDetector.EventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Service to convert JSON payload to corresponding entity based on event type.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayloadConverter {

    private final ObjectMapper objectMapper;

    /**
     * Converts JSON payload to the appropriate entity based on event type.
     */
    public Object convertToEntity(String payload, EventType eventType) {
        try {
            log.info("Converting payload to {} entity ...", eventType);
            
            return switch (eventType) {
                case CLIENT -> {
                    var entity = objectMapper.readValue(payload, ClientEntity.class);
                    log.info("🔵 Converted to ClientEntity: {}", entity.getId());
                    yield entity;
                }
                case ACCOUNT -> {
                    var entity = objectMapper.readValue(payload, AccountEntity.class);
                    log.info("🟣 Converted to AccountEntity: {}", entity.getId());
                    yield entity;
                }
                case USER -> {
                    var entity = objectMapper.readValue(payload, UserEntity.class);
                    log.info("🟢 Converted to UserEntity: {}", entity.getId());
                    yield entity;
                }
                case USER_ACCESS -> {
                    var entity = objectMapper.readValue(payload, UserAccessEntity.class);
                    log.info("🟠 Converted to UserAccessEntity: {}", entity.getId());
                    yield entity;
                }
                case LOG_OFFSET -> {
                    var entity = objectMapper.readValue(payload, LogOffsetEntity.class);
                    log.info("🟡 Converted to LogOffsetEntity: {}", entity.getId());
                    yield entity;
                }
                case UNKNOWN -> {
                    log.warn("⚠️ Unknown event type, skipping save - no entity will be persisted");
                    yield null;
                }
            };
        } catch (Exception e) {
            log.error("❌ Failed to convert payload to {} entity: {}", eventType, e.getMessage(), e);
            log.warn("🔄 Falling back to raw string storage for payload: {}", payload);
            return payload;
        }
    }
}