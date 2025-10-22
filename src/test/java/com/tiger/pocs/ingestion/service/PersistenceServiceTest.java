package com.tiger.pocs.ingestion.service;

import com.tiger.pocs.ingestion.domain.ClientEntity;
import com.tiger.pocs.ingestion.service.EventTypeDetector.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersistenceServiceTest {

    @Mock
    private ReactiveMongoTemplate mongoTemplate;
    private PersistenceService persistenceService;

    @BeforeEach
    void setUp() {
        persistenceService = new PersistenceService(mongoTemplate);
    }

    @ParameterizedTest
    @MethodSource("provideSuccessfulPersistenceScenarios")
    void shouldPersistEntitySuccessfully(ClientEntity clientEntity, EventType eventType, String topic, String key) {
        when(mongoTemplate.save(clientEntity)).thenReturn(Mono.just(clientEntity));
        persistenceService.persistEntity(clientEntity, eventType, topic, key);
        verify(mongoTemplate).save(clientEntity);
    }

    private static Stream<Arguments> provideSuccessfulPersistenceScenarios() {
        ClientEntity clientEntity = ClientEntity.builder()
                .id("CLI-001")
                .build();
        
        return Stream.of(
                Arguments.of(clientEntity, EventType.CLIENT, "clients", "test-key"),
                Arguments.of(clientEntity, EventType.ACCOUNT, "accounts", "account-key"),
                Arguments.of(clientEntity, EventType.UNKNOWN, "unknown-topic", "unknown-key"),
                Arguments.of(clientEntity, EventType.CLIENT, null, null),
                Arguments.of(clientEntity, EventType.CLIENT, "", "")
        );
    }

    @ParameterizedTest
    @MethodSource("provideErrorScenarios")
    void shouldHandlePersistenceError(ClientEntity clientEntity, Exception error, String description) {
        when(mongoTemplate.save(clientEntity)).thenReturn(Mono.error(error));
        persistenceService.persistEntity(clientEntity, EventType.CLIENT, "clients", "test-key");
        verify(mongoTemplate).save(clientEntity);
    }

    private static Stream<Arguments> provideErrorScenarios() {
        ClientEntity clientEntity = ClientEntity.builder()
                .id("CLI-001")
                .build();
        
        return Stream.of(
                Arguments.of(clientEntity, new RuntimeException("Database connection failed"), "Database connection error"),
                Arguments.of(clientEntity, new TimeoutException("Timeout"), "Timeout error")
        );
    }

    @Test
    void shouldSkipPersistenceForNullEntity() {
        persistenceService.persistEntity(null, EventType.CLIENT, "clients", "test-key");
        verify(mongoTemplate, never()).save(any());
    }


    @Test
    void shouldHandleComplexEntity() {
        // Given
        Object complexEntity = new Object() {
            @Override
            public String toString() {
                return "ComplexEntity";
            }
        };
        when(mongoTemplate.save(complexEntity)).thenReturn(Mono.just(complexEntity));
        persistenceService.persistEntity(complexEntity, EventType.USER, "users", "user-key");
        verify(mongoTemplate).save(complexEntity);
    }


    @Test
    void shouldHandleMultipleConcurrentSaves() {
        ClientEntity clientEntity = ClientEntity.builder()
                .id("CLI-001")
                .build();
        
        when(mongoTemplate.save(clientEntity)).thenReturn(Mono.just(clientEntity));
        persistenceService.persistEntity(clientEntity, EventType.CLIENT, "clients", "key1");
        persistenceService.persistEntity(clientEntity, EventType.CLIENT, "clients", "key2");
        persistenceService.persistEntity(clientEntity, EventType.CLIENT, "clients", "key3");
        verify(mongoTemplate, times(3)).save(clientEntity);
    }
}