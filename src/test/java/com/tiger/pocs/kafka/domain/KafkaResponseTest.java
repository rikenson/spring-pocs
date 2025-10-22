package com.tiger.pocs.kafka.domain;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KafkaResponseTest {

    @Test
    void shouldCreateSuccessfulPublishResponse() {
        String topic = "testTopic";
        String key = "testKey";
        KafkaResponse.PublishResponse response = KafkaResponse.PublishResponse.success(topic, key);
        assertEquals("success", response.getStatus());
        assertEquals("Payload published successfully", response.getMessage());
        assertEquals(topic, response.getTopic());
        assertEquals(key, response.getKey());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void shouldCreateErrorPublishResponse() {
        // Given
        String topic = "testTopic";
        String key = "testKey";
        String errorMessage = "Topic does not exist";
        KafkaResponse.PublishResponse response = KafkaResponse.PublishResponse.error(topic, key, errorMessage);
        assertEquals("error", response.getStatus());
        assertEquals("Failed to publish payload: " + errorMessage, response.getMessage());
        assertEquals(topic, response.getTopic());
        assertEquals(key, response.getKey());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void shouldCreateUpHealthResponse() {
        boolean kafkaConnected = true;
        String bootstrapServers = "localhost:9093";
        Map<String, KafkaResponse.TopicInfo> topics = Map.of(
                "topic1", new KafkaResponse.TopicInfo("topic1", "group1", true)
        );
        KafkaResponse.HealthResponse response = KafkaResponse.HealthResponse.up(kafkaConnected, bootstrapServers, topics);
        assertEquals("UP", response.getStatus());
        assertTrue(response.isKafkaConnected());
        assertEquals(bootstrapServers, response.getBootstrapServers());
        assertEquals(topics, response.getConfiguredTopics());
        assertNotNull(response.getTimestamp());
        assertNull(response.getError());
    }

    @Test
    void shouldCreateDownHealthResponse() {
        String errorMessage = "Connection failed";
        KafkaResponse.HealthResponse response = KafkaResponse.HealthResponse.down(errorMessage);
        assertEquals("DOWN", response.getStatus());
        assertFalse(response.isKafkaConnected());
        assertEquals(errorMessage, response.getError());
        assertNotNull(response.getTimestamp());
        assertNull(response.getBootstrapServers());
        assertNull(response.getConfiguredTopics());
    }

    @Test
    void shouldCreateTopicInfoFromTopicConfig() {
        KafkaProperties.TopicConfig topicConfig = new KafkaProperties.TopicConfig();
        topicConfig.setName("testTopic");
        topicConfig.setGroupId("testGroup");
        topicConfig.setAutoSubscribe(true);
        KafkaResponse.TopicInfo topicInfo = KafkaResponse.TopicInfo.from(topicConfig);
        assertEquals("testTopic", topicInfo.getName());
        assertEquals("testGroup", topicInfo.getGroupId());
        assertTrue(topicInfo.isAutoSubscribe());
    }

    @Test
    void shouldTestTopicInfoBuilder() {
        KafkaResponse.TopicInfo topicInfo = KafkaResponse.TopicInfo.builder()
                .name("topic")
                .groupId("group")
                .autoSubscribe(false)
                .build();
        assertEquals("topic", topicInfo.getName());
        assertEquals("group", topicInfo.getGroupId());
        assertFalse(topicInfo.isAutoSubscribe());
    }

    @Test
    void shouldTestPublishResponseBuilder() {
        KafkaResponse.PublishResponse response = KafkaResponse.PublishResponse.builder()
                .status("test-status")
                .message("test-message")
                .topic("test-topic")
                .key("test-key")
                .timestamp(123456L)
                .build();
        assertEquals("test-status", response.getStatus());
        assertEquals("test-message", response.getMessage());
        assertEquals("test-topic", response.getTopic());
        assertEquals("test-key", response.getKey());
        assertEquals(123456L, response.getTimestamp());
    }

    @Test
    void shouldTestHealthResponseBuilder() {
        Map<String, KafkaResponse.TopicInfo> topics = Map.of();
        KafkaResponse.HealthResponse response = KafkaResponse.HealthResponse.builder()
                .status("test-status")
                .kafkaConnected(true)
                .bootstrapServers("localhost:9092")
                .configuredTopics(topics)
                .timestamp(123456L)
                .error("test-error")
                .build();
        assertEquals("test-status", response.getStatus());
        assertTrue(response.isKafkaConnected());
        assertEquals("localhost:9092", response.getBootstrapServers());
        assertEquals(topics, response.getConfiguredTopics());
        assertEquals(123456L, response.getTimestamp());
        assertEquals("test-error", response.getError());
    }

    @Test
    void shouldTestNoArgsConstructors() {
        KafkaResponse.PublishResponse publishResponse = new KafkaResponse.PublishResponse();
        assertNull(publishResponse.getStatus());
        assertNull(publishResponse.getMessage());
        assertNull(publishResponse.getTopic());
        assertNull(publishResponse.getKey());
        assertNull(publishResponse.getTimestamp());

        KafkaResponse.HealthResponse healthResponse = new KafkaResponse.HealthResponse();
        assertNull(healthResponse.getStatus());
        assertFalse(healthResponse.isKafkaConnected());
        assertNull(healthResponse.getBootstrapServers());
        assertNull(healthResponse.getConfiguredTopics());
        assertNull(healthResponse.getTimestamp());
        assertNull(healthResponse.getError());

        KafkaResponse.TopicInfo topicInfo = new KafkaResponse.TopicInfo();
        assertNull(topicInfo.getName());
        assertNull(topicInfo.getGroupId());
        assertFalse(topicInfo.isAutoSubscribe());
    }

    @Test
    void shouldTestAllArgsConstructors() {
        KafkaResponse.PublishResponse publishResponse = new KafkaResponse.PublishResponse(
                "success", "message", "topic", "key", 123456L);
        assertEquals("success", publishResponse.getStatus());
        assertEquals("message", publishResponse.getMessage());
        assertEquals("topic", publishResponse.getTopic());
        assertEquals("key", publishResponse.getKey());
        assertEquals(123456L, publishResponse.getTimestamp());

        Map<String, KafkaResponse.TopicInfo> topics = Map.of();
        KafkaResponse.HealthResponse healthResponse = new KafkaResponse.HealthResponse(
                "UP", true, "localhost:9092", topics, 123456L, "error");
        assertEquals("UP", healthResponse.getStatus());
        assertTrue(healthResponse.isKafkaConnected());
        assertEquals("localhost:9092", healthResponse.getBootstrapServers());
        assertEquals(topics, healthResponse.getConfiguredTopics());
        assertEquals(123456L, healthResponse.getTimestamp());
        assertEquals("error", healthResponse.getError());

        KafkaResponse.TopicInfo topicInfo = new KafkaResponse.TopicInfo("topic", "group", true);
        assertEquals("topic", topicInfo.getName());
        assertEquals("group", topicInfo.getGroupId());
        assertTrue(topicInfo.isAutoSubscribe());
    }

    @Test
    void shouldTestSettersAndGetters() {
        KafkaResponse.PublishResponse publishResponse = new KafkaResponse.PublishResponse();
        publishResponse.setStatus("test-status");
        publishResponse.setMessage("test-message");
        publishResponse.setTopic("test-topic");
        publishResponse.setKey("test-key");
        publishResponse.setTimestamp(789L);
        
        assertEquals("test-status", publishResponse.getStatus());
        assertEquals("test-message", publishResponse.getMessage());
        assertEquals("test-topic", publishResponse.getTopic());
        assertEquals("test-key", publishResponse.getKey());
        assertEquals(789L, publishResponse.getTimestamp());

        KafkaResponse.HealthResponse healthResponse = new KafkaResponse.HealthResponse();
        Map<String, KafkaResponse.TopicInfo> topics = Map.of("test", new KafkaResponse.TopicInfo());
        healthResponse.setStatus("DOWN");
        healthResponse.setKafkaConnected(true);
        healthResponse.setBootstrapServers("localhost:9093");
        healthResponse.setConfiguredTopics(topics);
        healthResponse.setTimestamp(456L);
        healthResponse.setError("test-error");
        
        assertEquals("DOWN", healthResponse.getStatus());
        assertTrue(healthResponse.isKafkaConnected());
        assertEquals("localhost:9093", healthResponse.getBootstrapServers());
        assertEquals(topics, healthResponse.getConfiguredTopics());
        assertEquals(456L, healthResponse.getTimestamp());
        assertEquals("test-error", healthResponse.getError());

        KafkaResponse.TopicInfo topicInfo = new KafkaResponse.TopicInfo();
        topicInfo.setName("test-name");
        topicInfo.setGroupId("test-group");
        topicInfo.setAutoSubscribe(true);
        
        assertEquals("test-name", topicInfo.getName());
        assertEquals("test-group", topicInfo.getGroupId());
        assertTrue(topicInfo.isAutoSubscribe());
    }

    @Test
    void shouldTestEdgeCasesWithNullValues() {
        KafkaResponse.PublishResponse errorResponse = KafkaResponse.PublishResponse.error(null, null, null);
        assertEquals("error", errorResponse.getStatus());
        assertEquals("Failed to publish payload: null", errorResponse.getMessage());
        assertNull(errorResponse.getTopic());
        assertNull(errorResponse.getKey());
        assertNotNull(errorResponse.getTimestamp());

        KafkaResponse.HealthResponse upResponse = KafkaResponse.HealthResponse.up(false, null, null);
        assertEquals("UP", upResponse.getStatus());
        assertFalse(upResponse.isKafkaConnected());
        assertNull(upResponse.getBootstrapServers());
        assertNull(upResponse.getConfiguredTopics());
        assertNotNull(upResponse.getTimestamp());
        assertNull(upResponse.getError());

        KafkaResponse.HealthResponse downResponse = KafkaResponse.HealthResponse.down(null);
        assertEquals("DOWN", downResponse.getStatus());
        assertFalse(downResponse.isKafkaConnected());
        assertNull(downResponse.getError());
        assertNotNull(downResponse.getTimestamp());
    }

    @Test
    void shouldTestTopicInfoFromNullTopicConfig() {
        KafkaProperties.TopicConfig nullConfig = new KafkaProperties.TopicConfig();
        KafkaResponse.TopicInfo topicInfo = KafkaResponse.TopicInfo.from(nullConfig);
        assertNull(topicInfo.getName());
        assertNull(topicInfo.getGroupId());
        assertFalse(topicInfo.isAutoSubscribe());
    }
}