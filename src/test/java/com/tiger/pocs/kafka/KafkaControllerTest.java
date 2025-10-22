package com.tiger.pocs.kafka;

import com.tiger.pocs.kafka.config.KafkaConfigUtils;
import com.tiger.pocs.kafka.domain.KafkaProperties;
import com.tiger.pocs.kafka.domain.KafkaResponse;
import com.tiger.pocs.kafka.support.KafkaTestParameterResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, KafkaTestParameterResolver.class})
class KafkaControllerTest {

    @Mock
    private KafkaProducer kafkaProducer;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private KafkaController kafkaController;
    private KafkaProperties kafkaProperties;

    @BeforeEach
    void setUp(KafkaProperties testKafkaProperties) {
        this.kafkaProperties = testKafkaProperties;
        this.kafkaController = new KafkaController(kafkaProducer, kafkaProperties, kafkaTemplate);
    }

    @Test
    void shouldPublishPayloadSuccessfully() {
        String topic = "testTopic";
        String key = "testKey";
        Object payload = "testPayload";
        when(kafkaProducer.validateAndPublishPayload(topic, key, payload)).thenReturn(true);
        ResponseEntity<KafkaResponse.PublishResponse> response =
                kafkaController.publishPayload(topic, key, payload);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(kafkaProducer).validateAndPublishPayload(topic, key, payload);
    }

    @Test
    void shouldHandlePublishFailureWhenTopicDoesNotExist() {
        String topic = "nonExistentTopic";
        String key = "testKey";
        Object payload = "testPayload";
        when(kafkaProducer.validateAndPublishPayload(topic, key, payload)).thenReturn(false);
        ResponseEntity<KafkaResponse.PublishResponse> response =
                kafkaController.publishPayload(topic, key, payload);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("does not exist"));
    }

    @Test
    void shouldHandlePublishException() {
        String topic = "testTopic";
        String key = "testKey";
        Object payload = "testPayload";
        when(kafkaProducer.validateAndPublishPayload(topic, key, payload))
                .thenThrow(new RuntimeException("Kafka error"));
        ResponseEntity<KafkaResponse.PublishResponse> response =
                kafkaController.publishPayload(topic, key, payload);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Failed to publish payload: Kafka error", response.getBody().getMessage());
    }

    @Test
    void shouldPublishPayloadWithoutKey() {
        String topic = "testTopic";
        Object payload = "testPayload";
        when(kafkaProducer.validateAndPublishPayload(topic, null, payload)).thenReturn(true);
        ResponseEntity<KafkaResponse.PublishResponse> response =
                kafkaController.publishPayload(topic, null, payload);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(kafkaProducer).validateAndPublishPayload(topic, null, payload);
    }

    @Test
    void shouldReturnSanitizedKafkaConfiguration() {
        KafkaProperties sanitizedProps = new KafkaProperties();
        sanitizedProps.setBootstrapServers("localhost:9093");

        try (MockedStatic<KafkaConfigUtils> mockedUtils = mockStatic(KafkaConfigUtils.class)) {
            mockedUtils.when(() -> KafkaConfigUtils.createSanitizedConfiguration(kafkaProperties))
                    .thenReturn(sanitizedProps);
            ResponseEntity<KafkaProperties> response = kafkaController.getKafkaConfiguration();
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(sanitizedProps, response.getBody());
        }
    }

    @Test
    void shouldReturnKafkaHealthWhenUp() {
        // Given
        Map<String, KafkaResponse.TopicInfo> topicInfo = Map.of("topic1",
                new KafkaResponse.TopicInfo("topic1", "group1", true));

        try (MockedStatic<KafkaConfigUtils> mockedUtils = mockStatic(KafkaConfigUtils.class)) {
            mockedUtils.when(() -> KafkaConfigUtils.isKafkaConnected(kafkaTemplate)).thenReturn(true);
            mockedUtils.when(() -> KafkaConfigUtils.getConfiguredTopicsInfo(kafkaProperties))
                    .thenReturn(topicInfo);
            ResponseEntity<KafkaResponse.HealthResponse> response = kafkaController.getKafkaHealth();
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("UP", response.getBody().getStatus());
        }
    }

    @Test
    void shouldReturnKafkaHealthWhenDown() {
        try (MockedStatic<KafkaConfigUtils> mockedUtils = mockStatic(KafkaConfigUtils.class)) {
            mockedUtils.when(() -> KafkaConfigUtils.isKafkaConnected(kafkaTemplate))
                    .thenThrow(new RuntimeException("Connection failed"));
            ResponseEntity<KafkaResponse.HealthResponse> response = kafkaController.getKafkaHealth();
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("DOWN", response.getBody().getStatus());
            assertEquals("Connection failed", response.getBody().getError());
        }
    }
}